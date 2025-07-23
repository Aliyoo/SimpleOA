package com.example.simpleoa.dto;

import lombok.Data;
import java.util.Date;

@Data
public class LeaveRequestDTO {
    private Long id;
    private String leaveType;
    private Date startDate;
    private Date endDate;
    private String reason;
    private String status;
    private Long userId;
    private String comment;
}