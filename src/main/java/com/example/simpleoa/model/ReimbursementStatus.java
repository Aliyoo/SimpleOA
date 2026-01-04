package com.example.simpleoa.model;

public enum ReimbursementStatus {
    DRAFT,                      // 草稿
    PENDING_MANAGER_APPROVAL,   // 待项目经理审批（第一阶段）
    PENDING_LEADER_APPROVAL,    // 待领导审批（第二阶段，软件/硬件侧领导）
    PENDING_FINANCE_REVIEW,     // 待财务审查
    APPROVED,                   // 审批通过
    REJECTED                    // 已驳回
}
