package com.example.simpleoa.service.strategy;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.ApprovalStage;
import com.example.simpleoa.model.ApprovalStatus;
import com.example.simpleoa.model.EntityType;
import com.example.simpleoa.model.User;

/**
 * 审批策略接口
 *
 * <p>使用策略模式消除审批流程中的if-else地狱
 * <p>每种业务类型实现自己的审批策略，独立管理审批逻辑
 *
 * @param <T> 审批实体的类型（如WorkTimeRecord, LeaveRequest等）
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
public interface ApprovalStrategy<T> {

    /**
     * 判断是否支持指定的实体类型
     *
     * @param entityType 实体类型
     * @return true如果支持该类型
     */
    boolean supports(EntityType entityType);

    /**
     * 获取支持的实体类型
     *
     * @return 实体类型枚举
     */
    EntityType getEntityType();

    /**
     * 验证是否可以提交审批
     *
     * @param entity 待审批的实体
     * @throws IllegalArgumentException 如果验证失败
     */
    void validateForSubmission(T entity);

    /**
     * 处理审批通过逻辑
     *
     * @param entity 审批实体
     * @param flow 审批流程
     * @throws IllegalStateException 如果状态不允许审批通过
     */
    void onApproved(T entity, ApprovalFlow flow);

    /**
     * 处理审批拒绝逻辑
     *
     * @param entity 审批实体
     * @param flow 审批流程
     * @param comment 拒绝理由
     */
    void onRejected(T entity, ApprovalFlow flow, String comment);

    /**
     * 获取下一级审批人（支持多级审批）
     *
     * @param entity 审批实体
     * @param currentStage 当前审批阶段，null表示初始审批
     * @return 下一级审批人，如果不需要下一级审批则返回null
     */
    User getNextApprover(T entity, ApprovalStage currentStage);

    /**
     * 判断是否为最终审批（最后一级）
     *
     * @param currentStage 当前审批阶段
     * @return true如果是最后一级审批
     */
    boolean isFinalApproval(ApprovalStage currentStage);

    /**
     * 创建下一级审批流程
     *
     * @param entity 审批实体
     * @param currentStage 当前阶段
     * @param nextApprover 下一级审批人
     * @return 创建的下一级审批流程，如果不需要则返回null
     */
    default ApprovalFlow createNextStageApproval(T entity, ApprovalStage currentStage, User nextApprover) {
        if (nextApprover == null || isFinalApproval(currentStage)) {
            return null;
        }

        ApprovalFlow nextFlow = new ApprovalFlow();
        nextFlow.setEntityType(getEntityType());
        nextFlow.setEntityId(extractEntityId(entity));
        nextFlow.setApprover(nextApprover);
        nextFlow.setUnifiedStatus(com.example.simpleoa.model.ApprovalStatus.PENDING);
        nextFlow.setApprovalStage(currentStage.nextStage());
        return nextFlow;
    }

    /**
     * 从实体中提取ID
     *
     * @param entity 实体对象
     * @return 实体ID
     * @throws UnsupportedOperationException 如果实体不支持getId方法
     */
    default Long extractEntityId(T entity) {
        try {
            java.lang.reflect.Method getId = entity.getClass().getMethod("getId");
            return (Long) getId.invoke(entity);
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                "Entity " + entity.getClass().getSimpleName() + " does not support getId()", e
            );
        }
    }
}
