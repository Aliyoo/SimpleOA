package com.example.simpleoa.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报销统计DTO
 * 用于展示全局报销数据统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReimbursementStatDTO {
    
    /** 报销总金额 */
    private BigDecimal totalAmount;
    
    /** 报销申请总数 */
    private Integer totalCount;
    
    /** 已审批数量 */
    private Integer approvedCount;
    
    /** 待审批数量 */
    private Integer pendingCount;
    
    /** 已驳回数量 */
    private Integer rejectedCount;
    
    /** 草稿数量 */
    private Integer draftCount;
    
    /** 审批通过率 (0-100) */
    private Double approvalRate;
    
    /** 平均审批时长(天) */
    private Double avgApprovalDays;
    
    /** 状态分布 */
    private List<StatusDistributionDTO> statusDistribution;
    
    /** 类别分布 */
    private List<CategoryDistributionDTO> categoryDistribution;
    
    /** 项目排名 */
    private List<ProjectRankingDTO> projectRanking;
}
