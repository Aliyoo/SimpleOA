package com.example.simpleoa.model;

public enum ReimbursementStatus {
    DRAFT,                      // 草稿
    PENDING_MANAGER_APPROVAL,   // 待部门经理审批
    PENDING_FINANCE_APPROVAL,   // 待财务审批
    APPROVED,                   // 审批通过
    REJECTED                    // 已驳回
}
