package com.example.simpleoa.service.strategy;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.ApprovalStage;
import com.example.simpleoa.model.ApprovalStatus;
import com.example.simpleoa.model.EntityType;
import com.example.simpleoa.model.User;
import com.example.simpleoa.model.WorkTimeRecord;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.repository.WorkTimeRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工时审批策略
 *
 * <p>工时审批是最简单的审批类型，只有一级审批（项目经理审批）
 * <p>审批通过后设置工时记录的 approved 标志为 true
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkTimeApprovalStrategy implements ApprovalStrategy<WorkTimeRecord> {

    private final WorkTimeRecordRepository workTimeRecordRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public boolean supports(EntityType entityType) {
        return EntityType.WORK_TIME.equals(entityType);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WORK_TIME;
    }

    @Override
    public void validateForSubmission(WorkTimeRecord entity) {
        if (entity == null) {
            throw new IllegalArgumentException("工时记录不能为空");
        }

        if (entity.getId() == null) {
            throw new IllegalArgumentException("工时记录必须先保存才能提交审批");
        }

        if (entity.getUser() == null || entity.getUser().getId() == null) {
            throw new IllegalArgumentException("工时记录必须关联用户");
        }

        if (entity.getProject() == null || entity.getProject().getId() == null) {
            throw new IllegalArgumentException("工时记录必须关联项目");
        }

        if (entity.getDate() == null) {
            throw new IllegalArgumentException("工时记录必须包含日期");
        }

        if (entity.getHours() == null || entity.getHours() <= 0) {
            throw new IllegalArgumentException("工时记录必须包含有效的工作时长");
        }

        // 检查工时记录是否存在于数据库中
        if (!workTimeRecordRepository.existsById(entity.getId())) {
            throw new IllegalArgumentException("工时记录不存在于数据库中");
        }
    }

    @Override
    public void onApproved(WorkTimeRecord entity, ApprovalFlow flow) {
        log.info("工时审批通过: workTimeId={}, userId={}, projectId={}",
                entity.getId(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getProject() != null ? entity.getProject().getId() : null);

        // 设置 approved 标志为 true
        entity.setApproved(true);
        entity.setStatus("APPROVED");

        // 保存工时记录
        workTimeRecordRepository.save(entity);

        log.info("工时记录审批状态已更新: workTimeId={}, approved=true", entity.getId());
    }

    @Override
    public void onRejected(WorkTimeRecord entity, ApprovalFlow flow, String comment) {
        log.info("工时审批拒绝: workTimeId={}, userId={}, projectId={}, comment={}",
                entity.getId(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getProject() != null ? entity.getProject().getId() : null,
                comment);

        // 保持 approved 为 false，更新状态
        entity.setApproved(false);
        entity.setStatus("REJECTED");

        // 保存工时记录
        workTimeRecordRepository.save(entity);

        log.info("工时记录审批状态已更新: workTimeId={}, approved=false, status=REJECTED",
                entity.getId());
    }

    @Override
    public User getNextApprover(WorkTimeRecord entity, ApprovalStage currentStage) {
        // 工时审批只有一级，不需要下一级审批人
        // 如果需要项目经理作为审批人，可以在这里实现
        if (entity.getProject() != null && entity.getProject().getManager() != null) {
            return entity.getProject().getManager();
        }

        log.warn("工时记录没有关联的项目经理，无法确定审批人: workTimeId={}", entity.getId());
        return null;
    }

    @Override
    public boolean isFinalApproval(ApprovalStage currentStage) {
        // 工时审批只有一级，所以任何非null的stage都是最终审批
        return currentStage != null;
    }

    @Override
    public ApprovalFlow createNextStageApproval(WorkTimeRecord entity, ApprovalStage currentStage, User nextApprover) {
        // 工时审批没有多级审批，直接返回null
        return null;
    }
}
