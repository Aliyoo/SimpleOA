package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User approver;

    @ManyToOne
    private WorkTimeRecord workTimeRecord;

    @ManyToOne
    private LeaveRequest leaveRequest;

    @ManyToOne
    private BusinessTripRequest businessTripRequest;

    @ManyToOne
    private ReimbursementRequest reimbursementRequest;

    private String requestType; // WORKTIME, LEAVE, BUSINESS_TRIP, REIMBURSEMENT

    // Legacy status field (kept for backward compatibility during migration)
    private String status;

    // New unified design fields (Phase 0: adding, Phase 5: will remove legacy fields)
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 50)
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private ApprovalStatus approvalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_stage", length = 50)
    private ApprovalStage approvalStage;

    private String comment;
    private Date createTime;
    private Date updateTime;
    private Date approvalTime;

    // ===== Helper methods for migration period (Phase 0-4) =====

    /**
     * 判断是否为遗留数据（使用旧结构）
     *
     * @return true如果entityType为null（表示使用旧的外键字段）
     */
    public boolean isLegacy() {
        return entityType == null;
    }

    /**
     * 获取统一的审批状态
     * <p>
     * 优先使用新的approvalStatus枚举，如果为null则从legacy status字符串转换
     *
     * @return 审批状态，如果无法识别则返回null
     */
    public ApprovalStatus getUnifiedStatus() {
        if (approvalStatus != null) {
            return approvalStatus;
        }

        // 从legacy status字符串转换
        if (status != null) {
            try {
                return ApprovalStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * 设置统一的审批状态
     * <p>
     * 同时更新新的approvalStatus和旧的status字段（保持双写直到Phase 5）
     *
     * @param approvalStatus 审批状态
     */
    public void setUnifiedStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
        this.status = approvalStatus != null ? approvalStatus.name() : null;
    }
}