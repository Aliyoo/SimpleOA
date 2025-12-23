package com.example.simpleoa.controller;

import com.example.simpleoa.dto.statistics.*;
import com.example.simpleoa.service.BudgetStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 预算统计控制器
 * 提供全局预算和报销的统计分析功能
 * 仅限ADMIN和FINANCE角色访问
 */
@RestController
@RequestMapping("/api/budgets/statistics")
public class BudgetStatisticsController {

    private final BudgetStatisticsService budgetStatisticsService;

    @Autowired
    public BudgetStatisticsController(BudgetStatisticsService budgetStatisticsService) {
        this.budgetStatisticsService = budgetStatisticsService;
    }

    /**
     * 获取全局预算总览
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 预算总览数据
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public BudgetOverviewDTO getBudgetOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return budgetStatisticsService.getBudgetOverview(startDate, endDate);
    }

    /**
     * 按项目维度统计预算
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 项目预算统计列表
     */
    @GetMapping("/by-project")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public List<ProjectBudgetStatDTO> getStatisticsByProject(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return budgetStatisticsService.getStatisticsByProject(startDate, endDate);
    }

    /**
     * 按费用类别维度统计
     * @param projectId 项目ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 费用类别统计列表
     */
    @GetMapping("/by-category")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public List<CategoryStatDTO> getStatisticsByCategory(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return budgetStatisticsService.getStatisticsByCategory(projectId, startDate, endDate);
    }

    /**
     * 按时间维度统计
     * @param granularity 时间粒度（MONTH/QUARTER/YEAR）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 时间序列统计列表
     */
    @GetMapping("/by-time")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public List<TimeSeriesStatDTO> getStatisticsByTime(
            @RequestParam String granularity,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return budgetStatisticsService.getStatisticsByTime(granularity, startDate, endDate);
    }

    /**
     * 按项目经理维度统计
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 项目经理统计列表
     */
    @GetMapping("/by-manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public List<ManagerStatDTO> getStatisticsByManager(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return budgetStatisticsService.getStatisticsByManager(startDate, endDate);
    }

    /**
     * 获取报销统计数据
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 报销统计数据
     */
    @GetMapping("/reimbursement")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ReimbursementStatDTO getReimbursementStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return budgetStatisticsService.getReimbursementStatistics(startDate, endDate);
    }

    /**
     * 获取预算监控数据
     * @param alertLevel 预警级别（ALL/WARNING/DANGER，可选）
     * @return 预算监控列表
     */
    @GetMapping("/monitoring")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public List<BudgetMonitoringDTO> getBudgetMonitoring(
            @RequestParam(required = false) String alertLevel) {
        return budgetStatisticsService.getBudgetMonitoring(alertLevel);
    }

    /**
     * 导出统计数据为Excel
     * @param exportType 导出类型（OVERVIEW/PROJECT/CATEGORY/REIMBURSEMENT）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return Excel文件
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<byte[]> exportStatistics(
            @RequestParam String exportType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        
        byte[] excelData = budgetStatisticsService.exportStatistics(exportType, startDate, endDate);
        
        // 生成文件名
        String filename = "budget_statistics_" + exportType.toLowerCase() + "_" + 
                         System.currentTimeMillis() + ".xlsx";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}
