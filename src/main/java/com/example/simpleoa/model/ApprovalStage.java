package com.example.simpleoa.model;

/**
 * 审批阶段枚举
 *
 * <p>支持多级审批流程，标识当前处于哪个审批阶段
 * <p>例如：请假审批可能需要经过 MANAGER → HR → FINAL 三个阶段
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
public enum ApprovalStage {
    /**
     * 初始审批 - 第一级审批
     */
    INITIAL("初始审批", 1),

    /**
     * 部门经理审批
     */
    MANAGER("部门经理审批", 2),

    /**
     * HR审批
     */
    HR("HR审批", 3),

    /**
     * 财务审批
     */
    FINANCE("财务审批", 4),

    /**
     * 最终审批 - 最后一级审批
     */
    FINAL("最终审批", 99);

    private final String description;
    private final Integer level;

    ApprovalStage(String description, Integer level) {
        this.description = description;
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public Integer getLevel() {
        return level;
    }

    /**
     * 获取下一个审批阶段
     *
     * @return 下一个阶段，如果已经是最后阶段则返回null
     */
    public ApprovalStage nextStage() {
        switch (this) {
            case INITIAL:
                return MANAGER;
            case MANAGER:
                return HR;
            case HR:
                return FINANCE;
            case FINANCE:
                return FINAL;
            case FINAL:
                return null; // 已经是最后阶段
            default:
                return null;
        }
    }

    /**
     * 判断是否为最终阶段
     *
     * @return true如果是最终审批阶段
     */
    public boolean isFinalStage() {
        return this == FINAL;
    }
}
