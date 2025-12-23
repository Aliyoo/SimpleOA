package com.example.simpleoa.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 预算总览DTO
 * 用于展示全局预算汇总信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetOverviewDTO {
    
    /** 预算总额 */
    private BigDecimal totalBudgetAmount;
    
    /** 已使用金额 */
    private BigDecimal totalUsedAmount;
    
    /** 剩余金额 */
    private BigDecimal totalRemainingAmount;
    
    /** 整体使用率 (0-100) */
    private Double overallUsageRate;
    
    /** 活跃预算数量 */
    private Integer activeBudgetCount;
    
    /** 预警预算数量 */
    private Integer warningBudgetCount;
    
    /** 已关闭预算数量 */
    private Integer closedBudgetCount;
    
    /** 报销总金额 */
    private BigDecimal totalReimbursementAmount;
    
    /** 预算执行率 (0-100) */
    private Double budgetExecutionRate;
}
