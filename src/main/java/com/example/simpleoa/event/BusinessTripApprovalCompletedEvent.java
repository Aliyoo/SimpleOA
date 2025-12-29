package com.example.simpleoa.event;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.BusinessTripRequest;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 出差审批完成事件
 *
 * <p>当出差审批通过时发布此事件
 * <p>监听器可以：
 * - 发送通知给申请人
 * - 更新项目统计
 * - 触发其他业务逻辑（如行程安排、费用预算等）
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
public class BusinessTripApprovalCompletedEvent extends ApplicationEvent implements DomainEvent {

    private static final long serialVersionUID = 1L;

    private final ApprovalFlow approvalFlow;
    private final BusinessTripRequest businessTripRequest;
    private final LocalDateTime occurredOn;

    /**
     * 创建出差审批完成事件
     *
     * @param source              事件发布者
     * @param approvalFlow        审批流程
     * @param businessTripRequest 出差申请
     */
    public BusinessTripApprovalCompletedEvent(Object source, ApprovalFlow approvalFlow, BusinessTripRequest businessTripRequest) {
        super(source);
        this.approvalFlow = approvalFlow;
        this.businessTripRequest = businessTripRequest;
        this.occurredOn = LocalDateTime.now();
    }

    public ApprovalFlow getApprovalFlow() {
        return approvalFlow;
    }

    public BusinessTripRequest getBusinessTripRequest() {
        return businessTripRequest;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String toString() {
        return "BusinessTripApprovalCompletedEvent{" +
                "businessTripId=" + businessTripRequest.getId() +
                ", applicantId=" + (businessTripRequest.getApplicant() != null ? businessTripRequest.getApplicant().getId() : null) +
                ", projectId=" + (businessTripRequest.getProject() != null ? businessTripRequest.getProject().getId() : null) +
                ", destination=" + businessTripRequest.getDestination() +
                ", startTime=" + businessTripRequest.getStartTime() +
                ", endTime=" + businessTripRequest.getEndTime() +
                ", days=" + businessTripRequest.getDays() +
                '}';
    }
}
