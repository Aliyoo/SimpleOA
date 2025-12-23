package com.example.simpleoa.controller;

import com.example.simpleoa.dto.leave.AnnualLeaveCalculation;
import com.example.simpleoa.dto.leave.InitializationResult;
import com.example.simpleoa.dto.leave.SeniorityInfo;
import com.example.simpleoa.model.LeaveBalance;
import com.example.simpleoa.model.LeaveBalanceAdjustment;
import com.example.simpleoa.model.LeaveBalanceCarryover;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.AnnualLeaveRuleService;
import com.example.simpleoa.service.LeaveBalanceInitializationService;
import com.example.simpleoa.service.SeniorityCalculationService;
import com.example.simpleoa.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 假期余额管理控制器
 * 提供假期余额的查询、调整、初始化等功能
 */
@RestController
@RequestMapping("/api/leave/balance")
@CrossOrigin(origins = "*")
public class LeaveBalanceController {

    private static final Logger logger = LoggerFactory.getLogger(LeaveBalanceController.class);

    @Autowired
    private LeaveBalanceInitializationService balanceInitializationService;

    @Autowired
    private SeniorityCalculationService seniorityCalculationService;

    @Autowired
    private AnnualLeaveRuleService annualLeaveRuleService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.example.simpleoa.repository.LeaveBalanceRepository leaveBalanceRepository;

    /**
     * 查询用户的假期余额
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<Page<LeaveBalance>> getUserBalances(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String leaveType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            // 权限检查：员工只能查看自己的余额
            boolean isAdmin = hasAdminRole(currentUser);
            int targetYear = (year != null) ? year : LocalDate.now().getYear();
            
            List<com.example.simpleoa.model.LeaveBalance> balanceList;
            
            if (isAdmin) {
                // 管理员可以查询所有用户
                balanceList = leaveBalanceRepository.findByYear(targetYear);
            } else {
                // 普通用户只能查看自己的
                balanceList = leaveBalanceRepository.findByUserIdAndYear(currentUser.getId(), targetYear);
            }
            
            // 如果指定了leaveType,进行过滤
            if (leaveType != null && !leaveType.isEmpty()) {
                try {
                    com.example.simpleoa.model.LeaveType leaveTypeEnum = 
                        com.example.simpleoa.model.LeaveType.valueOf(leaveType);
                    balanceList = balanceList.stream()
                        .filter(b -> b.getLeaveType() == leaveTypeEnum)
                        .collect(java.util.stream.Collectors.toList());
                } catch (IllegalArgumentException e) {
                    logger.warn("无效的请假类型: {}", leaveType);
                }
            }
            
            // 分页处理
            int start = Math.min(page * size, balanceList.size());
            int end = Math.min(start + size, balanceList.size());
            List<com.example.simpleoa.model.LeaveBalance> pageContent = balanceList.subList(start, end);
            
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<com.example.simpleoa.model.LeaveBalance> balances = 
                new org.springframework.data.domain.PageImpl<>(pageContent, pageable, balanceList.size());

            return ResponseEntity.ok(balances);
        } catch (Exception e) {
            logger.error("查询用户假期余额失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取用户指定年份的所有假期余额
     */
    @GetMapping("/user/{userId}/year/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<LeaveBalance>> getUserYearlyBalances(
            @PathVariable Long userId,
            @PathVariable Integer year,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            // 权限检查
            if (!hasAdminRole(currentUser) && !userId.equals(currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            // 这里需要调用余额查询服务
            List<LeaveBalance> balances = List.of(); // 暂时返回空列表
            return ResponseEntity.ok(balances);
        } catch (Exception e) {
            logger.error("查询用户年度假期余额失败，用户ID: {}, 年份: {}", userId, year, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取用户的年假计算详情
     */
    @GetMapping("/user/{userId}/annual/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<AnnualLeaveCalculation> getUserAnnualLeaveDetails(
            @PathVariable Long userId,
            @PathVariable Integer year,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            // 权限检查
            if (!hasAdminRole(currentUser) && !userId.equals(currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            AnnualLeaveCalculation calculation = annualLeaveRuleService.calculateAnnualLeave(userId, year);
            return ResponseEntity.ok(calculation);
        } catch (Exception e) {
            logger.error("获取用户年假计算详情失败，用户ID: {}, 年份: {}", userId, year, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取用户工龄信息
     */
    @GetMapping("/seniority/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<SeniorityInfo> getUserSeniority(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate calculateDate,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            // 权限检查
            if (!hasAdminRole(currentUser) && !userId.equals(currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            LocalDate targetDate = (calculateDate != null) ? calculateDate : LocalDate.now();
            SeniorityInfo seniorityInfo = seniorityCalculationService.calculateSeniority(userId, targetDate);
            return ResponseEntity.ok(seniorityInfo);
        } catch (Exception e) {
            logger.error("获取用户工龄信息失败，用户ID: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 调整用户假期余额
     */
    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> adjustUserBalance(
            @Valid @RequestBody BalanceAdjustmentRequest request,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            // 这里需要调用余额调整服务
            Map<String, Object> result = Map.of(
                "success", false,
                "message", "功能待实现"
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("调整用户假期余额失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量初始化年度假期余额
     */
    @PostMapping("/initialize/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> initializeYearlyBalances(
            @PathVariable Integer year,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());
            }

            return balanceInitializationService.batchInitializeYearlyBalances(year)
                .thenApply(result -> {
                    boolean hasErrors = result.getFailureCount() > 0;
                    String message;
                    
                    if (hasErrors) {
                        message = String.format("批量初始化完成: 成功 %d 人,失败 %d 人。请查看详细错误列表",
                            result.getSuccessCount(), result.getFailureCount());
                    } else {
                        message = String.format("批量初始化成功完成,共初始化 %d 名员工的假期余额",
                            result.getSuccessCount());
                    }
                    
                    Map<String, Object> response = Map.of(
                        "success", !hasErrors,
                        "message", message,
                        "result", result,
                        "hasPartialFailure", hasErrors,
                        "errorCount", result.getFailureCount(),
                        "errors", result.getErrors() != null ? result.getErrors() : List.of()
                    );
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    logger.error("批量初始化假期余额失败", e);
                    Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "批量初始化系统错误: " + e.getMessage(),
                        "errors", List.of(e.getMessage())
                    );
                    return ResponseEntity.internalServerError().body(response);
                });
        } catch (Exception e) {
            logger.error("启动批量初始化失败", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.internalServerError().build()
            );
        }
    }

    /**
     * 初始化单个用户的假期余额
     */
    @PostMapping("/initialize/{userId}/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> initializeUserBalance(
            @PathVariable Long userId,
            @PathVariable Integer year,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            boolean success = balanceInitializationService.initializeUserYearlyBalance(userId, year);
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "用户假期余额初始化成功" : "用户假期余额初始化失败"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("初始化用户假期余额失败，用户ID: {}, 年份: {}", userId, year, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 新员工入职初始化假期余额
     */
    @PostMapping("/initialize/new-employee/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> initializeNewEmployeeBalance(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate hireDate,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            boolean success = balanceInitializationService.initializeNewEmployeeBalance(userId, hireDate);
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "新员工假期余额初始化成功" : "新员工假期余额初始化失败"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("初始化新员工假期余额失败，用户ID: {}, 入职日期: {}", userId, hireDate, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 执行假期结转
     */
    @PostMapping("/carryover")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> executeCarryover(
            @RequestParam Integer fromYear,
            @RequestParam Integer toYear,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());
            }

            return balanceInitializationService.executeCarryover(fromYear, toYear)
                .thenApply(result -> {
                    Map<String, Object> response = Map.of(
                        "success", true,
                        "message", "假期结转完成",
                        "result", result
                    );
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    logger.error("执行假期结转失败", e);
                    Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "假期结转失败: " + e.getMessage()
                    );
                    return ResponseEntity.internalServerError().body(response);
                });
        } catch (Exception e) {
            logger.error("启动假期结转失败", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.internalServerError().build()
            );
        }
    }

    /**
     * 重新计算用户假期余额
     */
    @PostMapping("/recalculate/{userId}/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> recalculateUserBalance(
            @PathVariable Long userId,
            @PathVariable Integer year,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            boolean success = balanceInitializationService.recalculateUserBalance(userId, year);
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "用户假期余额重新计算成功" : "用户假期余额重新计算失败"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("重新计算用户假期余额失败，用户ID: {}, 年份: {}", userId, year, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取假期余额初始化状态报告
     */
    @GetMapping("/initialization-status/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<LeaveBalanceInitializationService.InitializationStatusReport> getInitializationStatus(
            @PathVariable Integer year) {

        try {
            LeaveBalanceInitializationService.InitializationStatusReport report =
                balanceInitializationService.getInitializationStatus(year);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("获取假期余额初始化状态报告失败，年份: {}", year, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 修复不一致的假期余额数据
     */
    @PostMapping("/repair/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> repairInconsistentBalances(
            @PathVariable Integer year,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());
            }

            return balanceInitializationService.repairInconsistentBalances(year)
                .thenApply(result -> {
                    Map<String, Object> response = Map.of(
                        "success", true,
                        "message", "假期余额数据修复完成",
                        "result", result
                    );
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    logger.error("修复假期余额数据失败", e);
                    Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "假期余额数据修复失败: " + e.getMessage()
                    );
                    return ResponseEntity.internalServerError().body(response);
                });
        } catch (Exception e) {
            logger.error("启动假期余额数据修复失败", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.internalServerError().build()
            );
        }
    }

    /**
     * 获取假期余额统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> getBalanceStatistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String departmentId) {

        try {
            // 这里需要实现统计逻辑
            Map<String, Object> statistics = Map.of(
                "totalUsers", 0,
                "initializedUsers", 0,
                "uninitializedUsers", 0,
                "initRate", 0.0
            );

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("获取假期余额统计信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 私有辅助方法

    /**
     * 获取当前用户
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        return userService.getUserByUsername(username);
    }

    /**
     * 检查用户是否有管理员角色
     */
    private boolean hasAdminRole(User user) {
        return user.getRoles() != null && user.getRoles().stream()
            .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || "HR_MANAGER".equals(role.getName()));
    }

    /**
     * 余额调整请求DTO
     */
    public static class BalanceAdjustmentRequest {
        private Long userId;
        private Integer year;
        private String leaveType;
        private String adjustmentType;
        private BigDecimal adjustmentDays;
        private String reason;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public String getLeaveType() { return leaveType; }
        public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
        public String getAdjustmentType() { return adjustmentType; }
        public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }
        public BigDecimal getAdjustmentDays() { return adjustmentDays; }
        public void setAdjustmentDays(BigDecimal adjustmentDays) { this.adjustmentDays = adjustmentDays; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}