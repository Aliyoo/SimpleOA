package com.example.simpleoa.converter;

import com.example.simpleoa.model.LeaveStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LeaveStatusConverter implements AttributeConverter<LeaveStatus, String> {

    @Override
    public String convertToDatabaseColumn(LeaveStatus leaveStatus) {
        if (leaveStatus == null) {
            return null;
        }
        return leaveStatus.name();
    }

    @Override
    public LeaveStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        
        try {
            return LeaveStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // 处理中文状态值（向后兼容）
            switch (dbData) {
                case "待审批":
                case "pending":
                    return LeaveStatus.PENDING_MANAGER_APPROVAL;
                case "已审批":
                case "approved":
                    return LeaveStatus.APPROVED;
                case "已驳回":
                case "rejected":
                    return LeaveStatus.REJECTED;
                case "已撤销":
                case "cancelled":
                    return LeaveStatus.CANCELLED;
                case "草稿":
                case "draft":
                    return LeaveStatus.DRAFT;
                case "待人事审批":
                    return LeaveStatus.PENDING_HR_APPROVAL;
                default:
                    System.err.println("无法识别的请假状态: " + dbData + "，默认使用PENDING_MANAGER_APPROVAL");
                    return LeaveStatus.PENDING_MANAGER_APPROVAL;
            }
        }
    }
}