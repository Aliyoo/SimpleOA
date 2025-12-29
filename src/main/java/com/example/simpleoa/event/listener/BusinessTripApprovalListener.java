package com.example.simpleoa.event.listener;

import com.example.simpleoa.event.ApprovalRejectedEvent;
import com.example.simpleoa.event.BusinessTripApprovalCompletedEvent;
import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.BusinessTripRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 出差审批事件监听器
 *
 * <p>处理出差审批相关的事件：
 * - 审批通过事件
 * - 审批拒绝事件
 * <p>异步执行，不阻塞主流程
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Component
@Slf4j
public class BusinessTripApprovalListener {

    /**
     * 处理出差审批完成事件
     *
     * <p>当出差审批通过时触发：
     * - 发送通知给申请人
     * - 更新项目统计（可选）
     * - 记录审计日志
     * - 触发行程安排相关逻辑（可选）
     *
     * @param event 出差审批完成事件
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBusinessTripApprovalCompleted(BusinessTripApprovalCompletedEvent event) {
        log.info("收到出差审批完成事件: {}", event);

        BusinessTripRequest businessTripRequest = event.getBusinessTripRequest();
        ApprovalFlow approvalFlow = event.getApprovalFlow();

        try {
            // TODO: 发送通知给用户
            sendNotificationToUser(businessTripRequest, approvalFlow);

            // TODO: 更新项目统计
            // updateProjectStatistics(businessTripRequest);

            // TODO: 记录审计日志
            logAuditRecord(businessTripRequest, approvalFlow, "APPROVED");

            // TODO: 触发行程安排相关逻辑（可选）
            // triggerTripPlanning(businessTripRequest);

            log.info("出差审批完成事件处理成功: businessTripId={}", businessTripRequest.getId());

        } catch (Exception e) {
            log.error("处理出差审批完成事件失败: businessTripId={}", businessTripRequest.getId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 处理出差审批拒绝事件
     *
     * <p>当出差审批被拒绝时触发：
     * - 发送拒绝通知给用户
     * - 记录拒绝原因
     *
     * @param event 通用审批拒绝事件
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBusinessTripApprovalRejected(ApprovalRejectedEvent event) {
        log.info("收到出差审批拒绝事件: entityType={}, entityId={}",
                event.getEntityType(), event.getEntityId());

        // 只处理出差类型的拒绝事件
        if (!com.example.simpleoa.model.EntityType.BUSINESS_TRIP.equals(event.getEntityType())) {
            return;
        }

        try {
            // TODO: 发送拒绝通知给用户
            sendRejectionNotification(event);

            // TODO: 记录审计日志
            logRejectionAuditRecord(event);

            log.info("出差审批拒绝事件处理成功: entityId={}", event.getEntityId());

        } catch (Exception e) {
            log.error("处理出差审批拒绝事件失败: entityId={}", event.getEntityId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 发送通知给用户
     *
     * @param businessTripRequest 出差申请
     * @param approvalFlow        审批流程
     */
    private void sendNotificationToUser(BusinessTripRequest businessTripRequest, ApprovalFlow approvalFlow) {
        if (businessTripRequest.getApplicant() == null) {
            log.warn("出差申请没有关联申请人，无法发送通知: businessTripId={}", businessTripRequest.getId());
            return;
        }

        log.info("发送出差审批通过通知: applicantId={}, businessTripId={}, destination={}, startTime={}, endTime={}",
                businessTripRequest.getApplicant().getId(),
                businessTripRequest.getId(),
                businessTripRequest.getDestination(),
                businessTripRequest.getStartTime(),
                businessTripRequest.getEndTime());

        // TODO: 实现实际的通知发送逻辑
        // - 邮件通知
        // - 系统内消息
        // - 短信通知（可选）
        // 通知内容应包括：
        //   - 出差地点
        //   - 出差时间
        //   - 出差天数
        //   - 审批人
        //   - 审批时间
    }

    /**
     * 记录审计日志
     *
     * @param businessTripRequest 出差申请
     * @param approvalFlow        审批流程
     * @param action              操作类型
     */
    private void logAuditRecord(BusinessTripRequest businessTripRequest, ApprovalFlow approvalFlow, String action) {
        log.info("记录出差审批审计日志: businessTripId={}, applicantId={}, action={}, approverId={}",
                businessTripRequest.getId(),
                businessTripRequest.getApplicant() != null ? businessTripRequest.getApplicant().getId() : null,
                action,
                approvalFlow.getApprover() != null ? approvalFlow.getApprover().getId() : null);

        // TODO: 实现实际的审计日志记录
        // - 保存到审计日志表
        // - 记录操作时间、操作人、操作结果
        // - 记录出差详细信息（地点、时间、天数等）
    }

    /**
     * 发送拒绝通知给用户
     *
     * @param event 审批拒绝事件
     */
    private void sendRejectionNotification(ApprovalRejectedEvent event) {
        log.info("发送出差审批拒绝通知: entityType={}, entityId={}, reason={}",
                event.getEntityType(),
                event.getEntityId(),
                event.getReason());

        // TODO: 实现实际的拒绝通知发送逻辑
        // 通知内容应包括：
        //   - 拒绝原因
        //   - 审批人
        //   - 审批时间
    }

    /**
     * 记录拒绝审计日志
     *
     * @param event 审批拒绝事件
     */
    private void logRejectionAuditRecord(ApprovalRejectedEvent event) {
        log.info("记录出差审批拒绝审计日志: entityType={}, entityId={}, approverId={}, reason={}",
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
