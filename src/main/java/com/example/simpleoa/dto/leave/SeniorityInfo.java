package com.example.simpleoa.dto.leave;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 工龄信息DTO
 * 包含员工工龄计算的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeniorityInfo {

    /**
     * 精确工龄年数（包含小数）
     */
    private Double seniorityYears;

    /**
     * 完整年数
     */
    private Integer years;

    /**
     * 完整月数
     */
    private Integer months;

    /**
     * 完整天数
     */
    private Integer days;

    /**
     * 入职日期
     */
    private LocalDate hireDate;

    /**
     * 计算基准日期
     */
    private LocalDate calculateDate;

    /**
     * 获取工龄描述
     * @return 工龄描述字符串
     */
    public String getSeniorityDescription() {
        if (seniorityYears == null) {
            return "未知";
        }

        if (years == null || months == null) {
            return String.format("%.1f年", seniorityYears);
        }

        if (years > 0 && months > 0) {
            return String.format("%d年%d个月", years, months);
        } else if (years > 0) {
            return String.format("%d年", years);
        } else if (months > 0) {
            return String.format("%d个月", months);
        } else {
            return "不足1个月";
        }
    }

    /**
     * 检查是否为试用期员工（通常工龄小于1年）
     * @return 是否为试用期员工
     */
    public boolean isProbation() {
        return seniorityYears != null && seniorityYears < 1.0;
    }

    /**
     * 检查是否为正式员工（通常工龄大于等于1年）
     * @return 是否为正式员工
     */
    public boolean isFormal() {
        return seniorityYears != null && seniorityYears >= 1.0;
    }

    /**
     * 检查是否为长期员工（通常工龄大于等于5年）
     * @return 是否为长期员工
     */
    public boolean isLongTerm() {
        return seniorityYears != null && seniorityYears >= 5.0;
    }

    /**
     * 检查是否为资深员工（通常工龄大于等于10年）
     * @return 是否为资深员工
     */
    public boolean isSenior() {
        return seniorityYears != null && seniorityYears >= 10.0;
    }
}