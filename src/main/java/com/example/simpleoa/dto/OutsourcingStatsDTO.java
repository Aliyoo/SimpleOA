package com.example.simpleoa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutsourcingStatsDTO {
    private String company;
    private Double totalBudget;
    private Long projectCount;
}
