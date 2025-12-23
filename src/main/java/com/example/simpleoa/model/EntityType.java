package com.example.simpleoa.model;

/**
 * 实体类型枚举
 *
 * <p>用于ApprovalFlow的entity_type字段，实现多态设计
 * <p>统一管理所有需要审批的业务实体类型
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
public enum EntityType {
    /**
     * 工时记录
     */
    WORK_TIME("WorkTimeRecord", "工时记录"),

    /**
     * 请假申请
     */
    LEAVE_REQUEST("LeaveRequest", "请假申请"),

    /**
     * 出差申请
     */
    BUSINESS_TRIP("BusinessTripRequest", "出差申请"),

    /**
     * 报销申请
     */
    REIMBURSEMENT("ReimbursementRequest", "报销申请");

    private final String code;
    private final String description;

    EntityType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从旧的request_type字符串转换为EntityType
     *
     * @param requestType 旧的request_type值（如"WORKTIME", "LEAVE"等）
     * @return 对应的EntityType，如果无法识别则返回null
     */
    public static EntityType fromRequestType(String requestType) {
        if (requestType == null) {
            return null;
        }

        switch (requestType.toUpperCase()) {
            case "WORKTIME":
                return WORK_TIME;
            case "LEAVE":
                return LEAVE_REQUEST;
            case "BUSINESS_TRIP":
                return BUSINESS_TRIP;
            case "REIMBURSEMENT":
                return REIMBURSEMENT;
            default:
                return null;
        }
    }

    /**
     * 将EntityType转换为旧的request_type字符串（用于向后兼容）
     *
     * @return 旧的request_type值
     */
    public String toRequestType() {
        switch (this) {
            case WORK_TIME:
                return "WORKTIME";
            case LEAVE_REQUEST:
                return "LEAVE";
            case BUSINESS_TRIP:
                return "BUSINESS_TRIP";
            case REIMBURSEMENT:
                return "REIMBURSEMENT";
            default:
                return null;
        }
    }
}
