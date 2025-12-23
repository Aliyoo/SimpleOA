package com.example.simpleoa.model;

/**
 * 审批状态统一枚举
 *
 * <p>替换原有的String类型状态字段，提供类型安全的审批状态管理</p>
 * <p>支持所有审批模块的统一状态管理</p>
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
public enum ApprovalStatus {
    /**
     * 待审批 - 初始状态，等待审批人处理
     */
    PENDING("待审批"),

    /**
     * 已批准 - 审批通过
     */
    APPROVED("已批准"),

    /**
     * 已拒绝 - 审批被拒绝
     */
    REJECTED("已拒绝"),

    /**
     * 已撤销 - 申请人主动撤销申请
     */
    CANCELLED("已撤销"),

    /**
     * 已撤回 - 审批人撤回之前的审批决定
     */
    REVOKED("已撤回");

    private final String description;

    ApprovalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 判断是否为终态（不能再改变的状态）
     *
     * @return true如果状态为终态
     */
    public boolean isFinalState() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }

    /**
     * 判断是否可以转换为指定状态
     *
     * @param newStatus 新状态
     * @return true如果可以转换
     */
    public boolean canTransitionTo(ApprovalStatus newStatus) {
        if (this.isFinalState()) {
            return false;
        }

        // PENDING可以转换为任何状态
        if (this == PENDING) {
            return true;
        }

        // 其他状态只能转换为终态
        return newStatus.isFinalState();
    }
}
