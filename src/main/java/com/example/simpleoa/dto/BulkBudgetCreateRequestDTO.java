package com.example.simpleoa.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BulkBudgetCreateRequestDTO {
    private Long projectId;
    private Date startDate;
    private Date endDate;
    private List<BulkBudgetItemDTO> items;
}
