package com.example.simpleoa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTripRequestDTO {
    private Long id;
    
    private String destination;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endTime;
    
    // 保留原有字段用于兼容性
    private Date startDate;
    private Date endDate;
    
    @JsonProperty("reason")
    private String purpose; // 前端使用reason，后端使用purpose
    
    private Integer days; // 出差天数
    
    private String status;
    private String comment;
    
    private Long userId; // 保留原有字段
    private Long applicantId; // 申请人ID
    
    private Long projectId; // 项目ID
}