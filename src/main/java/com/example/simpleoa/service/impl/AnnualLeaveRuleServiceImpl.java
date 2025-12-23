package com.example.simpleoa.service.impl;

import com.example.simpleoa.dto.leave.AnnualLeaveCalculation;
import com.example.simpleoa.model.AnnualLeaveRule;
import com.example.simpleoa.repository.AnnualLeaveRuleRepository;
import com.example.simpleoa.repository.LeaveBalanceCarryoverRepository;
import com.example.simpleoa.service.AnnualLeaveRuleService;
import com.example.simpleoa.service.SeniorityCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 年假规则服务实现类
 * 提供年假规则的配置管理和基于工龄的年假计算功能
 */
@Service
@Transactional
public class AnnualLeaveRuleServiceImpl implements AnnualLeaveRuleService {

    private static final Logger logger = LoggerFactory.getLogger(AnnualLeaveRuleServiceImpl.class);

    @Autowired
    private AnnualLeaveRuleRepository annualLeaveRuleRepository;

    @Autowired
    private LeaveBalanceCarryoverRepository carryoverRepository;

    @Autowired
    private SeniorityCalculationService seniorityCalculationService;

    @Override
    @CacheEvict(value = "annual_leave_rules", allEntries = true)
    public AnnualLeaveRule createRule(AnnualLeaveRule rule, Long createdBy) {
        logger.info("创建年假规则: minYears={}, maxYears={}, annualDays={}",
            rule.getMinYears(), rule.getMaxYears(), rule.getAnnualDays());

        // 验证规则
        ValidationResult validation = validateRule(rule);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("规则验证失败: " + String.join(", ", validation.getErrors()));
        }

        // 检查范围重叠
        if (isRangeOverlapping(rule.getMinYears(), rule.getMaxYears(), null)) {
            throw new IllegalArgumentException("工龄范围与现有规则重叠");
        }

        rule.setCreatedBy(createdBy);
        rule.setCreatedAt(LocalDateTime.now());

        AnnualLeaveRule savedRule = annualLeaveRuleRepository.save(rule);
        logger.info("年假规则创建成功，ID: {}", savedRule.getId());

        return savedRule;
    }

    @Override
    @CacheEvict(value = "annual_leave_rules", allEntries = true)
    public AnnualLeaveRule updateRule(AnnualLeaveRule rule, Long updatedBy) {
        logger.info("更新年假规则 ID: {}", rule.getId());

        if (!annualLeaveRuleRepository.existsById(rule.getId())) {
            throw new IllegalArgumentException("规则不存在，ID: " + rule.getId());
        }

        // 验证规则
        ValidationResult validation = validateRule(rule);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("规则验证失败: " + String.join(", ", validation.getErrors()));
        }

        // 检查范围重叠（排除自身）
        if (isRangeOverlapping(rule.getMinYears(), rule.getMaxYears(), rule.getId())) {
            throw new IllegalArgumentException("工龄范围与现有规则重叠");
        }

        rule.setUpdatedBy(updatedBy);
        rule.setUpdatedAt(LocalDateTime.now());

        AnnualLeaveRule savedRule = annualLeaveRuleRepository.save(rule);
        logger.info("年假规则更新成功，ID: {}", savedRule.getId());

        return savedRule;
    }

    @Override
    @CacheEvict(value = "annual_leave_rules", allEntries = true)
    public boolean deleteRule(Long ruleId) {
        logger.info("删除年假规则 ID: {}", ruleId);

        if (!annualLeaveRuleRepository.existsById(ruleId)) {
            logger.warn("规则不存在，ID: {}", ruleId);
            return false;
        }

        try {
            annualLeaveRuleRepository.deleteById(ruleId);
            logger.info("年假规则删除成功，ID: {}", ruleId);
            return true;
        } catch (Exception e) {
            logger.error("删除年假规则失败，ID: {}", ruleId, e);
            return false;
        }
    }

    @Override
    @CacheEvict(value = "annual_leave_rules", allEntries = true)
    public AnnualLeaveRule toggleRuleStatus(Long ruleId, boolean enabled, Long updatedBy) {
        logger.info("{}年假规则 ID: {}", enabled ? "启用" : "禁用", ruleId);

        Optional<AnnualLeaveRule> ruleOpt = annualLeaveRuleRepository.findById(ruleId);
        if (!ruleOpt.isPresent()) {
            throw new IllegalArgumentException("规则不存在，ID: " + ruleId);
        }

        AnnualLeaveRule rule = ruleOpt.get();
        rule.setIsActive(enabled);
        rule.setUpdatedBy(updatedBy);
        rule.setUpdatedAt(LocalDateTime.now());

        AnnualLeaveRule savedRule = annualLeaveRuleRepository.save(rule);
        logger.info("年假规则状态更新成功，ID: {}, 状态: {}", savedRule.getId(), savedRule.getIsActive());

        return savedRule;
    }

    @Override
    @Cacheable(value = "annual_leave_rules", key = "'all'")
    public List<AnnualLeaveRule> getAllRules() {
        logger.debug("获取所有年假规则");
        return annualLeaveRuleRepository.findAllOrderedByRange();
    }

    @Override
    @Cacheable(value = "annual_leave_rules", key = "'active'")
    public List<AnnualLeaveRule> getActiveRules() {
        logger.debug("获取启用的年假规则");
        return annualLeaveRuleRepository.findByIsActiveTrueOrderByPriorityDesc();
    }

    @Override
    @Cacheable(value = "annual_leave_rules", key = "#ruleId")
    public Optional<AnnualLeaveRule> getRuleById(Long ruleId) {
        logger.debug("获取年假规则 ID: {}", ruleId);
        return annualLeaveRuleRepository.findById(ruleId);
    }

    @Override
    public boolean isRangeOverlapping(BigDecimal minYears, BigDecimal maxYears, Long excludeId) {
        List<AnnualLeaveRule> existingRules;

        if (excludeId != null) {
            existingRules = annualLeaveRuleRepository.findByIsActiveTrueOrderByPriorityDesc()
                .stream()
                .filter(rule -> !rule.getId().equals(excludeId))
                .collect(Collectors.toList());
        } else {
            existingRules = annualLeaveRuleRepository.findByIsActiveTrueOrderByPriorityDesc();
        }

        for (AnnualLeaveRule existingRule : existingRules) {
            if (isRangesOverlapping(minYears, maxYears, existingRule.getMinYears(), existingRule.getMaxYears())) {
                logger.debug("发现重叠规则: 新规则[{}-{}] 与 现有规则[{}-{}] 重叠",
                    minYears, maxYears, existingRule.getMinYears(), existingRule.getMaxYears());
                return true;
            }
        }

        return false;
    }

    @Override
    @Cacheable(value = "applicable_rule", key = "#seniorityYears")
    public Optional<AnnualLeaveRule> getApplicableRule(BigDecimal seniorityYears) {
        logger.debug("查找工龄 {} 对应的年假规则", seniorityYears);

        return annualLeaveRuleRepository.findBestApplicableRule(seniorityYears);
    }

    @Override
    @Cacheable(value = "annual_leave_calculation", key = "#userId + ':' + #year")
    public AnnualLeaveCalculation calculateAnnualLeave(Long userId, Integer year) {
        logger.debug("计算用户 {} 年 {} 的年假", userId, year);

        // 计算年初工龄
        var seniorityInfo = seniorityCalculationService.calculateYearStartSeniority(userId, year);
        BigDecimal seniorityYears = BigDecimal.valueOf(seniorityInfo.getSeniorityYears());

        // 查找适用的年假规则
        Optional<AnnualLeaveRule> applicableRuleOpt = getApplicableRule(seniorityYears);
        if (!applicableRuleOpt.isPresent()) {
            logger.warn("未找到用户 {} 工龄 {} 对应的年假规则", userId, seniorityYears);
            throw new IllegalStateException("未找到适用的年假规则");
        }

        AnnualLeaveRule applicableRule = applicableRuleOpt.get();

        // 计算结转天数
        BigDecimal carryoverDays = calculateCarryoverDays(userId, year - 1);

        // 构建计算结果
        return AnnualLeaveCalculation.builder()
            .userId(userId)
            .year(year)
            .seniorityYears(seniorityInfo.getSeniorityYears())
            .baseDays(applicableRule.getAnnualDays())
            .carryoverDays(carryoverDays)
            .totalDays(BigDecimal.valueOf(applicableRule.getAnnualDays()).add(carryoverDays))
            .appliedRule(applicableRule)
            .build();
    }

    @Override
    public AnnualLeaveCalculation calculateCurrentAnnualLeave(Long userId) {
        int currentYear = java.time.Year.now().getValue();
        return calculateAnnualLeave(userId, currentYear);
    }

    @Override
    public List<AnnualLeaveCalculation> batchCalculateAnnualLeave(List<Long> userIds, Integer year) {
        logger.info("批量计算 {} 个用户 {} 年的年假", userIds.size(), year);

        return userIds.stream()
            .map(userId -> {
                try {
                    return calculateAnnualLeave(userId, year);
                } catch (Exception e) {
                    logger.error("计算用户 {} 年假失败: {}", userId, e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public Integer getAnnualLeaveDays(BigDecimal seniorityYears) {
        Optional<AnnualLeaveRule> ruleOpt = getApplicableRule(seniorityYears);
        return ruleOpt.map(AnnualLeaveRule::getAnnualDays).orElse(0);
    }

    @Override
    @Cacheable(value = "rule_statistics", key = "'statistics'")
    public RuleStatistics getRuleStatistics() {
        List<AnnualLeaveRule> allRules = annualLeaveRuleRepository.findAll();
        List<AnnualLeaveRule> activeRules = allRules.stream()
            .filter(AnnualLeaveRule::getIsActive)
            .collect(Collectors.toList());

        int totalRules = allRules.size();
        int activeRuleCount = activeRules.size();
        int inactiveRuleCount = totalRules - activeRuleCount;

        Integer minDays = annualLeaveRuleRepository.findMinAnnualDays();
        Integer maxDays = annualLeaveRuleRepository.findMaxAnnualDays();
        Double averageDays = activeRules.stream()
            .mapToInt(AnnualLeaveRule::getAnnualDays)
            .average()
            .orElse(0.0);

        return new RuleStatistics(totalRules, activeRuleCount, inactiveRuleCount, minDays, maxDays, averageDays);
    }

    @Override
    public ValidationResult validateRule(AnnualLeaveRule rule) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 必填字段检查
        if (rule.getMinYears() == null) {
            errors.add("最小工龄不能为空");
        }

        if (rule.getAnnualDays() == null) {
            errors.add("年假天数不能为空");
        }

        if (errors.isEmpty()) {
            // 数值范围检查
            if (rule.getMinYears().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("最小工龄不能为负数");
            }

            if (rule.getMaxYears() != null && rule.getMaxYears().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("最大工龄不能为负数");
            }

            if (rule.getMaxYears() != null && rule.getMinYears().compareTo(rule.getMaxYears()) >= 0) {
                errors.add("最大工龄必须大于最小工龄");
            }

            if (rule.getAnnualDays() <= 0) {
                errors.add("年假天数必须大于0");
            }

            if (rule.getAnnualDays() > 30) {
                warnings.add("年假天数超过30天，请确认是否正确");
            }

            if (rule.getCarryoverLimit() != null && rule.getCarryoverLimit() > 15) {
                warnings.add("结转限制超过15天，请确认是否正确");
            }

            // 优先级检查
            if (rule.getPriority() != null && rule.getPriority() < 0) {
                warnings.add("优先级为负数，建议使用非负数");
            }
        }

        boolean isValid = errors.isEmpty();
        logger.debug("规则验证结果: valid={}, errors={}, warnings={}", isValid, errors.size(), warnings.size());

        return new ValidationResult(isValid, errors, warnings);
    }

    @Override
    public String getRuleDescription(AnnualLeaveRule rule) {
        if (rule == null) {
            return "未知规则";
        }

        StringBuilder desc = new StringBuilder();
        desc.append("工龄").append(rule.getRangeDescription())
            .append("，年假").append(rule.getAnnualDays()).append("天");

        if (rule.getCarryoverLimit() != null) {
            desc.append("，结转限制").append(rule.getCarryoverLimit()).append("天");
        }

        if (rule.getDescription() != null && !rule.getDescription().trim().isEmpty()) {
            desc.append("（").append(rule.getDescription()).append("）");
        }

        return desc.toString();
    }

    /**
     * 检查两个工龄范围是否重叠
     */
    private boolean isRangesOverlapping(BigDecimal min1, BigDecimal max1, BigDecimal min2, BigDecimal max2) {
        // 检查是否重叠：如果max1 <= min2 或 max2 <= min1，则不重叠
        boolean noOverlap = (max1 != null && max1.compareTo(min2) <= 0) ||
                          (max2 != null && max2.compareTo(min1) <= 0);

        return !noOverlap;
    }

    /**
     * 计算用户指定年份的结转天数
     */
    private BigDecimal calculateCarryoverDays(Long userId, Integer fromYear) {
        try {
            Double totalDays = carryoverRepository.sumCarryoverDaysByUserAndYearAndType(userId, fromYear + 1, "ANNUAL_LEAVE");
            return totalDays != null ? BigDecimal.valueOf(totalDays) : BigDecimal.ZERO;
        } catch (Exception e) {
            logger.error("计算用户 {} 年 {} 结转天数失败: {}", userId, fromYear, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}