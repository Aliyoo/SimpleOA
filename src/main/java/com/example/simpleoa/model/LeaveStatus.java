package com.example.simpleoa.model;

public enum LeaveStatus {
    DRAFT,                      // 草稿
    PENDING_MANAGER_APPROVAL,   // 待部门经理审批
    PENDING_HR_APPROVAL,        // 待人事审批
    APPROVED,                   // 审批通过
    REJECTED,                   // 已驳回
    CANCELLED                   // 已撤销
}
