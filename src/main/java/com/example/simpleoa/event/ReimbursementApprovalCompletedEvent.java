package com.example.simpleoa.event;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.ReimbursementRequest;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 报销审批完成事件
 *
 * <p>当报销审批最终通过时发布此事件
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
public class ReimbursementApprovalCompletedEvent extends ApplicationEvent implements DomainEvent {

    private static final long serialVersionUID = 1L;

    private final ApprovalFlow approvalFlow;
    private final ReimbursementRequest reimbursementRequest;
    private final LocalDateTime occurredOn;

    public ReimbursementApprovalCompletedEvent(Object source, ApprovalFlow approvalFlow, ReimbursementRequest reimbursementRequest) {
        super(source);
        this.approvalFlow = approvalFlow;
        this.reimbursementRequest = reimbursementRequest;
        this.occurredOn = LocalDateTime.now();
    }

    public ApprovalFlow getApprovalFlow() {
        return approvalFlow;
    }

    public ReimbursementRequest getReimbursementRequest() {
        return reimbursementRequest;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String toString() {
        return "ReimbursementApprovalCompletedEvent{" +
                "reimbursementRequestId=" + reimbursementRequest.getId() +
                ", applicantId=" + (reimbursementRequest.getApplicant() != null ? reimbursementRequest.getApplicant().getId() : null) +
                ", title=" + reimbursementRequest.getTitle() +
                ", totalAmount=" + reimbursementRequest.getTotalAmount() +
                ", itemCount=" + (reimbursementRequest.getItems() != null ? reimbursementRequest.getItems().size() : 0) +
                '}';
    }
}
