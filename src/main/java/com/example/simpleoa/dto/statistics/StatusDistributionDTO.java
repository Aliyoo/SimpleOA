package com.example.simpleoa.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 状态分布DTO
 * 用于展示报销申请的状态分布情况
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusDistributionDTO {
    
    /** 状态码 */
    private String status;
    
    /** 状态标签（中文显示名） */
    private String statusLabel;
    
    /** 数量 */
    private Integer count;
    
    /** 金额 */
    private BigDecimal amount;
    
    /** 占比 (0-100) */
    private Double percentage;
}
