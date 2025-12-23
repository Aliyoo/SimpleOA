package com.example.simpleoa.service.impl;

import com.example.simpleoa.dto.leave.AnnualLeaveCalculation;
import com.example.simpleoa.dto.leave.InitializationResult;
import com.example.simpleoa.model.LeaveBalance;
import com.example.simpleoa.model.LeaveBalanceCarryover;
import com.example.simpleoa.model.LeaveSystemConfig;
import com.example.simpleoa.model.LeaveType;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.LeaveBalanceRepository;
import com.example.simpleoa.repository.LeaveBalanceCarryoverRepository;
import com.example.simpleoa.repository.LeaveSystemConfigRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 假期余额初始化服务实现类
 * 提供自动化的假期余额初始化、结转和管理功能
 */
@Service
@Transactional
public class LeaveBalanceInitializationServiceImpl implements LeaveBalanceInitializationService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveBalanceInitializationServiceImpl.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveBalanceCarryoverRepository carryoverRepository;

    @Autowired
    private AnnualLeaveRuleService annualLeaveRuleService;

    @Autowired
    private SeniorityCalculationService seniorityCalculationService;

    @Autowired
    private LeaveSystemConfigService configService;

    // 假期类型配置
    private static final Map<String, LeaveTypeInfo> LEAVE_TYPES = Map.of(
        "ANNUAL_LEAVE", new LeaveTypeInfo("年假", true, 15),
        "SICK_LEAVE", new LeaveTypeInfo("病假", true, 10),
        "PERSONAL_LEAVE", new LeaveTypeInfo("事假", true, 5),
        "MARRIAGE_LEAVE", new LeaveTypeInfo("婚假", false, 3),
        "MATERNITY_LEAVE", new LeaveTypeInfo("产假", false, 98),
        "PATERNITY_LEAVE", new LeaveTypeInfo("陪产假", false, 15),
        "BEREAVEMENT_LEAVE", new LeaveTypeInfo("丧假", false, 3),
        "OTHER_LEAVE", new LeaveTypeInfo("其他", false, 0)
    );

    @Override
    @Async
    public CompletableFuture<InitializationResult> batchInitializeYearlyBalances(Integer year) {
        logger.info("开始批量初始化{}年所有员工假期余额", year);
        java.time.Instant start = java.time.Instant.now();

        List<User> activeUsers = userRepository.findByEnabledTrue();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        for (User user : activeUsers) {
            try {
                boolean success = initializeUserYearlyBalance(user.getId(), year);
                if (success) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                    String errorMsg = String.format("用户: %s (姓名: %s, ID: %d) - 初始化失败,请检查用户数据完整性",
                        user.getUsername(), 
                        user.getRealName() != null ? user.getRealName() : user.getUsername(),
                        user.getId());
                    errors.add(errorMsg);
                }
            } catch (IllegalArgumentException e) {
                failureCount.incrementAndGet();
                // 特别处理入职日期缺失的错误
                String errorMsg;
                if (e.getMessage() != null && e.getMessage().contains("入职日期为空")) {
                    errorMsg = String.format("用户: %s (姓名: %s, ID: %d) - 缺少入职日期,请在用户管理中补充",
                        user.getUsername(),
                        user.getRealName() != null ? user.getRealName() : user.getUsername(),
                        user.getId());
                } else {
                    errorMsg = String.format("用户: %s (姓名: %s, ID: %d) - %s",
                        user.getUsername(),
                        user.getRealName() != null ? user.getRealName() : user.getUsername(),
                        user.getId(),
                        e.getMessage());
                }
                errors.add(errorMsg);
                logger.warn("初始化用户{}年{}假期余额失败: {}", user.getId(), year, e.getMessage());
            } catch (Exception e) {
                failureCount.incrementAndGet();
                String errorMsg = String.format("用户: %s (姓名: %s, ID: %d) - 初始化错误: %s",
                    user.getUsername(),
                    user.getRealName() != null ? user.getRealName() : user.getUsername(),
                    user.getId(),
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                errors.add(errorMsg);
                logger.error("初始化用户{}年{}假期余额失败: {}", user.getId(), year, e.getMessage(), e);
            }
        }

        InitializationResult result = InitializationResult.builder()
            .year(year)
            .totalUsers(activeUsers.size())
            .successCount(successCount.get())
            .failureCount(failureCount.get())
            .errors(errors)
            .startTime(start)
            .endTime(java.time.Instant.now())
            .processingTime(java.time.Duration.between(start, java.time.Instant.now()))
            .build();

        logger.info("批量初始化完成，总用户：{}，成功：{}，失败：{}",
            result.getTotalUsers(), result.getSuccessCount(), result.getFailureCount());

        return CompletableFuture.completedFuture(result);
    }

    @Override
    public boolean initializeUserYearlyBalance(Long userId, Integer year) {
        logger.debug("初始化用户{}年{}的假期余额", userId, year);

        User user = userService.getUserById(userId);
        if (user == null || user.getEnabled() == null || user.getEnabled() != 1) {
            logger.warn("用户不存在或已禁用，ID: {}", userId);
            return false;
        }

        try {
            // 计算各种假期类型的年假额度
            Map<String, BigDecimal> yearlyEntitlements = calculateYearlyEntitlements(userId, year);

            for (Map.Entry<String, BigDecimal> entry : yearlyEntitlements.entrySet()) {
                String leaveTypeStr = entry.getKey();
                Integer totalDays = entry.getValue().intValue();
                
                // 转换字符串为LeaveType枚举
                LeaveType leaveType = LeaveType.valueOf(leaveTypeStr);

                initializeUserLeaveTypeBalance(userId, year, leaveType, totalDays);
            }

            logger.debug("用户{}年{}假期余额初始化成功", userId, year);
            return true;

        } catch (Exception e) {
            logger.error("初始化用户{}年{}假期余额失败: {}", userId, year, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean initializeNewEmployeeBalance(Long userId, LocalDate hireDate) {
        logger.info("初始化新用户{}的假期余额，入职日期: {}", userId, hireDate);

        User user = userService.getUserById(userId);
        if (user == null) {
            logger.warn("用户不存在，ID: {}", userId);
            return false;
        }

        int currentYear = LocalDate.now().getYear();

        try {
            // 计算当年剩余工作月数比例
            LocalDate yearEnd = LocalDate.of(currentYear, 12, 31);
            BigDecimal remainingYearRatio = calculateRemainingYearRatio(hireDate, yearEnd);

            // 计算各种假期的按比例额度
            Map<String, BigDecimal> yearlyEntitlements = calculateYearlyEntitlements(userId, currentYear);
            Map<String, BigDecimal> proportionalEntitlements = new HashMap<>();

            for (Map.Entry<String, BigDecimal> entry : yearlyEntitlements.entrySet()) {
                String leaveType = entry.getKey();
                BigDecimal fullDays = entry.getValue();
                LeaveTypeInfo typeInfo = LEAVE_TYPES.get(leaveType);

                if (typeInfo != null && typeInfo.isProrated()) {
                    // 按比例计算
                    BigDecimal proratedDays = fullDays.multiply(remainingYearRatio)
                        .setScale(1, BigDecimal.ROUND_HALF_UP);
                    proportionalEntitlements.put(leaveType, proratedDays);
                } else {
                    // 不按比例
                    proportionalEntitlements.put(leaveType, fullDays);
                }
            }

            // 初始化各类型假期余额
            for (Map.Entry<String, BigDecimal> entry : proportionalEntitlements.entrySet()) {
                String leaveTypeStr = entry.getKey();
                Integer totalDays = entry.getValue().intValue();
                LeaveType leaveType = LeaveType.valueOf(leaveTypeStr);
                initializeUserLeaveTypeBalance(userId, currentYear, leaveType, totalDays);
            }

            logger.info("新用户{}假期余额初始化成功", userId);
            return true;

        } catch (Exception e) {
            logger.error("初始化新用户{}假期余额失败: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Async
    public CompletableFuture<CarryoverResult> executeCarryover(Integer fromYear, Integer toYear) {
        logger.info("执行{}年到{}年的假期结转", fromYear, toYear);

        List<LeaveBalance> fromYearBalances = leaveBalanceRepository.findByYear(fromYear);
        int totalUsers = fromYearBalances.size() / LEAVE_TYPES.size(); // 估算用户数
        int processedUsers = 0;
        BigDecimal totalCarryoverDays = BigDecimal.ZERO;
        List<String> errors = new ArrayList<>();

        // 按用户分组处理
        Map<Long, List<LeaveBalance>> userBalances = fromYearBalances.stream()
            .collect(Collectors.groupingBy(LeaveBalance::getUserId));

        for (Map.Entry<Long, List<LeaveBalance>> entry : userBalances.entrySet()) {
            Long userId = entry.getKey();
            List<LeaveBalance> balances = entry.getValue();

            try {
                BigDecimal userCarryoverDays = executeUserCarryover(userId, fromYear, toYear, balances);
                if (userCarryoverDays.compareTo(BigDecimal.ZERO) > 0) {
                    totalCarryoverDays = totalCarryoverDays.add(userCarryoverDays);
                    processedUsers++;
                }
            } catch (Exception e) {
                String errorMsg = String.format("用户ID %d 结转失败: %s", userId, e.getMessage());
                errors.add(errorMsg);
                logger.error("用户{}假期结转失败: {}", userId, e.getMessage(), e);
            }
        }

        CarryoverResult result = new CarryoverResult(totalUsers, processedUsers, totalCarryoverDays, errors);
        logger.info("假期结转完成，处理用户：{}，结转总天数：{}", processedUsers, totalCarryoverDays);

        return CompletableFuture.completedFuture(result);
    }

    @Override
    public Map<String, BigDecimal> calculateYearlyEntitlements(Long userId, Integer year) {
        logger.debug("计算用户{}年{}的假期额度", userId, year);

        Map<String, BigDecimal> entitlements = new HashMap<>();

        for (Map.Entry<String, LeaveTypeInfo> entry : LEAVE_TYPES.entrySet()) {
            String leaveType = entry.getKey();
            LeaveTypeInfo typeInfo = entry.getValue();

            if (typeInfo.hasQuota()) {
                // 有配额限制的类型需要计算
                if ("ANNUAL_LEAVE".equals(leaveType)) {
                    // 年假基于工龄计算
                    AnnualLeaveCalculation calculation = annualLeaveRuleService.calculateAnnualLeave(userId, year);
                    entitlements.put(leaveType, calculation.getTotalDays());
                } else {
                    // 其他有配额的类型使用默认值
                    entitlements.put(leaveType, BigDecimal.valueOf(typeInfo.getDefaultDays()));
                }
            } else {
                // 无配额限制的类型不预分配
                entitlements.put(leaveType, BigDecimal.ZERO);
            }
        }

        return entitlements;
    }

    @Override
    public boolean recalculateUserBalance(Long userId, Integer year) {
        logger.info("重新计算用户{}年{}的假期余额", userId, year);

        try {
            // 重新计算额度
            Map<String, BigDecimal> yearlyEntitlements = calculateYearlyEntitlements(userId, year);

            // 获取当前余额
            List<LeaveBalance> currentBalances = leaveBalanceRepository.findByUserIdAndYear(userId, year);

            // 更新或创建余额记录
            for (Map.Entry<String, BigDecimal> entry : yearlyEntitlements.entrySet()) {
                String leaveType = entry.getKey();
                Integer newTotalDays = entry.getValue().intValue();

                Optional<LeaveBalance> existingBalanceOpt = currentBalances.stream()
                    .filter(b -> leaveType.equals(b.getLeaveType()))
                    .findFirst();

                if (existingBalanceOpt.isPresent()) {
                    // 更新现有记录
                    LeaveBalance balance = existingBalanceOpt.get();
                    Integer usedDays = balance.getUsedDays() != null ? balance.getUsedDays() : 0;
                    Integer newRemainingDays = Math.max(0, newTotalDays - usedDays);

                    balance.setTotalDays(newTotalDays);
                    balance.setRemainingDays(newRemainingDays);
                    leaveBalanceRepository.save(balance);
                } else {
                    // 创建新记录
                    LeaveType leaveTypeEnum = LeaveType.valueOf(leaveType);
                    initializeUserLeaveTypeBalance(userId, year, leaveTypeEnum, newTotalDays);
                }
            }

            logger.info("用户{}年{}假期余额重新计算完成", userId, year);
            return true;

        } catch (Exception e) {
            logger.error("重新计算用户{}年{}假期余额失败: {}", userId, year, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public InitializationResult batchRecalculateBalances(List<Long> userIds, Integer year) {
        logger.info("批量重新计算{}个用户{}年的假期余额", userIds.size(), year);

        int totalUsers = userIds.size();
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                boolean success = recalculateUserBalance(userId, year);
                if (success) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                failureCount++;
                String errorMsg = String.format("用户ID %d: %s", userId, e.getMessage());
                errors.add(errorMsg);
                logger.error("重新计算用户{}年假期余额失败: {}", userId, e.getMessage(), e);
            }
        }

        return InitializationResult.builder()
            .year(year)
            .totalUsers(totalUsers)
            .successCount(successCount)
            .failureCount(failureCount)
            .errors(errors)
            .build();
    }

    @Override
    public boolean isBalanceInitialized(Long userId, Integer year) {
        List<LeaveBalance> balances = leaveBalanceRepository.findByUserIdAndYear(userId, year);
        return !balances.isEmpty();
    }

    @Override
    public List<Long> getUninitializedUsers(Integer year) {
        List<User> activeUsers = userRepository.findByEnabledTrue();
        List<Long> uninitializedUsers = new ArrayList<>();

        for (User user : activeUsers) {
            if (!isBalanceInitialized(user.getId(), year)) {
                uninitializedUsers.add(user.getId());
            }
        }

        return uninitializedUsers;
    }

    @Override
    public InitializationResult resetYearlyBalances(Integer year, boolean confirm) {
        if (!confirm) {
            throw new IllegalArgumentException("重置操作需要确认");
        }

        logger.warn("重置{}年所有用户的假期余额", year);

        List<LeaveBalance> yearBalances = leaveBalanceRepository.findByYear(year);
        int totalRecords = yearBalances.size();

        try {
            leaveBalanceRepository.deleteAll(yearBalances);
            logger.info("{}年假期余额重置完成，删除记录数：{}", year, totalRecords);

            return InitializationResult.builder()
                .year(year)
                .totalUsers(totalRecords / LEAVE_TYPES.size())
                .successCount(totalRecords / LEAVE_TYPES.size())
                .failureCount(0)
                .errors(Collections.emptyList())
                .build();

        } catch (Exception e) {
            logger.error("重置{}年假期余额失败: {}", year, e.getMessage(), e);
            return InitializationResult.builder()
                .year(year)
                .totalUsers(totalRecords / LEAVE_TYPES.size())
                .successCount(0)
                .failureCount(totalRecords / LEAVE_TYPES.size())
                .errors(Arrays.asList("重置失败: " + e.getMessage()))
                .build();
        }
    }

    @Override
    public CompletableFuture<RepairResult> repairInconsistentBalances(Integer year) {
        logger.info("开始修复{}年不一致的假期余额数据", year);

        List<LeaveBalance> allBalances = leaveBalanceRepository.findByYear(year);
        int totalChecked = allBalances.size();
        int repairedRecords = 0;
        List<String> repairDetails = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (LeaveBalance balance : allBalances) {
            try {
                if (repairBalanceIfInconsistent(balance)) {
                    repairedRecords++;
                    repairDetails.add(String.format("修复用户ID %d 的%s余额",
                        balance.getUserId(), balance.getLeaveType()));
                }
            } catch (Exception e) {
                errors.add(String.format("修复用户ID %d 的%s余额失败: %s",
                    balance.getUserId(), balance.getLeaveType(), e.getMessage()));
            }
        }

        RepairResult result = new RepairResult(totalChecked, repairedRecords, repairDetails, errors);
        logger.info("假期余额修复完成，检查记录：{}，修复记录：{}", totalChecked, repairedRecords);

        return CompletableFuture.completedFuture(result);
    }

    @Override
    public InitializationStatusReport getInitializationStatus(Integer year) {
        List<User> activeUsers = userRepository.findByEnabledTrue();
        int totalUsers = activeUsers.size();
        int initializedUsers = 0;
        Map<String, Integer> leaveTypeStats = new HashMap<>();

        for (User user : activeUsers) {
            List<LeaveBalance> balances = leaveBalanceRepository.findByUserIdAndYear(user.getId(), year);
            if (!balances.isEmpty()) {
                initializedUsers++;

                // 统计各类型假期初始化情况
                for (LeaveBalance balance : balances) {
                    leaveTypeStats.merge(balance.getLeaveType().name(), 1, Integer::sum);
                }
            }
        }

        int uninitializedUsers = totalUsers - initializedUsers;

        return new InitializationStatusReport(totalUsers, initializedUsers, uninitializedUsers, leaveTypeStats);
    }

    @Override
    public ValidationResult validateBalanceConfiguration() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // 检查年假规则配置
            List<com.example.simpleoa.model.AnnualLeaveRule> activeRules = annualLeaveRuleService.getActiveRules();
            if (activeRules.isEmpty()) {
                errors.add("没有启用的年假规则");
            } else {
                // 检查工龄范围覆盖
                BigDecimal maxCoveredYears = activeRules.stream()
                    .filter(rule -> rule.getMaxYears() == null)
                    .map(com.example.simpleoa.model.AnnualLeaveRule::getMinYears)
                    .min(BigDecimal::compareTo)
                    .orElse(null);

                if (maxCoveredYears != null && maxCoveredYears.compareTo(BigDecimal.ZERO) > 0) {
                    warnings.add("年假规则存在工龄覆盖间隙");
                }
            }

            // 检查系统配置
            Optional<LeaveSystemConfig> carryoverEnabled = configService.getConfigByKey("leave.carryover.enabled");
            if (!carryoverEnabled.isPresent()) {
                warnings.add("缺少结转功能配置，使用默认值");
            }

            // 检查用户数据完整性
            List<User> usersWithoutHireDate = userRepository.findByEnabledTrue().stream()
                .filter(user -> user.getHireDate() == null)
                .collect(Collectors.toList());

            if (!usersWithoutHireDate.isEmpty()) {
                errors.add(String.format("有%d个用户缺少入职日期", usersWithoutHireDate.size()));
            }

        } catch (Exception e) {
            errors.add("配置验证过程中发生错误: " + e.getMessage());
        }

        boolean isValid = errors.isEmpty();
        logger.debug("假期余额配置验证结果: valid={}, errors={}, warnings={}",
            isValid, errors.size(), warnings.size());

        return new ValidationResult(isValid, errors, warnings);
    }

    // 私有辅助方法

    /**
     * 初始化用户指定类型的假期余额
     */
    private void initializeUserLeaveTypeBalance(Long userId, Integer year, LeaveType leaveType, Integer totalDays) {
        Optional<LeaveBalance> existingBalanceOpt = leaveBalanceRepository
            .findByUserIdAndYearAndLeaveType(userId, year, leaveType);

        if (existingBalanceOpt.isPresent()) {
            // 更新现有记录
            LeaveBalance balance = existingBalanceOpt.get();
            Integer usedDays = balance.getUsedDays() != null ? balance.getUsedDays() : 0;

            balance.setTotalDays(totalDays);
            balance.setRemainingDays(Math.max(0, totalDays - usedDays));

            leaveBalanceRepository.save(balance);
        } else {
            // 创建新记录
            User user = userService.getUserById(userId);
            LeaveBalance newBalance = new LeaveBalance();
            newBalance.setUser(user);
            newBalance.setYear(year);
            newBalance.setLeaveType(leaveType);
            newBalance.setTotalDays(totalDays);
            newBalance.setUsedDays(0);
            newBalance.setRemainingDays(totalDays);

            leaveBalanceRepository.save(newBalance);
        }
    }

    /**
     * 计算当年剩余工作年数比例
     */
    private BigDecimal calculateRemainingYearRatio(LocalDate hireDate, LocalDate yearEnd) {
        if (hireDate.isAfter(yearEnd)) {
            return BigDecimal.ZERO;
        }

        LocalDate yearStart = LocalDate.of(hireDate.getYear(), 1, 1);
        if (hireDate.isBefore(yearStart)) {
            return BigDecimal.ONE;
        }

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(yearStart, yearEnd) + 1;
        long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(hireDate, yearEnd) + 1;

        return BigDecimal.valueOf(remainingDays)
            .divide(BigDecimal.valueOf(totalDays), 4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 执行单个用户的假期结转
     */
    private BigDecimal executeUserCarryover(Long userId, Integer fromYear, Integer toYear, List<LeaveBalance> balances) {
        BigDecimal totalCarryoverDays = BigDecimal.ZERO;
        BigDecimal carryoverLimit = BigDecimal.valueOf(getCarryoverLimit());

        for (LeaveBalance balance : balances) {
            LeaveType leaveType = balance.getLeaveType();

            // 只有年假支持结转
            if (LeaveType.ANNUAL_LEAVE.equals(leaveType)) {
                Integer remainingDays = balance.getRemainingDays() != null ?
                    balance.getRemainingDays() : 0;

                if (remainingDays > 0) {
                    // 限制结转天数
                    Integer carryoverDays = Math.min(remainingDays, getCarryoverLimit());

                    if (carryoverDays > 0) {
                        createCarryoverRecord(userId, fromYear, toYear, leaveType, carryoverDays);
                        totalCarryoverDays = totalCarryoverDays.add(BigDecimal.valueOf(carryoverDays));
                    }
                }
            }
        }

        return totalCarryoverDays;
    }

    /**
     * 创建结转记录
     */
    private void createCarryoverRecord(Long userId, Integer fromYear, Integer toYear,
                                      LeaveType leaveType, Integer carryoverDays) {
        // 检查是否已存在结转记录
        Optional<LeaveBalanceCarryover> existingRecord = carryoverRepository
            .findByUserIdAndFromYearAndToYearAndLeaveType(userId, fromYear, toYear, leaveType.name());

        if (existingRecord.isPresent()) {
            // 更新现有记录
            LeaveBalanceCarryover carryover = existingRecord.get();
            carryover.setCarryoverDays(BigDecimal.valueOf(carryoverDays));
            carryover.setUpdatedAt(LocalDateTime.now());
            carryoverRepository.save(carryover);
        } else {
            // 创建新记录
            LeaveBalanceCarryover carryover = new LeaveBalanceCarryover();
            carryover.setUserId(userId);
            carryover.setFromYear(fromYear);
            carryover.setToYear(toYear);
            carryover.setLeaveType(leaveType.name());
            carryover.setCarryoverDays(BigDecimal.valueOf(carryoverDays));
            carryover.setUsedDays(BigDecimal.ZERO);
            carryover.setIsUsed(false);
            carryover.setCarryoverRuleId(null);
            carryover.setCreatedAt(LocalDateTime.now());
            carryoverRepository.save(carryover);
        }
    }

    /**
     * 修复不一致的余额记录
     */
    private boolean repairBalanceIfInconsistent(LeaveBalance balance) {
        boolean repaired = false;
        Integer totalDays = balance.getTotalDays();
        Integer usedDays = balance.getUsedDays() != null ? balance.getUsedDays() : 0;
        Integer remainingDays = balance.getRemainingDays();

        // 计算正确的剩余天数
        Integer correctRemainingDays = totalDays - usedDays;

        // 处理负数情况
        if (correctRemainingDays < 0) {
            correctRemainingDays = 0;
            repaired = true;
        }

        // 检查是否需要修复
        if (remainingDays == null || !remainingDays.equals(correctRemainingDays)) {
            balance.setRemainingDays(correctRemainingDays);
            leaveBalanceRepository.save(balance);
            repaired = true;
        }

        return repaired;
    }

    /**
     * 获取结转限制配置
     */
    private int getCarryoverLimit() {
        try {
            Optional<LeaveSystemConfig> config = configService.getConfigByKey("leave.carryover.max_days");
            return config.map(c -> c.getIntValue()).orElse(5);
        } catch (Exception e) {
            logger.warn("获取结转限制配置失败，使用默认值: {}", e.getMessage());
            return 5;
        }
    }

    /**
     * 假期类型信息
     */
    private static class LeaveTypeInfo {
        private final String name;
        private final boolean hasQuota;
        private final int defaultDays;

        public LeaveTypeInfo(String name, boolean hasQuota, int defaultDays) {
            this.name = name;
            this.hasQuota = hasQuota;
            this.defaultDays = defaultDays;
        }

        public String getName() { return name; }
        public boolean hasQuota() { return hasQuota; }
        public int getDefaultDays() { return defaultDays; }
        public boolean isProrated() { return hasQuota; }
    }
}