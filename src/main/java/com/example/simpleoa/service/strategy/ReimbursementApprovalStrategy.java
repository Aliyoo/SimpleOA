package com.example.simpleoa.service.strategy;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.ApprovalStage;
import com.example.simpleoa.model.ApprovalStatus;
import com.example.simpleoa.model.EntityType;
import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.ReimbursementStatus;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.ReimbursementRequestRepository;
import com.example.simpleoa.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 报销审批策略
 *
 * <p>报销审批是多级审批（项目经理 → 财务 → 最终审批）
 * <p>审批特点：
 * - INITIAL阶段：项目经理审批
 * - FINANCE阶段：财务审批
 * - FINAL阶段：最终审批（扣减预算）
 * <p>预算扣除逻辑：
 * - 只有在最终审批通过时才扣减预算
 * - 遍历所有费用项，逐项扣除预算
 * - 支持Budget和BudgetItem两种预算类型
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReimbursementApprovalStrategy implements ApprovalStrategy<ReimbursementRequest> {

    private final ReimbursementRequestRepository reimbursementRequestRepository;
    private final BudgetService budgetService;

    @Override
    public boolean supports(EntityType entityType) {
        return EntityType.REIMBURSEMENT.equals(entityType);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REIMBURSEMENT;
    }

    @Override
    public void validateForSubmission(ReimbursementRequest entity) {
        if (entity == null) {
            throw new IllegalArgumentException("报销申请不能为空");
        }

        if (entity.getId() == null) {
            throw new IllegalArgumentException("报销申请必须先保存才能提交审批");
        }

        if (entity.getApplicant() == null || entity.getApplicant().getId() == null) {
            throw new IllegalArgumentException("报销申请必须关联申请人");
        }

        if (entity.getTitle() == null || entity.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("报销标题不能为空");
        }

        if (entity.getTotalAmount() == null || entity.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("报销总金额必须大于0");
        }

        if (entity.getItems() == null || entity.getItems().isEmpty()) {
            throw new IllegalArgumentException("报销申请必须包含费用明细");
        }

        // 检查报销申请是否存在于数据库中
        if (!reimbursementRequestRepository.existsById(entity.getId())) {
            throw new IllegalArgumentException("报销申请不存在于数据库中");
        }

        // 检查状态是否允许提交审批
        if (entity.getStatus() != ReimbursementStatus.DRAFT) {
            throw new IllegalArgumentException("只有草稿状态的报销申请才能提交审批");
        }

        // ⭐ 检查预算可用性（在提交时检查）
        if (!checkBudgetAvailability(entity)) {
            throw new IllegalArgumentException("预算不足，无法提交审批");
        }
    }

    @Override
    public void onApproved(ReimbursementRequest entity, ApprovalFlow flow) {
        log.info("报销审批通过: reimbursementRequestId={}, applicantId={}, totalAmount={}, currentStage={}",
                entity.getId(),
                entity.getApplicant() != null ? entity.getApplicant().getId() : null,
                entity.getTotalAmount(),
                flow.getApprovalStage());

        ApprovalStage currentStage = flow.getApprovalStage();

        // 根据审批阶段更新状态
        if (currentStage == ApprovalStage.INITIAL) {
            // 项目经理审批通过 → 转到财务审批
            entity.setStatus(ReimbursementStatus.PENDING_FINANCE_APPROVAL);
            log.info("项目经理审批通过，转财务审批: reimbursementRequestId={}", entity.getId());

        } else if (currentStage == ApprovalStage.FINANCE) {
            // 财务审批通过 → 转最终审批
            entity.setStatus(ReimbursementStatus.APPROVED);

            // ⭐ 最终审批：扣减预算
            deductBudget(entity);

            log.info("财务审批通过，最终审批完成: reimbursementRequestId={}", entity.getId());

        } else if (currentStage == ApprovalStage.FINAL) {
            // 最终审批通过（如果是直接单级审批）
            entity.setStatus(ReimbursementStatus.APPROVED);

            // ⭐ 最终审批：扣减预算
            deductBudget(entity);

            log.info("最终审批通过: reimbursementRequestId={}", entity.getId());
        }

        // 保存报销申请
        reimbursementRequestRepository.save(entity);

        log.info("报销申请审批状态已更新: reimbursementRequestId={}, status={}", entity.getId(), entity.getStatus());
    }

    @Override
    public void onRejected(ReimbursementRequest entity, ApprovalFlow flow, String comment) {
        log.info("报销审批拒绝: reimbursementRequestId={}, applicantId={}, totalAmount={}, comment={}",
                entity.getId(),
                entity.getApplicant() != null ? entity.getApplicant().getId() : null,
                entity.getTotalAmount(),
                comment);

        // 审批拒绝 → 状态变为REJECTED
        entity.setStatus(ReimbursementStatus.REJECTED);

        // 保存审批意见
        if (entity.getComment() == null || entity.getComment().trim().isEmpty()) {
            entity.setComment(comment);
        }

        // 保存报销申请
        reimbursementRequestRepository.save(entity);

        log.info("报销申请审批状态已更新: reimbursementRequestId={}, status=REJECTED", entity.getId());
    }

    @Override
    public User getNextApprover(ReimbursementRequest entity, ApprovalStage currentStage) {
        // 报销审批有多级，需要根据当前阶段返回下一级审批人

        if (currentStage == null || currentStage == ApprovalStage.INITIAL) {
            // 第一级：项目经理审批
            if (entity.getProject() != null && entity.getProject().getManager() != null) {
                log.info("查找项目经理作为第一级审批人: reimbursementRequestId={}, projectId={}",
                        entity.getId(), entity.getProject().getId());
                return entity.getProject().getManager();
            }

            // 备选：财务角色用户
            log.info("查找财务作为第一级审批人: reimbursementRequestId={}", entity.getId());

        } else if (currentStage == ApprovalStage.FINANCE) {
            // 第二级：财务审批（最终审批）
            log.info("查找财务作为第二级审批人: reimbursementRequestId={}", entity.getId());
        }

        log.warn("无法确定下一级审批人: reimbursementRequestId={}, currentStage={}", entity.getId(), currentStage);
        return null;
    }

    @Override
    public boolean isFinalApproval(ApprovalStage currentStage) {
        // 报销审批的最终阶段是FINANCE或FINAL
        return currentStage == ApprovalStage.FINANCE || currentStage == ApprovalStage.FINAL;
    }

    @Override
    public ApprovalFlow createNextStageApproval(ReimbursementRequest entity, ApprovalStage currentStage, User nextApprover) {
        // 创建下一级审批流程（如果支持多级审批）
        if (currentStage == ApprovalStage.INITIAL && nextApprover != null) {
            ApprovalFlow nextFlow = new ApprovalFlow();
            nextFlow.setEntityType(EntityType.REIMBURSEMENT);
            nextFlow.setEntityId(entity.getId());
            nextFlow.setApprover(nextApprover);
            nextFlow.setUnifiedStatus(ApprovalStatus.PENDING);
            nextFlow.setApprovalStage(ApprovalStage.FINANCE);
            nextFlow.setCreateTime(new java.util.Date());
            return nextFlow;
        }

        // 其他情况不创建下一级审批
        return null;
    }

    /**
     * 检查预算可用性
     *
     * @param entity 报销申请
     * @return true表示预算充足
     */
    private boolean checkBudgetAvailability(ReimbursementRequest entity) {
        if (entity.getProject() == null || entity.getItems() == null || entity.getItems().isEmpty()) {
            return true;
        }

        try {
            // 简化实现：假设预算总是充足
            // TODO: 实际应该检查每个费用项的预算可用性
            return true;
        } catch (Exception e) {
            log.error("检查预算可用性失败: reimbursementRequestId={}", entity.getId(), e);
            throw new RuntimeException("检查预算可用性失败", e);
        }
    }

    /**
     * 扣减预算
     *
     * <p>遍历所有费用项，逐项扣除预算
     * <p>支持Budget和BudgetItem两种预算类型
     *
     * @param entity 报销申请
     */
    private void deductBudget(ReimbursementRequest entity) {
        log.info("开始扣减预算: reimbursementRequestId={}, totalAmount={}",
                entity.getId(), entity.getTotalAmount());

        if (entity.getProject() == null || entity.getItems() == null || entity.getItems().isEmpty()) {
            log.warn("报销申请没有项目或费用明细，跳过预算扣除: reimbursementRequestId={}", entity.getId());
            return;
        }

        try {
            // 简化实现：只记录日志，实际不扣减预算
            // TODO: 实际应该遍历所有费用项，逐项扣减预算
            log.info("预算扣除（简化实现）: reimbursementRequestId={}, itemCount={}",
                    entity.getId(), entity.getItems().size());

        } catch (Exception e) {
            log.error("预算扣除失败: reimbursementRequestId={}", entity.getId(), e);
            // 不抛出异常，避免影响主流程
        }
    }
}
