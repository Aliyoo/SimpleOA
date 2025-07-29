package com.example.simpleoa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReimbursementItemDTO {
    private Long id;
    private LocalDate expenseDate;
    private String itemCategory;
    private String description;
    private BigDecimal amount;
    private Long budgetId;
    private Long budgetItemId;
}
