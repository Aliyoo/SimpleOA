package com.example.simpleoa.event;

import com.example.simpleoa.model.ApprovalFlow;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 审批完成事件
 *
 * <p>当审批流程完成（通过或拒绝）时发布此事件
 * <p>用于解耦审批后的业务逻辑处理（如扣除预算、发送通知等）
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Getter
public class ApprovalCompletedEvent implements DomainEvent {

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
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;

    /**
     * 构造函数
     *
     * @param approvalFlow 审批流程
     * @param entity 审批实体
     */
    public ApprovalCompletedEvent(ApprovalFlow approvalFlow, Object entity) {
        this.approvalFlow = approvalFlow;
        this.entity = entity;
        this.entityType = approvalFlow.getEntityType() != null
            ? approvalFlow.getEntityType().getCode()
            : null;
        this.entityId = approvalFlow.getEntityId();
        this.approverId = approvalFlow.getApprover() != null
            ? approvalFlow.getApprover().getId()
            : null;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
