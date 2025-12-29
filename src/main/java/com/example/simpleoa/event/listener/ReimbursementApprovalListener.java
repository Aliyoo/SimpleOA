package com.example.simpleoa.event.listener;

import com.example.simpleoa.event.ApprovalRejectedEvent;
import com.example.simpleoa.event.ReimbursementApprovalCompletedEvent;
import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.ReimbursementRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报销审批事件监听器
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Component
@Slf4j
public class ReimbursementApprovalListener {

    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReimbursementApprovalCompleted(ReimbursementApprovalCompletedEvent event) {
        log.info("收到报销审批完成事件: {}", event);

        ReimbursementRequest reimbursementRequest = event.getReimbursementRequest();
        ApprovalFlow approvalFlow = event.getApprovalFlow();

        try {
            sendNotificationToUser(reimbursementRequest, approvalFlow);
            logAuditRecord(reimbursementRequest, approvalFlow, "APPROVED");
            log.info("报销审批完成事件处理成功: reimbursementRequestId={}", reimbursementRequest.getId());
        } catch (Exception e) {
            log.error("处理报销审批完成事件失败: reimbursementRequestId={}", reimbursementRequest.getId(), e);
        }
    }

    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReimbursementApprovalRejected(ApprovalRejectedEvent event) {
        log.info("收到报销审批拒绝事件: entityType={}, entityId={}",
                event.getEntityType(), event.getEntityId());

        if (!com.example.simpleoa.model.EntityType.REIMBURSEMENT.equals(event.getEntityType())) {
            return;
        }

        try {
            sendRejectionNotification(event);
            logRejectionAuditRecord(event);
            log.info("报销审批拒绝事件处理成功: entityId={}", event.getEntityId());
        } catch (Exception e) {
            log.error("处理报销审批拒绝事件失败: entityId={}", event.getEntityId(), e);
        }
    }

    private void sendNotificationToUser(ReimbursementRequest reimbursementRequest, ApprovalFlow approvalFlow) {
        if (reimbursementRequest.getApplicant() == null) {
            log.warn("报销申请没有关联申请人，无法发送通知: reimbursementRequestId={}", reimbursementRequest.getId());
            return;
        }

        log.info("发送报销审批通过通知: applicantId={}, reimbursementRequestId={}, totalAmount={}",
                reimbursementRequest.getApplicant().getId(),
                reimbursementRequest.getId(),
                reimbursementRequest.getTotalAmount());

        // TODO: 实现实际的通知发送逻辑
    }

    private void logAuditRecord(ReimbursementRequest reimbursementRequest, ApprovalFlow approvalFlow, String action) {
        log.info("记录报销审批审计日志: reimbursementRequestId={}, applicantId={}, totalAmount={}, action={}, approverId={}",
                reimbursementRequest.getId(),
                reimbursementRequest.getApplicant() != null ? reimbursementRequest.getApplicant().getId() : null,
                reimbursementRequest.getTotalAmount(),
                action,
                approvalFlow.getApprover() != null ? approvalFlow.getApprover().getId() : null);

        // TODO: 实现实际的审计日志记录
    }

    private void sendRejectionNotification(ApprovalRejectedEvent event) {
        log.info("发送报销审批拒绝通知: entityType={}, entityId={}, reason={}",
                event.getEntityType(),
                event.getEntityId(),
                event.getReason());

        // TODO: 实现实际的拒绝通知发送逻辑
    }

    private void logRejectionAuditRecord(ApprovalRejectedEvent event) {
        log.info("记录报销审批拒绝审计日志: entityType={}, entityId={}, approverId={}, reason={}",
                event.getEntityType(),
                event.getEntityId(),
                event.getApproverId(),
                event.getReason());

        // TODO: 实现实际的拒绝审计日志记录
    }
}
