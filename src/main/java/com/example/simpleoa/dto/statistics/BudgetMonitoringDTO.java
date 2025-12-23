package com.example.simpleoa.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 预算监控DTO
 * 用于展示预算执行情况和预警状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetMonitoringDTO {
    
    /** 预算ID */
    private Long budgetId;
    
    /** 预算名称 */
    private String budgetName;
    
    /** 项目名称 */
    private String projectName;
    
    /** 预算类型 */
    private String budgetType;
    
    /** 预算总额 */
    private BigDecimal totalAmount;
    
    /** 已使用金额 */
    private BigDecimal usedAmount;
    
    /** 剩余金额 */
    private BigDecimal remainingAmount;
    
    /** 使用率 (0-100) */
    private Double usageRate;
    
    /** 预警级别: NORMAL, WARNING, DANGER */
    private String alertLevel;
    
    /** 开始日期 */
    private Date startDate;
    
    /** 结束日期 */
    private Date endDate;
}
