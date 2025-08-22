package com.example.simpleoa.dto;

import lombok.Data;

@Data
public class BulkBudgetItemDTO {
    private String budgetType;
    private Double totalAmount;
    private String description;
}
