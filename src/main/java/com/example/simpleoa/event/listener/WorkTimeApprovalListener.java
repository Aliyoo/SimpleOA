package com.example.simpleoa.event.listener;

import com.example.simpleoa.event.ApprovalCompletedEvent;
import com.example.simpleoa.event.ApprovalRejectedEvent;
import com.example.simpleoa.event.WorkTimeApprovalCompletedEvent;
import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.WorkTimeRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工时审批事件监听器
 *
 * <p>处理工时审批相关的事件：
 * - 审批通过事件
 * - 审批拒绝事件
 * <p>异步执行，不阻塞主流程
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Component
@Slf4j
public class WorkTimeApprovalListener {

    /**
     * 处理工时审批完成事件
     *
     * <p>当工时审批通过时触发：
     * - 发送通知给用户
     * - 更新项目统计（可选）
     * - 记录审计日志
     *
     * @param event 工时审批完成事件
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleWorkTimeApprovalCompleted(WorkTimeApprovalCompletedEvent event) {
        log.info("收到工时审批完成事件: {}", event);

        WorkTimeRecord workTimeRecord = event.getWorkTimeRecord();
        ApprovalFlow approvalFlow = event.getApprovalFlow();

        try {
            // TODO: 发送通知给用户
            sendNotificationToUser(workTimeRecord, approvalFlow);

            // TODO: 更新项目统计
            // updateProjectStatistics(workTimeRecord);

            // TODO: 记录审计日志
            logAuditRecord(workTimeRecord, approvalFlow, "APPROVED");

            log.info("工时审批完成事件处理成功: workTimeId={}", workTimeRecord.getId());

        } catch (Exception e) {
            log.error("处理工时审批完成事件失败: workTimeId={}", workTimeRecord.getId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 处理工时审批拒绝事件
     *
     * <p>当工时审批被拒绝时触发：
     * - 发送拒绝通知给用户
     * - 记录拒绝原因
     *
     * @param event 通用审批拒绝事件
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleWorkTimeApprovalRejected(ApprovalRejectedEvent event) {
        log.info("收到工时审批拒绝事件: entityType={}, entityId={}",
                event.getEntityType(), event.getEntityId());

        // 只处理工时类型的拒绝事件
        if (!com.example.simpleoa.model.EntityType.WORK_TIME.equals(event.getEntityType())) {
            return;
        }

        try {
            // TODO: 发送拒绝通知给用户
            sendRejectionNotification(event);

            // TODO: 记录审计日志
            logRejectionAuditRecord(event);

            log.info("工时审批拒绝事件处理成功: entityId={}", event.getEntityId());

        } catch (Exception e) {
            log.error("处理工时审批拒绝事件失败: entityId={}", event.getEntityId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 发送通知给用户
     *
     * @param workTimeRecord 工时记录
     * @param approvalFlow   审批流程
     */
    private void sendNotificationToUser(WorkTimeRecord workTimeRecord, ApprovalFlow approvalFlow) {
        if (workTimeRecord.getUser() == null) {
            log.warn("工时记录没有关联用户，无法发送通知: workTimeId={}", workTimeRecord.getId());
            return;
        }

        log.info("发送工时审批通过通知: userId={}, workTimeId={}, hours={}",
                workTimeRecord.getUser().getId(),
                workTimeRecord.getId(),
                workTimeRecord.getHours());

        // TODO: 实现实际的通知发送逻辑
        // - 邮件通知
        // - 系统内消息
        // - 短信通知（可选）
    }

    /**
     * 记录审计日志
     *
     * @param workTimeRecord 工时记录
     * @param approvalFlow   审批流程
     * @param action         操作类型
     */
    private void logAuditRecord(WorkTimeRecord workTimeRecord, ApprovalFlow approvalFlow, String action) {
        log.info("记录工时审批审计日志: workTimeId={}, userId={}, action={}, approverId={}",
                workTimeRecord.getId(),
                workTimeRecord.getUser() != null ? workTimeRecord.getUser().getId() : null,
                action,
                approvalFlow.getApprover() != null ? approvalFlow.getApprover().getId() : null);

        // TODO: 实现实际的审计日志记录
        // - 保存到审计日志表
        // - 记录操作时间、操作人、操作结果
    }

    /**
     * 发送拒绝通知给用户
     *
     * @param event 审批拒绝事件
     */
    private void sendRejectionNotification(ApprovalRejectedEvent event) {
        log.info("发送工时审批拒绝通知: entityType={}, entityId={}, reason={}",
                event.getEntityType(),
                event.getEntityId(),
                event.getReason()); // 修正方法名

        // TODO: 实现实际的拒绝通知发送逻辑
    }

    /**
     * 记录拒绝审计日志
     *
     * @param event 审批拒绝事件
     */
    private void logRejectionAuditRecord(ApprovalRejectedEvent event) {
        log.info("记录工时审批拒绝审计日志: entityType={}, entityId={}, approverId={}, reason={}",
                event.getEntityType(),
                event.getEntityId(),
                event.getApproverId(),
                event.getReason()); // 修正方法名

        // TODO: 实现实际的拒绝审计日志记录
    }
}
