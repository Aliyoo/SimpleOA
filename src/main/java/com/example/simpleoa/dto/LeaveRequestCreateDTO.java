package com.example.simpleoa.dto;

import com.example.simpleoa.model.LeaveType;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class LeaveRequestCreateDTO {
    
    @NotNull(message = "请假类型不能为空")
    private LeaveType leaveType;
    
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startDate;
    
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endDate;
    
    private Double leaveDays;  // 请假天数（支持半天）
    
    @NotNull(message = "请假事由不能为空")
    private String reason;
    
    private String attachments;  // 附件路径，多个文件用逗号分隔
    
    private Long deputyId;  // 工作代理人ID
}
