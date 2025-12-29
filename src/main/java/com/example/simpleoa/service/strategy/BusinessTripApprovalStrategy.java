package com.example.simpleoa.service.strategy;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.ApprovalStage;
import com.example.simpleoa.model.ApprovalStatus;
import com.example.simpleoa.model.BusinessTripRequest;
import com.example.simpleoa.model.BusinessTripStatus;
import com.example.simpleoa.model.EntityType;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.BusinessTripRequestRepository;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 出差审批策略
 *
 * <p>出差审批是一级审批（项目经理审批）
 * <p>审批通过后更新出差申请状态为 APPROVED
 * <p>审批拒绝后更新出差申请状态为 REJECTED
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessTripApprovalStrategy implements ApprovalStrategy<BusinessTripRequest> {

    private final BusinessTripRequestRepository businessTripRequestRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public boolean supports(EntityType entityType) {
        return EntityType.BUSINESS_TRIP.equals(entityType);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.BUSINESS_TRIP;
    }

    @Override
    public void validateForSubmission(BusinessTripRequest entity) {
        if (entity == null) {
            throw new IllegalArgumentException("出差申请不能为空");
        }

        if (entity.getId() == null) {
            throw new IllegalArgumentException("出差申请必须先保存才能提交审批");
        }

        if (entity.getApplicant() == null || entity.getApplicant().getId() == null) {
            throw new IllegalArgumentException("出差申请必须关联申请人");
        }

        if (entity.getProject() == null || entity.getProject().getId() == null) {
            throw new IllegalArgumentException("出差申请必须关联项目");
        }

        if (entity.getDestination() == null || entity.getDestination().trim().isEmpty()) {
            throw new IllegalArgumentException("出差地点不能为空");
        }

        if (entity.getStartTime() == null || entity.getEndTime() == null) {
            throw new IllegalArgumentException("出差时间不能为空");
        }

        if (entity.getStartTime().isAfter(entity.getEndTime())) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }

        if (entity.getPurpose() == null || entity.getPurpose().trim().isEmpty()) {
            throw new IllegalArgumentException("出差事由不能为空");
        }

        // 检查出差申请是否存在于数据库中
        if (!businessTripRequestRepository.existsById(entity.getId())) {
            throw new IllegalArgumentException("出差申请不存在于数据库中");
        }

        // 检查项目是否有项目经理
        if (entity.getProject().getManager() == null) {
            throw new IllegalArgumentException("项目必须指定项目经理作为审批人");
        }
    }

    @Override
    public void onApproved(BusinessTripRequest entity, ApprovalFlow flow) {
        log.info("出差审批通过: businessTripId={}, applicantId={}, projectId={}",
                entity.getId(),
                entity.getApplicant() != null ? entity.getApplicant().getId() : null,
                entity.getProject() != null ? entity.getProject().getId() : null);

        // 更新状态为 APPROVED
        entity.setStatus(BusinessTripStatus.APPROVED);

        // 保存出差申请
        businessTripRequestRepository.save(entity);

        log.info("出差申请审批状态已更新: businessTripId={}, status=APPROVED", entity.getId());
    }

    @Override
    public void onRejected(BusinessTripRequest entity, ApprovalFlow flow, String comment) {
        log.info("出差审批拒绝: businessTripId={}, applicantId={}, projectId={}, comment={}",
                entity.getId(),
                entity.getApplicant() != null ? entity.getApplicant().getId() : null,
                entity.getProject() != null ? entity.getProject().getId() : null,
                comment);

        // 更新状态为 REJECTED
        entity.setStatus(BusinessTripStatus.REJECTED);

        // 保存审批意见（如果提供了comment字段）
        if (entity.getComment() == null || entity.getComment().trim().isEmpty()) {
            entity.setComment(comment);
        }

        // 保存出差申请
        businessTripRequestRepository.save(entity);

        log.info("出差申请审批状态已更新: businessTripId={}, status=REJECTED", entity.getId());
    }

    @Override
    public User getNextApprover(BusinessTripRequest entity, ApprovalStage currentStage) {
        // 出差审批只有一级，审批人为项目经理
        if (entity.getProject() != null && entity.getProject().getManager() != null) {
            return entity.getProject().getManager();
        }

        log.warn("出差申请没有关联的项目经理，无法确定审批人: businessTripId={}", entity.getId());
        return null;
    }

    @Override
    public boolean isFinalApproval(ApprovalStage currentStage) {
        // 出差审批只有一级，所以任何非null的stage都是最终审批
        return currentStage != null;
    }

    @Override
    public ApprovalFlow createNextStageApproval(BusinessTripRequest entity, ApprovalStage currentStage, User nextApprover) {
        // 出差审批没有多级审批，直接返回null
        return null;
    }
}
