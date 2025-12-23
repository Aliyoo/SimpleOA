package com.example.simpleoa.event;

import com.example.simpleoa.model.ApprovalFlow;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 审批拒绝事件
 *
 * <p>当审批流程被拒绝时发布此事件
 * <p>包含拒绝原因信息，用于通知申请人
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Getter
public class ApprovalRejectedEvent implements DomainEvent {

    /**
     * 审批流程
     */
    private final ApprovalFlow approvalFlow;

    /**
     * 审批实体
     */
    private final Object entity;

    /**
     * 实体类型
     */
    private final String entityType;

    /**
     * 实体ID
     */
    private final Long entityId;

    /**
     * 审批人ID
     */
    private final Long approverId;

    /**
     * 拒绝原因
     */
    private final String reason;

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;

    /**
     * 构造函数
     *
     * @param approvalFlow 审批流程
     * @param entity 审批实体
     * @param reason 拒绝原因
     */
    public ApprovalRejectedEvent(ApprovalFlow approvalFlow, Object entity, String reason) {
        this.approvalFlow = approvalFlow;
        this.entity = entity;
        this.entityType = approvalFlow.getEntityType() != null
            ? approvalFlow.getEntityType().getCode()
            : null;
        this.entityId = approvalFlow.getEntityId();
        this.approverId = approvalFlow.getApprover() != null
            ? approvalFlow.getApprover().getId()
            : null;
        this.reason = reason;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
