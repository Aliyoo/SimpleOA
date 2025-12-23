package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 假期结转记录实体
 * 记录年假等可结转假期的结转历史和使用情况
 */
@Entity
@Table(name = "leave_balance_carryover",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "from_year", "to_year", "leave_type"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceCarryover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 来源年份
     */
    @Column(name = "from_year", nullable = false)
    private Integer fromYear;

    /**
     * 目标年份
     */
    @Column(name = "to_year", nullable = false)
    private Integer toYear;

    /**
     * 假期类型
     */
    @Column(name = "leave_type", nullable = false, length = 50)
    private String leaveType;

    /**
     * 结转天数
     */
    @Column(name = "carryover_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal carryoverDays;

    /**
     * 已使用结转天数
     */
    @Column(name = "used_days", precision = 4, scale = 1)
    private BigDecimal usedDays;

    /**
     * 是否已使用完
     */
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed;

    /**
     * 过期日期（结转年假通常到次年3月31日过期）
     */
    @Column(name = "expire_date")
    private LocalDate expireDate;

    /**
     * 应用的结转规则ID
     */
    @Column(name = "carryover_rule_id")
    private Long carryoverRuleId;

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
        if (this.usedDays == null) {
            this.usedDays = BigDecimal.ZERO;
        }
        if (this.isUsed == null) {
            this.isUsed = false;
        }
        // 如果未设置过期日期，默认为次年3月31日
        if (this.expireDate == null && this.toYear != null) {
            this.expireDate = LocalDate.of(this.toYear, 3, 31);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        // 更新使用状态
        if (this.usedDays != null && this.carryoverDays != null) {
            this.isUsed = this.usedDays.compareTo(this.carryoverDays) >= 0;
        }
    }

    /**
     * 获取剩余结转天数
     * @return 剩余天数
     */
    public BigDecimal getRemainingDays() {
        if (usedDays == null || carryoverDays == null) {
            return carryoverDays != null ? carryoverDays : BigDecimal.ZERO;
        }
        BigDecimal remaining = carryoverDays.subtract(usedDays);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    /**
     * 检查是否已过期
     * @return 是否过期
     */
    public boolean isExpired() {
        if (expireDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(expireDate);
    }

    /**
     * 使用指定数量的结转天数
     * @param daysToUse 要使用的天数
     * @return 实际使用的天数（不会超过剩余天数）
     */
    public BigDecimal useCarryoverDays(BigDecimal daysToUse) {
        if (daysToUse == null || daysToUse.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal availableDays = getRemainingDays();
        if (availableDays.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal actualUsed = daysToUse.min(availableDays);
        this.usedDays = this.usedDays.add(actualUsed);

        return actualUsed;
    }

    /**
     * 获取结转描述
     * @return 描述字符串
     */
    public String getDescription() {
        return String.format("%d年→%d年 %s结转 %.1f天",
            fromYear, toYear, getLeaveTypeDisplayName(), carryoverDays);
    }

    /**
     * 获取假期类型显示名称
     * @return 显示名称
     */
    private String getLeaveTypeDisplayName() {
        if (leaveType == null) {
            return "未知";
        }
        switch (leaveType) {
            case "ANNUAL_LEAVE":
                return "年假";
            case "SICK_LEAVE":
                return "病假";
            case "PERSONAL_LEAVE":
                return "事假";
            default:
                return leaveType;
        }
    }
}