package com.example.simpleoa.dto.leave;

import com.example.simpleoa.model.AnnualLeaveRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 年假计算结果DTO
 * 包含年假计算的完整信息和结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnualLeaveCalculation {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 工龄年数
     */
    private Double seniorityYears;

    /**
     * 基础年假天数
     */
    private Integer baseDays;

    /**
     * 结转年假天数
     */
    private BigDecimal carryoverDays;

    /**
     * 总年假天数（基础+结转）
     */
    private BigDecimal totalDays;

    /**
     * 应用的年假规则
     */
    private AnnualLeaveRule appliedRule;

    /**
     * 计算描述
     */
    private String description;

    /**
     * 获取年假描述
     * @return 描述字符串
     */
    public String getAnnualLeaveDescription() {
        StringBuilder sb = new StringBuilder();

        if (baseDays != null) {
            sb.append("基础年假").append(baseDays).append("天");
        }

        if (carryoverDays != null && carryoverDays.compareTo(BigDecimal.ZERO) > 0) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append("结转年假").append(carryoverDays).append("天");
        }

        if (totalDays != null) {
            sb.append(" = 总计").append(totalDays).append("天");
        }

        if (appliedRule != null) {
            sb.append("（").append(appliedRule.getDescription()).append("）");
        }

        return sb.toString();
    }

    /**
     * 检查是否有结转年假
     * @return 是否有结转年假
     */
    public boolean hasCarryover() {
        return carryoverDays != null && carryoverDays.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 获取基础年假描述
     * @return 基础年假描述
     */
    public String getBaseLeaveDescription() {
        if (appliedRule != null) {
            return String.format("工龄%.1f年，基础年假%d天（%s）",
                seniorityYears != null ? seniorityYears : 0.0,
                baseDays != null ? baseDays : 0,
                appliedRule.getDescription());
        }
        return String.format("基础年假%d天", baseDays != null ? baseDays : 0);
    }

    /**
     * 获取结转年假描述
     * @return 结转年假描述
     */
    public String getCarryoverDescription() {
        if (!hasCarryover()) {
            return "无结转年假";
        }
        return String.format("从%d年结转%.1f天", year - 1, carryoverDays);
    }
}