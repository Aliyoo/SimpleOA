package com.example.simpleoa.service;

import com.example.simpleoa.dto.leave.AnnualLeaveCalculation;
import com.example.simpleoa.model.AnnualLeaveRule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 年假规则服务接口
 * 提供年假规则的配置管理和年假计算功能
 */
public interface AnnualLeaveRuleService {

    // 规则管理相关方法

    /**
     * 创建年假规则
     * @param rule 年假规则
     * @param createdBy 创建人ID
     * @return 创建的年假规则
     */
    AnnualLeaveRule createRule(AnnualLeaveRule rule, Long createdBy);

    /**
     * 更新年假规则
     * @param rule 年假规则
     * @param updatedBy 更新人ID
     * @return 更新后的年假规则
     */
    AnnualLeaveRule updateRule(AnnualLeaveRule rule, Long updatedBy);

    /**
     * 删除年假规则
     * @param ruleId 规则ID
     * @return 是否删除成功
     */
    boolean deleteRule(Long ruleId);

    /**
     * 启用/禁用年假规则
     * @param ruleId 规则ID
     * @param enabled 是否启用
     * @param updatedBy 更新人ID
     * @return 更新后的规则
     */
    AnnualLeaveRule toggleRuleStatus(Long ruleId, boolean enabled, Long updatedBy);

    /**
     * 获取所有年假规则（按优先级排序）
     * @return 年假规则列表
     */
    List<AnnualLeaveRule> getAllRules();

    /**
     * 获取所有启用的年假规则
     * @return 启用的年假规则列表
     */
    List<AnnualLeaveRule> getActiveRules();

    /**
     * 根据ID获取年假规则
     * @param ruleId 规则ID
     * @return 年假规则
     */
    Optional<AnnualLeaveRule> getRuleById(Long ruleId);

    /**
     * 检查工龄范围是否重叠
     * @param minYears 最小工龄
     * @param maxYears 最大工龄
     * @param excludeId 排除的规则ID（用于更新时检查）
     * @return 是否重叠
     */
    boolean isRangeOverlapping(BigDecimal minYears, BigDecimal maxYears, Long excludeId);

    // 年假计算相关方法

    /**
     * 根据工龄获取适用的年假规则
     * @param seniorityYears 工龄年数
     * @return 适用的年假规则
     */
    Optional<AnnualLeaveRule> getApplicableRule(BigDecimal seniorityYears);

    /**
     * 计算用户指定年份的年假天数
     * @param userId 用户ID
     * @param year 年份
     * @return 年假计算结果
     */
    AnnualLeaveCalculation calculateAnnualLeave(Long userId, Integer year);

    /**
     * 计算用户当前年份的年假天数
     * @param userId 用户ID
     * @return 年假计算结果
     */
    AnnualLeaveCalculation calculateCurrentAnnualLeave(Long userId);

    /**
     * 批量计算多个用户的年假
     * @param userIds 用户ID列表
     * @param year 年份
     * @return 年假计算结果列表
     */
    List<AnnualLeaveCalculation> batchCalculateAnnualLeave(List<Long> userIds, Integer year);

    /**
     * 根据工龄获取年假天数
     * @param seniorityYears 工龄年数
     * @return 年假天数
     */
    Integer getAnnualLeaveDays(BigDecimal seniorityYears);

    /**
     * 获取年假规则统计信息
     * @return 统计信息
     */
    RuleStatistics getRuleStatistics();

    /**
     * 验证年假规则的有效性
     * @param rule 年假规则
     * @return 验证结果
     */
    ValidationResult validateRule(AnnualLeaveRule rule);

    /**
     * 获取年假规则描述
     * @param rule 年假规则
     * @return 规则描述
     */
    String getRuleDescription(AnnualLeaveRule rule);

    /**
     * 内部类：规则统计信息
     */
    class RuleStatistics {
        private final int totalRules;
        private final int activeRules;
        private final int inactiveRules;
        private final Integer minAnnualDays;
        private final Integer maxAnnualDays;
        private final Double averageAnnualDays;

        public RuleStatistics(int totalRules, int activeRules, int inactiveRules,
                            Integer minAnnualDays, Integer maxAnnualDays, Double averageAnnualDays) {
            this.totalRules = totalRules;
            this.activeRules = activeRules;
            this.inactiveRules = inactiveRules;
            this.minAnnualDays = minAnnualDays;
            this.maxAnnualDays = maxAnnualDays;
            this.averageAnnualDays = averageAnnualDays;
        }

        // Getters
        public int getTotalRules() { return totalRules; }
        public int getActiveRules() { return activeRules; }
        public int getInactiveRules() { return inactiveRules; }
        public Integer getMinAnnualDays() { return minAnnualDays; }
        public Integer getMaxAnnualDays() { return maxAnnualDays; }
        public Double getAverageAnnualDays() { return averageAnnualDays; }
    }

    /**
     * 内部类：验证结果
     */
    class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        // Getters
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }
}