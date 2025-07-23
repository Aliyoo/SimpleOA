package com.example.simpleoa.dto;

import lombok.Data;

import java.util.Date;

@Data
public class BudgetExpenseRequestDTO {
    private Long id;
    private Long budgetId;  // 预算ID
    private Long budgetItemId;  // 预算项目ID（可选）
    private Double amount;
    private Date expenseDate;
    private String expenseType;
    private String referenceNumber;
    private String status;
    private String description;
    private Long recordedBy;  // 记录人ID
    private Date recordTime;
    private Date createTime;
    private Date lastUpdateTime;
}
