package com.example.simpleoa.service;

import com.example.simpleoa.dto.statistics.*;

import java.util.Date;
import java.util.List;

/**
 * 预算统计服务接口
 * 提供全局预算和报销的多维度统计分析功能
 */
public interface BudgetStatisticsService {
    
    /**
     * 获取预算总览统计
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 预算总览DTO
     */
    BudgetOverviewDTO getBudgetOverview(Date startDate, Date endDate);
    
    /**
     * 按项目维度统计预算使用情况
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 项目预算统计列表
     */
    List<ProjectBudgetStatDTO> getStatisticsByProject(Date startDate, Date endDate);
    
    /**
     * 按费用类别维度统计
     * @param projectId 项目ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 类别统计列表
     */
    List<CategoryStatDTO> getStatisticsByCategory(Long projectId, Date startDate, Date endDate);
    
    /**
     * 按时间维度统计预算使用趋势
     * @param granularity 时间粒度：MONTH, QUARTER, YEAR
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 时间序列统计列表
     */
    List<TimeSeriesStatDTO> getStatisticsByTime(String granularity, Date startDate, Date endDate);
    
    /**
     * 按项目经理维度统计
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 项目经理统计列表
     */
    List<ManagerStatDTO> getStatisticsByManager(Date startDate, Date endDate);
    
    /**
     * 获取报销统计数据
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 报销统计DTO
     */
    ReimbursementStatDTO getReimbursementStatistics(Date startDate, Date endDate);
    
    /**
     * 获取预算监控列表
     * @param alertLevel 预警级别：ALL, WARNING, DANGER（可选）
     * @return 预算监控列表
     */
    List<BudgetMonitoringDTO> getBudgetMonitoring(String alertLevel);
    
    /**
     * 导出统计数据为Excel
     * @param exportType 导出类型：OVERVIEW, PROJECT, CATEGORY, REIMBURSEMENT
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return Excel文件字节数组
     */
    byte[] exportStatistics(String exportType, Date startDate, Date endDate);
}
