package com.example.simpleoa.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 项目排名DTO
 * 用于展示按报销金额的项目排名
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRankingDTO {
    
    /** 项目ID */
    private Long projectId;
    
    /** 项目名称 */
    private String projectName;
    
    /** 报销金额 */
    private BigDecimal reimbursementAmount;
    
    /** 排名 */
    private Integer rank;
}
