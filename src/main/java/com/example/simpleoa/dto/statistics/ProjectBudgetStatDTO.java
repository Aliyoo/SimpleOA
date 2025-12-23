package com.example.simpleoa.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 项目预算统计DTO
 * 用于按项目维度展示预算使用情况
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBudgetStatDTO {
    
    /** 项目ID */
    private Long projectId;
    
    /** 项目名称 */
    private String projectName;
    
    /** 项目经理名称 */
    private String managerName;
    
    /** 预算总额 */
    private BigDecimal totalBudget;
    
    /** 已使用金额 */
    private BigDecimal usedAmount;
    
    /** 剩余金额 */
    private BigDecimal remainingAmount;
    
    /** 使用率 (0-100) */
    private Double usageRate;
    
    /** 预算数量 */
    private Integer budgetCount;
    
    /** 排名 */
    private Integer rank;
}
