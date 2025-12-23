package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 假期余额调整记录实体
 * 记录手动调整假期余额的审计信息
 */
@Entity
@Table(name = "leave_balance_adjustment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 年份
     */
    @Column(name = "year", nullable = false)
    private Integer year;

    /**
     * 假期类型
     */
    @Column(name = "leave_type", nullable = false, length = 50)
    private String leaveType;

    /**
     * 调整类型：INCREASE, DECREASE
     */
    @Column(name = "adjustment_type", nullable = false, length = 20)
    private String adjustmentType;

    /**
     * 调整天数
     */
    @Column(name = "adjustment_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal adjustmentDays;

    /**
     * 调整前总天数
     */
    @Column(name = "before_total_days", precision = 4, scale = 1)
    private BigDecimal beforeTotalDays;

    /**
     * 调整后总天数
     */
    @Column(name = "after_total_days", precision = 4, scale = 1)
    private BigDecimal afterTotalDays;

    /**
     * 调整前剩余天数
     */
    @Column(name = "before_remaining_days", precision = 4, scale = 1)
    private BigDecimal beforeRemainingDays;

    /**
     * 调整后剩余天数
     */
    @Column(name = "after_remaining_days", precision = 4, scale = 1)
    private BigDecimal afterRemainingDays;

    /**
     * 调整原因
     */
    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    /**
     * 审批人ID
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    /**
     * 审批时间
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * 创建人ID
     */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * 获取调整类型显示名称
     * @return 显示名称
     */
    public String getAdjustmentTypeDisplayName() {
        if ("INCREASE".equals(adjustmentType)) {
            return "增加";
        } else if ("DECREASE".equals(adjustmentType)) {
            return "减少";
        }
        return adjustmentType;
    }

    /**
     * 获取假期类型显示名称
     * @return 显示名称
     */
    public String getLeaveTypeDisplayName() {
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

    /**
     * 检查是否已审批
     * @return 是否已审批
     */
    public boolean isApproved() {
        return approvedAt != null && approvedBy != null;
    }

    /**
     * 获取调整描述
     * @return 描述字符串
     */
    public String getDescription() {
        return String.format("%s %d年 %s %.1f天",
            getAdjustmentTypeDisplayName(), year, getLeaveTypeDisplayName(), adjustmentDays);
    }
}