package com.example.simpleoa.converter;

import com.example.simpleoa.model.LeaveType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LeaveTypeConverter implements AttributeConverter<LeaveType, String> {

    @Override
    public String convertToDatabaseColumn(LeaveType leaveType) {
        if (leaveType == null) {
            return null;
        }
        return leaveType.name();
    }

    @Override
    public LeaveType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        
        try {
            return LeaveType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // 处理旧的枚举值（向后兼容）
            switch (dbData.toLowerCase()) {
                case "annual":
                    return LeaveType.ANNUAL_LEAVE;
                case "sick":
                    return LeaveType.SICK_LEAVE;
                case "personal":
                    return LeaveType.PERSONAL_LEAVE;
                case "marriage":
                    return LeaveType.MARRIAGE_LEAVE;
                case "maternity":
                    return LeaveType.MATERNITY_LEAVE;
                case "paternity":
                    return LeaveType.PATERNITY_LEAVE;
                case "bereavement":
                    return LeaveType.BEREAVEMENT_LEAVE;
                case "other":
                    return LeaveType.OTHER;
                default:
                    System.err.println("无法识别的请假类型: " + dbData + "，默认使用OTHER");
                    return LeaveType.OTHER;
            }
        }
    }
}
