package com.example.simpleoa.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 项目经理统计DTO
 * 用于按项目经理维度展示预算使用汇总
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerStatDTO {
    
    /** 项目经理ID */
    private Long managerId;
    
    /** 项目经理名称 */
    private String managerName;
    
    /** 管理项目数量 */
    private Integer projectCount;
    
    /** 预算总额 */
    private BigDecimal totalBudget;
    
    /** 已使用金额 */
    private BigDecimal usedAmount;
    
    /** 使用率 (0-100) */
    private Double usageRate;
}
