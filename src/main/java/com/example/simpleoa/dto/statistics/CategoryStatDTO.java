package com.example.simpleoa.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 费用类别统计DTO
 * 用于按费用类别维度展示支出分布
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryStatDTO {
    
    /** 费用类别 */
    private String category;
    
    /** 总金额 */
    private BigDecimal totalAmount;
    
    /** 数量 */
    private Integer count;
    
    /** 占比 (0-100) */
    private Double percentage;
}
