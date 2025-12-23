package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 年假规则配置实体
 * 支持基于工龄的阶梯式年假计算规则
 */
@Entity
@Table(name = "annual_leave_rule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnualLeaveRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 最小工龄（年），支持小数
     */
    @Column(name = "min_years", nullable = false, precision = 5, scale = 1)
    private BigDecimal minYears;

    /**
     * 最大工龄（年），支持小数，NULL表示无上限
     */
    @Column(name = "max_years", precision = 5, scale = 1)
    private BigDecimal maxYears;

    /**
     * 年假天数
     */
    @Column(name = "annual_days", nullable = false)
    private Integer annualDays;

    /**
     * 年假结转限制天数
     */
    @Column(name = "carryover_limit")
    private Integer carryoverLimit;

    /**
     * 是否启用
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * 优先级，数字越大优先级越高，用于处理重叠区间
     */
    @Column(name = "priority")
    private Integer priority;

    /**
     * 规则描述
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新人ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.priority == null) {
            this.priority = 0;
        }
        if (this.carryoverLimit == null) {
            this.carryoverLimit = 5;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查指定的工龄是否在规则范围内
     * @param seniorityYears 工龄年数
     * @return 是否在范围内
     */
    public boolean isInRange(BigDecimal seniorityYears) {
        if (seniorityYears == null) {
            return false;
        }

        boolean aboveMin = seniorityYears.compareTo(this.minYears) >= 0;
        boolean belowMax = this.maxYears == null ||
                          seniorityYears.compareTo(this.maxYears) < 0;

        return aboveMin && belowMax;
    }

    /**
     * 获取工龄范围描述
     * @return 范围描述字符串
     */
    public String getRangeDescription() {
        if (maxYears == null) {
            return String.format("%.1f年以上", minYears);
        }
        return String.format("%.1f-%.1f年", minYears, maxYears);
    }
}