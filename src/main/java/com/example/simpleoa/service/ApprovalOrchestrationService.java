package com.example.simpleoa.service;

import com.example.simpleoa.event.ApprovalCompletedEvent;
import com.example.simpleoa.event.ApprovalRejectedEvent;
import com.example.simpleoa.event.WorkTimeApprovalCompletedEvent;
import com.example.simpleoa.event.BusinessTripApprovalCompletedEvent;
import com.example.simpleoa.event.LeaveRequestApprovalCompletedEvent;
import com.example.simpleoa.event.ReimbursementApprovalCompletedEvent;
import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.ApprovalFlowRepository;
import com.example.simpleoa.repository.WorkTimeRecordRepository;
import com.example.simpleoa.repository.BusinessTripRequestRepository;
import com.example.simpleoa.repository.LeaveRequestRepository;
import com.example.simpleoa.repository.ReimbursementRequestRepository;
import com.example.simpleoa.service.strategy.ApprovalStrategy;
import com.example.simpleoa.service.strategy.ApprovalStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

/**
 * 审批编排服务
 *
 * <p>使用策略模式统一处理各种业务类型的审批
 * <p>消除原有 ApprovalFlowServiceImpl 中的 if-else 地狱
 * <p>通过领域事件解除循环依赖
 *
 * <p>支持新旧两种数据结构的过渡期：
 * - 新结构：entity_type + entity_id + approval_status + approval_stage
 * - 旧结构：work_time_record_id/leave_request_id/etc + request_type + status
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalOrchestrationService {

    private final ApprovalFlowRepository approvalFlowRepository;
    private final ApprovalStrategyFactory strategyFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final WorkTimeRecordRepository workTimeRecordRepository;
    private final BusinessTripRequestRepository businessTripRequestRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ReimbursementRequestRepository reimbursementRequestRepository;
    // 其他 repository 在需要时注入

    /**
     * 提交审批（使用新架构）
     *
     * @param entityType 业务实体类型
     * @param entityId   业务实体ID
     * @param approver   审批人
     * @param <T>        实体类型
     * @return 创建的审批流程
     */
    @Transactional
    public <T> ApprovalFlow submitApproval(EntityType entityType, Long entityId, User approver) {
        log.info("提交审批: entityType={}, entityId={}, approverId={}",
                entityType, entityId, approver != null ? approver.getId() : null);

        // 获取对应的策略
        ApprovalStrategy<Object> strategy = strategyFactory.getStrategy(entityType);

        // TODO: 加载实体并验证
        // T entity = loadEntity(entityType, entityId);
        // strategy.validateForSubmission(entity);

        // 创建审批流程
        ApprovalFlow flow = new ApprovalFlow();
        flow.setEntityType(entityType);
        flow.setEntityId(entityId);
        flow.setApprover(approver);
        flow.setUnifiedStatus(ApprovalStatus.PENDING);
        flow.setApprovalStage(ApprovalStage.INITIAL);
        flow.setCreateTime(new Date());

        // 保持向后兼容 - 同时设置旧字段
        flow.setRequestType(entityType.toRequestType());
        flow.setStatus("PENDING");

        return approvalFlowRepository.save(flow);
    }

    /**
     * 处理审批通过（使用新架构）
     *
     * @param flowId 审批流程ID
     * @param comment 审批意见
     * @return 更新后的审批流程
     */
    @Transactional
    public ApprovalFlow approveApproval(Long flowId, String comment) {
        log.info("处理审批通过: flowId={}", flowId);

        ApprovalFlow flow = approvalFlowRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("审批流程不存在: " + flowId));

        // 检查状态
        ApprovalStatus currentStatus = flow.getUnifiedStatus();
        if (currentStatus != null && !currentStatus.equals(ApprovalStatus.PENDING)) {
            throw new IllegalStateException("审批流程已处理，当前状态: " + currentStatus);
        }

        // 判断是新结构还是旧结构
        if (flow.getEntityType() != null) {
            return handleApprovalWithNewStructure(flow, comment, true);
        } else {
            return handleApprovalWithLegacyStructure(flow, comment, true);
        }
    }

    /**
     * 处理审批拒绝（使用新架构）
     *
     * @param flowId 审批流程ID
     * @param comment 拒绝理由
     * @return 更新后的审批流程
     */
    @Transactional
    public ApprovalFlow rejectApproval(Long flowId, String comment) {
        log.info("处理审批拒绝: flowId={}, comment={}", flowId, comment);

        ApprovalFlow flow = approvalFlowRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("审批流程不存在: " + flowId));

        // 检查状态
        ApprovalStatus currentStatus = flow.getUnifiedStatus();
        if (currentStatus != null && !currentStatus.equals(ApprovalStatus.PENDING)) {
            throw new IllegalStateException("审批流程已处理，当前状态: " + currentStatus);
        }

        // 判断是新结构还是旧结构
        if (flow.getEntityType() != null) {
            return handleApprovalWithNewStructure(flow, comment, false);
        } else {
            return handleApprovalWithLegacyStructure(flow, comment, false);
        }
    }

    /**
     * 使用新架构（策略模式）处理审批
     *
     * @param flow    审批流程
     * @param comment 审批意见
     * @param approved 是否批准
     * @return 更新后的审批流程
     */
    private ApprovalFlow handleApprovalWithNewStructure(ApprovalFlow flow, String comment, boolean approved) {
        EntityType entityType = flow.getEntityType();
        ApprovalStrategy<Object> strategy = strategyFactory.getStrategy(entityType);

        try {
            // 加载实体
            Object entity = loadEntity(entityType, flow.getEntityId());

            if (approved) {
                // 策略处理审批通过逻辑
                strategy.onApproved(entity, flow);

                // 更新审批流程状态
                flow.setUnifiedStatus(ApprovalStatus.APPROVED);

                // 发布审批完成事件
                eventPublisher.publishEvent(new ApprovalCompletedEvent(flow, entity));

                // 如果是工时审批，发布工时专用事件
                if (EntityType.WORK_TIME.equals(entityType)) {
                    eventPublisher.publishEvent(new WorkTimeApprovalCompletedEvent(this, flow, (WorkTimeRecord) entity));
                }

                // 如果是出差审批，发布出差专用事件
                if (EntityType.BUSINESS_TRIP.equals(entityType)) {
                    eventPublisher.publishEvent(new BusinessTripApprovalCompletedEvent(this, flow, (BusinessTripRequest) entity));
                }

                // 如果是请假审批，发布请假专用事件
                if (EntityType.LEAVE_REQUEST.equals(entityType)) {
                    eventPublisher.publishEvent(new LeaveRequestApprovalCompletedEvent(this, flow, (LeaveRequest) entity));
                }

                // 如果是报销审批，发布报销专用事件
                if (EntityType.REIMBURSEMENT.equals(entityType)) {
                    eventPublisher.publishEvent(new ReimbursementApprovalCompletedEvent(this, flow, (com.example.simpleoa.model.ReimbursementRequest) entity));
                }

                log.info("审批通过处理完成: entityType={}, entityId={}", entityType, flow.getEntityId());

            } else {
                // 策略处理审批拒绝逻辑
                strategy.onRejected(entity, flow, comment);

                // 更新审批流程状态
                flow.setUnifiedStatus(ApprovalStatus.REJECTED);

                // 发布审批拒绝事件
                eventPublisher.publishEvent(new ApprovalRejectedEvent(
                        flow,
                        entity,
                        comment
                ));

                log.info("审批拒绝处理完成: entityType={}, entityId={}", entityType, flow.getEntityId());
            }

            flow.setComment(comment);
            flow.setUpdateTime(new Date());
            flow.setApprovalTime(new Date());

            return approvalFlowRepository.save(flow);

        } catch (Exception e) {
            log.error("处理审批失败: entityType={}, entityId={}, approved={}",
                    entityType, flow.getEntityId(), approved, e);
            throw new RuntimeException("处理审批失败", e);
        }
    }

    /**
     * 使用旧结构处理审批（过渡期兼容）
     *
     * @param flow    审批流程
     * @param comment 审批意见
     * @param approved 是否批准
     * @return 更新后的审批流程
     */
    @SuppressWarnings("deprecation")
    private ApprovalFlow handleApprovalWithLegacyStructure(ApprovalFlow flow, String comment, boolean approved) {
        log.warn("使用旧结构处理审批（过渡期兼容）: requestType={}, workTimeRecordId={}",
                flow.getRequestType(),
                flow.getWorkTimeRecord() != null ? flow.getWorkTimeRecord().getId() : null);

        // 旧逻辑保持不变，直接委托给现有的 ApprovalFlowServiceImpl
        // 这里暂时保持原有行为，后续逐步迁移到新架构

        if (approved && "APPROVED".equals(flow.getStatus())) {
            // 旧结构中，审批通过后的状态由各个服务自行处理
            // 这里只更新审批流程本身的状态
        }

        flow.setComment(comment);
        flow.setUpdateTime(new Date());
        if (approved) {
            flow.setApprovalTime(new Date());
        }

        return approvalFlowRepository.save(flow);
    }

    /**
     * 加载实体
     *
     * @param entityType 实体类型
     * @param entityId   实体ID
     * @return 实体对象
     */
    private Object loadEntity(EntityType entityType, Long entityId) {
        log.debug("加载实体: entityType={}, entityId={}", entityType, entityId);

        switch (entityType) {
            case WORK_TIME:
                Optional<WorkTimeRecord> workTime = workTimeRecordRepository.findById(entityId);
                if (workTime.isEmpty()) {
                    throw new IllegalArgumentException("工时记录不存在: " + entityId);
                }
                return workTime.get();

            // 其他类型在后续 Phase 实现
            case LEAVE_REQUEST:
                Optional<LeaveRequest> leaveRequest = leaveRequestRepository.findById(entityId);
                if (leaveRequest.isEmpty()) {
                    throw new IllegalArgumentException("请假申请不存在: " + entityId);
                }
                return leaveRequest.get();

            case BUSINESS_TRIP:
                Optional<BusinessTripRequest> businessTrip = businessTripRequestRepository.findById(entityId);
                if (businessTrip.isEmpty()) {
                    throw new IllegalArgumentException("出差申请不存在: " + entityId);
                }
                return businessTrip.get();

            case REIMBURSEMENT:
                Optional<com.example.simpleoa.model.ReimbursementRequest> reimbursement = reimbursementRequestRepository.findById(entityId);
                if (reimbursement.isEmpty()) {
                    throw new IllegalArgumentException("报销申请不存在: " + entityId);
                }
                return reimbursement.get();

            default:
                throw new IllegalArgumentException("不支持的实体类型: " + entityType);
        }
    }
}
