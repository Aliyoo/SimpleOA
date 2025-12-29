package com.example.simpleoa.event.listener;

import com.example.simpleoa.event.ApprovalRejectedEvent;
import com.example.simpleoa.event.LeaveRequestApprovalCompletedEvent;
import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.LeaveRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 请假审批事件监听器
 *
 * <p>处理请假审批相关的事件：
 * - 最终审批通过事件
 * - 审批拒绝事件
 * <p>异步执行，不阻塞主流程
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Component
@Slf4j
public class LeaveRequestApprovalListener {

    /**
     * 处理请假审批完成事件
     *
     * <p>当请假审批最终通过时触发：
     * - 发送通知给申请人
     * - 更新项目统计（可选）
     * - 记录审计日志
     * - 触发工作交接相关逻辑（可选）
     * - 触发系统权限调整（可选）
     *
     * @param event 请假审批完成事件
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLeaveRequestApprovalCompleted(LeaveRequestApprovalCompletedEvent event) {
        log.info("收到请假审批完成事件: {}", event);

        LeaveRequest leaveRequest = event.getLeaveRequest();
        ApprovalFlow approvalFlow = event.getApprovalFlow();

        try {
            // TODO: 发送通知给用户
            sendNotificationToUser(leaveRequest, approvalFlow);

            // TODO: 更新项目统计
            // updateProjectStatistics(leaveRequest);

            // TODO: 记录审计日志
            logAuditRecord(leaveRequest, approvalFlow, "APPROVED");

            // TODO: 触发工作交接相关逻辑（可选）
            // triggerWorkHandover(leaveRequest);

            // TODO: 触发系统权限调整（可选，如长期休假）
            // adjustSystemPermissions(leaveRequest);

            log.info("请假审批完成事件处理成功: leaveRequestId={}", leaveRequest.getId());

        } catch (Exception e) {
            log.error("处理请假审批完成事件失败: leaveRequestId={}", leaveRequest.getId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 处理请假审批拒绝事件
     *
     * <p>当请假审批被拒绝时触发：
     * - 发送拒绝通知给用户
     * - 记录拒绝原因
     *
     * @param event 通用审批拒绝事件
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLeaveRequestApprovalRejected(ApprovalRejectedEvent event) {
        log.info("收到请假审批拒绝事件: entityType={}, entityId={}",
                event.getEntityType(), event.getEntityId());

        // 只处理请假类型的拒绝事件
        if (!com.example.simpleoa.model.EntityType.LEAVE_REQUEST.equals(event.getEntityType())) {
            return;
        }

        try {
            // TODO: 发送拒绝通知给用户
            sendRejectionNotification(event);

            // TODO: 记录审计日志
            logRejectionAuditRecord(event);

            log.info("请假审批拒绝事件处理成功: entityId={}", event.getEntityId());

        } catch (Exception e) {
            log.error("处理请假审批拒绝事件失败: entityId={}", event.getEntityId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 发送通知给用户
     *
     * @param leaveRequest  请假申请
     * @param approvalFlow  审批流程
     */
    private void sendNotificationToUser(LeaveRequest leaveRequest, ApprovalFlow approvalFlow) {
        if (leaveRequest.getApplicant() == null) {
            log.warn("请假申请没有关联申请人，无法发送通知: leaveRequestId={}", leaveRequest.getId());
            return;
        }

        log.info("发送请假审批通过通知: applicantId={}, leaveRequestId={}, leaveType={}, leaveDays={}, startDate={}, endDate={}",
                leaveRequest.getApplicant().getId(),
                leaveRequest.getId(),
                leaveRequest.getLeaveType(),
                leaveRequest.getLeaveDays(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate());

        // TODO: 实现实际的通知发送逻辑
        // - 邮件通知
        // - 系统内消息
        // - 短信通知（可选）
        // 通知内容应包括：
        //   - 请假类型（年假、病假、事假等）
        //   - 请假天数
        //   - 请假时间
        //   - 审批人
        //   - 审批时间
        //   - 请假余额变化（如果适用）
    }

    /**
     * 记录审计日志
     *
     * @param leaveRequest  请假申请
     * @param approvalFlow  审批流程
     * @param action        操作类型
     */
    private void logAuditRecord(LeaveRequest leaveRequest, ApprovalFlow approvalFlow, String action) {
        log.info("记录请假审批审计日志: leaveRequestId={}, applicantId={}, leaveType={}, leaveDays={}, action={}, approverId={}",
                leaveRequest.getId(),
                leaveRequest.getApplicant() != null ? leaveRequest.getApplicant().getId() : null,
                leaveRequest.getLeaveType(),
                leaveRequest.getLeaveDays(),
                action,
                approvalFlow.getApprover() != null ? approvalFlow.getApprover().getId() : null);

        // TODO: 实现实际的审计日志记录
        // - 保存到审计日志表
        // - 记录操作时间、操作人、操作结果
        // - 记录请假详细信息（类型、天数、时间等）
        // - 记录余额变化（如果适用）
    }

    /**
     * 发送拒绝通知给用户
     *
     * @param event 审批拒绝事件
     */
    private void sendRejectionNotification(ApprovalRejectedEvent event) {
        log.info("发送请假审批拒绝通知: entityType={}, entityId={}, reason={}",
                event.getEntityType(),
                event.getEntityId(),
                event.getReason());

        // TODO: 实现实际的拒绝通知发送逻辑
        // 通知内容应包括：
        //   - 拒绝原因
        //   - 审批人
        //   - 审批时间
        //   - 可修改重新申请的建议
    }

    /**
     * 记录拒绝审计日志
     *
     * @param event 审批拒绝事件
     */
    private void logRejectionAuditRecord(ApprovalRejectedEvent event) {
        log.info("记录请假审批拒绝审计日志: entityType={}, entityId={}, approverId={}, reason={}",
                event.getEntityType(),
                event.getEntityId(),
                event.getApproverId(),
                event.getReason());

        // TODO: 实现实际的拒绝审计日志记录
        // - 记录拒绝原因
        // - 记录拒绝时间
        // - 记录审批人信息
    }
}
