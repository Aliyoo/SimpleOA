package com.example.simpleoa.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 时间序列统计DTO
 * 用于按时间维度展示预算使用趋势
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSeriesStatDTO {
    
    /** 时间周期，如 "2025-01", "2025-Q1", "2025" */
    private String period;
    
    /** 预算金额 */
    private BigDecimal budgetAmount;
    
    /** 已使用金额 */
    private BigDecimal usedAmount;
    
    /** 报销金额 */
    private BigDecimal reimbursementAmount;
    
    /** 使用率 (0-100) */
    private Double usageRate;
}
