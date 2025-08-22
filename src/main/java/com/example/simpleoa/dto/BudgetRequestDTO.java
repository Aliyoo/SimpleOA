package com.example.simpleoa.dto;

import lombok.Data;

import java.util.Date;

@Data
public class BudgetRequestDTO {
    private Long id;
    private String name;
    private String budgetType;
    private Long projectId;  // 前端发送的项目ID
    private Double totalAmount;
    private Double usedAmount;
    private Double remainingAmount;
    private Date startDate;
    private Date endDate;
    private String status;
    private String description;
    private Long createdBy;
    private Date createTime;
    private Date lastUpdateTime;
}
