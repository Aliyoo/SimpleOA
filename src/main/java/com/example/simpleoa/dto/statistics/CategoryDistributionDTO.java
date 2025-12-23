package com.example.simpleoa.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 类别分布DTO
 * 用于展示报销的费用类别分布情况
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDistributionDTO {
    
    /** 费用类别 */
    private String category;
    
    /** 金额 */
    private BigDecimal amount;
    
    /** 占比 (0-100) */
    private Double percentage;
}
