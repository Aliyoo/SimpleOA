package com.example.simpleoa.service.impl;

import com.example.simpleoa.dto.statistics.*;
import com.example.simpleoa.model.Budget;
import com.example.simpleoa.model.BudgetExpense;
import com.example.simpleoa.model.ReimbursementItem;
import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.ReimbursementStatus;
import com.example.simpleoa.repository.BudgetExpenseRepository;
import com.example.simpleoa.repository.BudgetRepository;
import com.example.simpleoa.repository.ReimbursementRequestRepository;
import com.example.simpleoa.service.BudgetStatisticsService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预算统计服务实现类
 */
@Service
@Transactional(readOnly = true)
public class BudgetStatisticsServiceImpl implements BudgetStatisticsService {

    private final BudgetRepository budgetRepository;
    private final BudgetExpenseRepository budgetExpenseRepository;
    private final ReimbursementRequestRepository reimbursementRequestRepository;

    @Autowired
    public BudgetStatisticsServiceImpl(
            BudgetRepository budgetRepository,
            BudgetExpenseRepository budgetExpenseRepository,
            ReimbursementRequestRepository reimbursementRequestRepository) {
        this.budgetRepository = budgetRepository;
        this.budgetExpenseRepository = budgetExpenseRepository;
        this.reimbursementRequestRepository = reimbursementRequestRepository;
    }

    @Override
    public BudgetOverviewDTO getBudgetOverview(Date startDate, Date endDate) {
        // 获取所有预算数据
        List<Budget> budgets = getAllBudgetsInRange(startDate, endDate);
        
        // 计算预算总额、已使用金额、剩余金额
        BigDecimal totalBudgetAmount = budgets.stream()
                .map(b -> BigDecimal.valueOf(b.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalUsedAmount = budgets.stream()
                .map(b -> BigDecimal.valueOf(b.getUsedAmount() != null ? b.getUsedAmount() : 0.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalRemainingAmount = totalBudgetAmount.subtract(totalUsedAmount);
        
        // 计算整体使用率
        Double overallUsageRate = totalBudgetAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalUsedAmount.divide(totalBudgetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        
        // 统计活跃/预警/关闭预算数量
        int activeBudgetCount = 0;
        int warningBudgetCount = 0;
        int closedBudgetCount = 0;
        
        for (Budget budget : budgets) {
            String status = budget.getStatus();
            if (status == null) continue;
            
            if ("活跃".equals(status) || "ACTIVE".equals(status)) {
                activeBudgetCount++;
                // 检查是否需要预警
                double usageRate = budget.getTotalAmount() > 0 
                    ? (budget.getUsedAmount() != null ? budget.getUsedAmount() : 0.0) / budget.getTotalAmount() 
                    : 0.0;
                if (usageRate >= 0.8) {
                    warningBudgetCount++;
                }
            } else if ("关闭".equals(status) || "CLOSED".equals(status)) {
                closedBudgetCount++;
            }
        }
        
        // 获取报销总金额
        BigDecimal totalReimbursementAmount = getReimbursementTotalAmount(startDate, endDate);
        
        // 计算预算执行率（报销金额/预算总额）
        Double budgetExecutionRate = totalBudgetAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalReimbursementAmount.divide(totalBudgetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        
        return BudgetOverviewDTO.builder()
                .totalBudgetAmount(totalBudgetAmount)
                .totalUsedAmount(totalUsedAmount)
                .totalRemainingAmount(totalRemainingAmount)
                .overallUsageRate(overallUsageRate)
                .activeBudgetCount(activeBudgetCount)
                .warningBudgetCount(warningBudgetCount)
                .closedBudgetCount(closedBudgetCount)
                .totalReimbursementAmount(totalReimbursementAmount)
                .budgetExecutionRate(budgetExecutionRate)
                .build();
    }

    @Override
    public List<ProjectBudgetStatDTO> getStatisticsByProject(Date startDate, Date endDate) {
        List<Budget> budgets = getAllBudgetsInRange(startDate, endDate);
        
        // 按项目分组统计
        Map<Long, List<Budget>> budgetsByProject = budgets.stream()
                .filter(b -> b.getProject() != null)
                .collect(Collectors.groupingBy(b -> b.getProject().getId()));
        
        List<ProjectBudgetStatDTO> result = new ArrayList<>();
        int rank = 1;
        
        for (Map.Entry<Long, List<Budget>> entry : budgetsByProject.entrySet()) {
            List<Budget> projectBudgets = entry.getValue();
            if (projectBudgets.isEmpty()) continue;
            
            // 获取项目信息（从第一个预算中获取）
            Budget firstBudget = projectBudgets.get(0);
            Long projectId = firstBudget.getProject().getId();
            String projectName = firstBudget.getProject().getName();
            String managerName = firstBudget.getProject().getManager() != null 
                    ? firstBudget.getProject().getManager().getRealName() 
                    : "未分配";
            
            // 计算项目预算汇总
            BigDecimal totalBudget = projectBudgets.stream()
                    .map(b -> BigDecimal.valueOf(b.getTotalAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal usedAmount = projectBudgets.stream()
                    .map(b -> BigDecimal.valueOf(b.getUsedAmount() != null ? b.getUsedAmount() : 0.0))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal remainingAmount = totalBudget.subtract(usedAmount);
            
            Double usageRate = totalBudget.compareTo(BigDecimal.ZERO) > 0
                    ? usedAmount.divide(totalBudget, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;
            
            result.add(ProjectBudgetStatDTO.builder()
                    .projectId(projectId)
                    .projectName(projectName)
                    .managerName(managerName)
                    .totalBudget(totalBudget)
                    .usedAmount(usedAmount)
                    .remainingAmount(remainingAmount)
                    .usageRate(usageRate)
                    .budgetCount(projectBudgets.size())
                    .rank(rank++)
                    .build());
        }
        
        // 按使用率降序排序
        result.sort((a, b) -> Double.compare(b.getUsageRate(), a.getUsageRate()));
        
        // 重新设置排名
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }
        
        return result;
    }

    @Override
    public List<CategoryStatDTO> getStatisticsByCategory(Long projectId, Date startDate, Date endDate) {
        // 获取预算支出数据
        List<BudgetExpense> expenses;
        
        if (projectId != null) {
            expenses = budgetExpenseRepository.findByProjectId(projectId);
        } else {
            expenses = budgetExpenseRepository.findAll();
        }
        
        // 按时间范围筛选
        if (startDate != null && endDate != null) {
            expenses = expenses.stream()
                    .filter(e -> e.getExpenseDate() != null 
                            && !e.getExpenseDate().before(startDate)
                            && !e.getExpenseDate().after(endDate))
                    .collect(Collectors.toList());
        }
        
        // 按费用类别分组统计
        Map<String, List<BudgetExpense>> expensesByCategory = expenses.stream()
                .filter(e -> e.getExpenseType() != null)
                .collect(Collectors.groupingBy(BudgetExpense::getExpenseType));
        
        // 计算总金额用于计算百分比
        BigDecimal totalAmount = expenses.stream()
                .map(e -> BigDecimal.valueOf(e.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<CategoryStatDTO> result = new ArrayList<>();
        
        for (Map.Entry<String, List<BudgetExpense>> entry : expensesByCategory.entrySet()) {
            String category = entry.getKey();
            List<BudgetExpense> categoryExpenses = entry.getValue();
            
            BigDecimal categoryAmount = categoryExpenses.stream()
                    .map(e -> BigDecimal.valueOf(e.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Double percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? categoryAmount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;
            
            result.add(CategoryStatDTO.builder()
                    .category(category)
                    .totalAmount(categoryAmount)
                    .count(categoryExpenses.size())
                    .percentage(percentage)
                    .build());
        }
        
        // 按金额降序排序
        result.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        
        return result;
    }

    @Override
    public List<TimeSeriesStatDTO> getStatisticsByTime(String granularity, Date startDate, Date endDate) {
        List<Budget> budgets = getAllBudgetsInRange(startDate, endDate);
        List<BudgetExpense> expenses = budgetExpenseRepository.findAll();
        
        // 按时间范围筛选支出
        if (startDate != null && endDate != null) {
            expenses = expenses.stream()
                    .filter(e -> e.getExpenseDate() != null 
                            && !e.getExpenseDate().before(startDate)
                            && !e.getExpenseDate().after(endDate))
                    .collect(Collectors.toList());
        }
        
        // 按时间粒度分组
        Map<String, List<Budget>> budgetsByPeriod = new LinkedHashMap<>();
        Map<String, List<BudgetExpense>> expensesByPeriod = new LinkedHashMap<>();
        
        Calendar cal = Calendar.getInstance();
        
        for (Budget budget : budgets) {
            if (budget.getStartDate() == null) continue;
            cal.setTime(budget.getStartDate());
            String period = formatPeriod(cal, granularity);
            budgetsByPeriod.computeIfAbsent(period, k -> new ArrayList<>()).add(budget);
        }
        
        for (BudgetExpense expense : expenses) {
            if (expense.getExpenseDate() == null) continue;
            cal.setTime(expense.getExpenseDate());
            String period = formatPeriod(cal, granularity);
            expensesByPeriod.computeIfAbsent(period, k -> new ArrayList<>()).add(expense);
        }
        
        // 合并所有时间周期
        Set<String> allPeriods = new TreeSet<>();
        allPeriods.addAll(budgetsByPeriod.keySet());
        allPeriods.addAll(expensesByPeriod.keySet());
        
        List<TimeSeriesStatDTO> result = new ArrayList<>();
        
        for (String period : allPeriods) {
            List<Budget> periodBudgets = budgetsByPeriod.getOrDefault(period, new ArrayList<>());
            List<BudgetExpense> periodExpenses = expensesByPeriod.getOrDefault(period, new ArrayList<>());
            
            BigDecimal budgetAmount = periodBudgets.stream()
                    .map(b -> BigDecimal.valueOf(b.getTotalAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal usedAmount = periodBudgets.stream()
                    .map(b -> BigDecimal.valueOf(b.getUsedAmount() != null ? b.getUsedAmount() : 0.0))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal reimbursementAmount = periodExpenses.stream()
                    .map(e -> BigDecimal.valueOf(e.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Double usageRate = budgetAmount.compareTo(BigDecimal.ZERO) > 0
                    ? usedAmount.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;
            
            result.add(TimeSeriesStatDTO.builder()
                    .period(period)
                    .budgetAmount(budgetAmount)
                    .usedAmount(usedAmount)
                    .reimbursementAmount(reimbursementAmount)
                    .usageRate(usageRate)
                    .build());
        }
        
        return result;
    }

    @Override
    public List<ManagerStatDTO> getStatisticsByManager(Date startDate, Date endDate) {
        List<Budget> budgets = getAllBudgetsInRange(startDate, endDate);
        
        // 按项目经理分组统计
        Map<Long, List<Budget>> budgetsByManager = budgets.stream()
                .filter(b -> b.getProject() != null && b.getProject().getManager() != null)
                .collect(Collectors.groupingBy(b -> b.getProject().getManager().getId()));
        
        List<ManagerStatDTO> result = new ArrayList<>();
        
        for (Map.Entry<Long, List<Budget>> entry : budgetsByManager.entrySet()) {
            List<Budget> managerBudgets = entry.getValue();
            if (managerBudgets.isEmpty()) continue;
            
            // 获取项目经理信息
            Budget firstBudget = managerBudgets.get(0);
            Long managerId = firstBudget.getProject().getManager().getId();
            String managerName = firstBudget.getProject().getManager().getRealName();
            
            // 统计该经理管理的项目数量（去重）
            Set<Long> projectIds = managerBudgets.stream()
                    .map(b -> b.getProject().getId())
                    .collect(Collectors.toSet());
            
            // 计算预算汇总
            BigDecimal totalBudget = managerBudgets.stream()
                    .map(b -> BigDecimal.valueOf(b.getTotalAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal usedAmount = managerBudgets.stream()
                    .map(b -> BigDecimal.valueOf(b.getUsedAmount() != null ? b.getUsedAmount() : 0.0))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Double usageRate = totalBudget.compareTo(BigDecimal.ZERO) > 0
                    ? usedAmount.divide(totalBudget, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;
            
            result.add(ManagerStatDTO.builder()
                    .managerId(managerId)
                    .managerName(managerName)
                    .projectCount(projectIds.size())
                    .totalBudget(totalBudget)
                    .usedAmount(usedAmount)
                    .usageRate(usageRate)
                    .build());
        }
        
        // 按预算总额降序排序
        result.sort((a, b) -> b.getTotalBudget().compareTo(a.getTotalBudget()));
        
        return result;
    }

    @Override
    public ReimbursementStatDTO getReimbursementStatistics(Date startDate, Date endDate) {
        // 获取报销申请数据
        List<ReimbursementRequest> reimbursements;
        
        if (startDate != null && endDate != null) {
            java.time.LocalDateTime startDateTime = new java.sql.Timestamp(startDate.getTime()).toLocalDateTime();
            java.time.LocalDateTime endDateTime = new java.sql.Timestamp(endDate.getTime()).toLocalDateTime();
            reimbursements = reimbursementRequestRepository.findAll().stream()
                    .filter(r -> r.getCreateTime() != null 
                            && !r.getCreateTime().isBefore(startDateTime)
                            && !r.getCreateTime().isAfter(endDateTime))
                    .collect(Collectors.toList());
        } else {
            reimbursements = reimbursementRequestRepository.findAll();
        }
        
        // 计算报销总金额和总数
        BigDecimal totalAmount = reimbursements.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalCount = reimbursements.size();
        
        // 按状态统计数量
        int approvedCount = 0;
        int pendingCount = 0;
        int rejectedCount = 0;
        int draftCount = 0;
        
        for (ReimbursementRequest r : reimbursements) {
            ReimbursementStatus status = r.getStatus();
            if (status == null) continue;
            
            switch (status) {
                case APPROVED:
                    approvedCount++;
                    break;
                case PENDING_MANAGER_APPROVAL:
                case PENDING_FINANCE_APPROVAL:
                    pendingCount++;
                    break;
                case REJECTED:
                    rejectedCount++;
                    break;
                case DRAFT:
                    draftCount++;
                    break;
            }
        }
        
        // 计算审批通过率
        int totalProcessed = approvedCount + rejectedCount;
        Double approvalRate = totalProcessed > 0 
                ? (double) approvedCount / totalProcessed * 100 
                : 0.0;
        
        // 计算平均审批时长（天）
        Double avgApprovalDays = calculateAvgApprovalDays(reimbursements);
        
        // 构建状态分布
        List<StatusDistributionDTO> statusDistribution = buildStatusDistribution(reimbursements);
        
        // 构建类别分布
        List<CategoryDistributionDTO> categoryDistribution = buildCategoryDistribution(reimbursements);
        
        // 构建项目排名
        List<ProjectRankingDTO> projectRanking = buildProjectRanking(reimbursements);
        
        return ReimbursementStatDTO.builder()
                .totalAmount(totalAmount)
                .totalCount(totalCount)
                .approvedCount(approvedCount)
                .pendingCount(pendingCount)
                .rejectedCount(rejectedCount)
                .draftCount(draftCount)
                .approvalRate(approvalRate)
                .avgApprovalDays(avgApprovalDays)
                .statusDistribution(statusDistribution)
                .categoryDistribution(categoryDistribution)
                .projectRanking(projectRanking)
                .build();
    }

    @Override
    public List<BudgetMonitoringDTO> getBudgetMonitoring(String alertLevel) {
        // 获取所有预算
        List<Budget> budgets = budgetRepository.findAll();
        
        List<BudgetMonitoringDTO> result = new ArrayList<>();
        
        for (Budget budget : budgets) {
            // 计算使用率
            double usageRate = budget.getTotalAmount() > 0 
                    ? (budget.getUsedAmount() != null ? budget.getUsedAmount() : 0.0) / budget.getTotalAmount() * 100
                    : 0.0;
            
            // 判断预警级别
            String budgetAlertLevel = determineAlertLevel(usageRate);
            
            // 根据筛选条件过滤
            if (alertLevel != null && !"ALL".equalsIgnoreCase(alertLevel)) {
                if (!budgetAlertLevel.equalsIgnoreCase(alertLevel)) {
                    continue;
                }
            }
            
            String projectName = budget.getProject() != null ? budget.getProject().getName() : "未关联项目";
            
            result.add(BudgetMonitoringDTO.builder()
                    .budgetId(budget.getId())
                    .budgetName(budget.getName())
                    .projectName(projectName)
                    .budgetType(budget.getBudgetType())
                    .totalAmount(BigDecimal.valueOf(budget.getTotalAmount()))
                    .usedAmount(BigDecimal.valueOf(budget.getUsedAmount() != null ? budget.getUsedAmount() : 0.0))
                    .remainingAmount(BigDecimal.valueOf(budget.getRemainingAmount() != null ? budget.getRemainingAmount() : budget.getTotalAmount()))
                    .usageRate(usageRate)
                    .alertLevel(budgetAlertLevel)
                    .startDate(budget.getStartDate())
                    .endDate(budget.getEndDate())
                    .build());
        }
        
        // 按使用率降序排序
        result.sort((a, b) -> Double.compare(b.getUsageRate(), a.getUsageRate()));
        
        return result;
    }

    @Override
    public byte[] exportStatistics(String exportType, Date startDate, Date endDate) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            
            // 根据导出类型创建不同的工作表
            switch (exportType.toUpperCase()) {
                case "OVERVIEW":
                    createOverviewSheet(workbook, headerStyle, dataStyle, dateStyle, startDate, endDate);
                    break;
                case "PROJECT":
                    createProjectStatisticsSheet(workbook, headerStyle, dataStyle, startDate, endDate);
                    break;
                case "CATEGORY":
                    createCategoryStatisticsSheet(workbook, headerStyle, dataStyle, startDate, endDate);
                    break;
                case "REIMBURSEMENT":
                    createReimbursementStatisticsSheet(workbook, headerStyle, dataStyle, startDate, endDate);
                    break;
                default:
                    // 默认导出所有统计数据
                    createOverviewSheet(workbook, headerStyle, dataStyle, dateStyle, startDate, endDate);
                    createProjectStatisticsSheet(workbook, headerStyle, dataStyle, startDate, endDate);
                    createCategoryStatisticsSheet(workbook, headerStyle, dataStyle, startDate, endDate);
                    createReimbursementStatisticsSheet(workbook, headerStyle, dataStyle, startDate, endDate);
                    break;
            }
            
            // 自动调整列宽
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet.getPhysicalNumberOfRows() > 0) {
                    Row firstRow = sheet.getRow(sheet.getFirstRowNum());
                    if (firstRow != null) {
                        for (int j = 0; j < firstRow.getLastCellNum(); j++) {
                            sheet.autoSizeColumn(j);
                        }
                    }
                }
            }
            
            workbook.write(out);
            return out.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建预算总览工作表
     */
    private void createOverviewSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, 
                                     CellStyle dateStyle, Date startDate, Date endDate) {
        Sheet sheet = workbook.createSheet("预算总览");
        int rowIdx = 0;
        
        // 添加标题和筛选条件
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("预算总览统计报表");
        titleCell.setCellStyle(headerStyle);
        
        // 添加数据生成时间
        Row timeRow = sheet.createRow(rowIdx++);
        Cell timeCell = timeRow.createCell(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        timeCell.setCellValue("生成时间: " + sdf.format(new Date()));
        
        // 添加筛选条件
        if (startDate != null && endDate != null) {
            Row filterRow = sheet.createRow(rowIdx++);
            Cell filterCell = filterRow.createCell(0);
            SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd");
            filterCell.setCellValue("筛选条件: " + dateSdf.format(startDate) + " 至 " + dateSdf.format(endDate));
        }
        
        // 空行
        rowIdx++;
        
        // 获取总览数据
        BudgetOverviewDTO overview = getBudgetOverview(startDate, endDate);
        
        // 创建表头
        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"指标名称", "数值"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 填充数据
        addDataRow(sheet, rowIdx++, dataStyle, "预算总额", overview.getTotalBudgetAmount());
        addDataRow(sheet, rowIdx++, dataStyle, "已使用金额", overview.getTotalUsedAmount());
        addDataRow(sheet, rowIdx++, dataStyle, "剩余金额", overview.getTotalRemainingAmount());
        addDataRow(sheet, rowIdx++, dataStyle, "整体使用率(%)", overview.getOverallUsageRate());
        addDataRow(sheet, rowIdx++, dataStyle, "活跃预算数量", overview.getActiveBudgetCount());
        addDataRow(sheet, rowIdx++, dataStyle, "预警预算数量", overview.getWarningBudgetCount());
        addDataRow(sheet, rowIdx++, dataStyle, "已关闭预算数量", overview.getClosedBudgetCount());
        addDataRow(sheet, rowIdx++, dataStyle, "报销总金额", overview.getTotalReimbursementAmount());
        addDataRow(sheet, rowIdx++, dataStyle, "预算执行率(%)", overview.getBudgetExecutionRate());
    }
    
    /**
     * 创建项目统计工作表
     */
    private void createProjectStatisticsSheet(Workbook workbook, CellStyle headerStyle, 
                                             CellStyle dataStyle, Date startDate, Date endDate) {
        Sheet sheet = workbook.createSheet("项目统计");
        int rowIdx = 0;
        
        // 添加标题
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("按项目统计");
        titleCell.setCellStyle(headerStyle);
        
        // 空行
        rowIdx++;
        
        // 获取项目统计数据
        List<ProjectBudgetStatDTO> projectStats = getStatisticsByProject(startDate, endDate);
        
        // 创建表头
        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"排名", "项目名称", "项目经理", "预算总额", "已使用金额", "剩余金额", "使用率(%)", "预算数量"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 填充数据
        for (ProjectBudgetStatDTO stat : projectStats) {
            Row dataRow = sheet.createRow(rowIdx++);
            int colIdx = 0;
            
            dataRow.createCell(colIdx++).setCellValue(stat.getRank());
            dataRow.createCell(colIdx++).setCellValue(stat.getProjectName());
            dataRow.createCell(colIdx++).setCellValue(stat.getManagerName());
            dataRow.createCell(colIdx++).setCellValue(stat.getTotalBudget().doubleValue());
            dataRow.createCell(colIdx++).setCellValue(stat.getUsedAmount().doubleValue());
            dataRow.createCell(colIdx++).setCellValue(stat.getRemainingAmount().doubleValue());
            dataRow.createCell(colIdx++).setCellValue(stat.getUsageRate());
            dataRow.createCell(colIdx++).setCellValue(stat.getBudgetCount());
            
            // 应用样式
            for (int i = 0; i < colIdx; i++) {
                dataRow.getCell(i).setCellStyle(dataStyle);
            }
        }
    }
    
    /**
     * 创建类别统计工作表
     */
    private void createCategoryStatisticsSheet(Workbook workbook, CellStyle headerStyle, 
                                              CellStyle dataStyle, Date startDate, Date endDate) {
        Sheet sheet = workbook.createSheet("类别统计");
        int rowIdx = 0;
        
        // 添加标题
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("按费用类别统计");
        titleCell.setCellStyle(headerStyle);
        
        // 空行
        rowIdx++;
        
        // 获取类别统计数据
        List<CategoryStatDTO> categoryStats = getStatisticsByCategory(null, startDate, endDate);
        
        // 创建表头
        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"费用类别", "总金额", "数量", "占比(%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 填充数据
        for (CategoryStatDTO stat : categoryStats) {
            Row dataRow = sheet.createRow(rowIdx++);
            int colIdx = 0;
            
            dataRow.createCell(colIdx++).setCellValue(stat.getCategory());
            dataRow.createCell(colIdx++).setCellValue(stat.getTotalAmount().doubleValue());
            dataRow.createCell(colIdx++).setCellValue(stat.getCount());
            dataRow.createCell(colIdx++).setCellValue(stat.getPercentage());
            
            // 应用样式
            for (int i = 0; i < colIdx; i++) {
                dataRow.getCell(i).setCellStyle(dataStyle);
            }
        }
    }
    
    /**
     * 创建报销统计工作表
     */
    private void createReimbursementStatisticsSheet(Workbook workbook, CellStyle headerStyle, 
                                                   CellStyle dataStyle, Date startDate, Date endDate) {
        Sheet sheet = workbook.createSheet("报销统计");
        int rowIdx = 0;
        
        // 添加标题
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("报销统计汇总");
        titleCell.setCellStyle(headerStyle);
        
        // 空行
        rowIdx++;
        
        // 获取报销统计数据
        ReimbursementStatDTO reimbursementStat = getReimbursementStatistics(startDate, endDate);
        
        // 汇总信息
        Row summaryHeaderRow = sheet.createRow(rowIdx++);
        String[] summaryHeaders = {"指标名称", "数值"};
        for (int i = 0; i < summaryHeaders.length; i++) {
            Cell cell = summaryHeaderRow.createCell(i);
            cell.setCellValue(summaryHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        
        addDataRow(sheet, rowIdx++, dataStyle, "报销总金额", reimbursementStat.getTotalAmount());
        addDataRow(sheet, rowIdx++, dataStyle, "报销申请总数", reimbursementStat.getTotalCount());
        addDataRow(sheet, rowIdx++, dataStyle, "已审批数量", reimbursementStat.getApprovedCount());
        addDataRow(sheet, rowIdx++, dataStyle, "待审批数量", reimbursementStat.getPendingCount());
        addDataRow(sheet, rowIdx++, dataStyle, "已驳回数量", reimbursementStat.getRejectedCount());
        addDataRow(sheet, rowIdx++, dataStyle, "草稿数量", reimbursementStat.getDraftCount());
        addDataRow(sheet, rowIdx++, dataStyle, "审批通过率(%)", reimbursementStat.getApprovalRate());
        addDataRow(sheet, rowIdx++, dataStyle, "平均审批时长(天)", reimbursementStat.getAvgApprovalDays());
        
        // 空行
        rowIdx += 2;
        
        // 状态分布
        Row statusTitleRow = sheet.createRow(rowIdx++);
        Cell statusTitleCell = statusTitleRow.createCell(0);
        statusTitleCell.setCellValue("状态分布");
        statusTitleCell.setCellStyle(headerStyle);
        
        Row statusHeaderRow = sheet.createRow(rowIdx++);
        String[] statusHeaders = {"状态", "状态标签", "数量", "金额", "占比(%)"};
        for (int i = 0; i < statusHeaders.length; i++) {
            Cell cell = statusHeaderRow.createCell(i);
            cell.setCellValue(statusHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        
        for (StatusDistributionDTO statusDist : reimbursementStat.getStatusDistribution()) {
            Row dataRow = sheet.createRow(rowIdx++);
            int colIdx = 0;
            
            dataRow.createCell(colIdx++).setCellValue(statusDist.getStatus());
            dataRow.createCell(colIdx++).setCellValue(statusDist.getStatusLabel());
            dataRow.createCell(colIdx++).setCellValue(statusDist.getCount());
            dataRow.createCell(colIdx++).setCellValue(statusDist.getAmount().doubleValue());
            dataRow.createCell(colIdx++).setCellValue(statusDist.getPercentage());
            
            for (int i = 0; i < colIdx; i++) {
                dataRow.getCell(i).setCellStyle(dataStyle);
            }
        }
        
        // 空行
        rowIdx += 2;
        
        // 项目排名
        Row projectTitleRow = sheet.createRow(rowIdx++);
        Cell projectTitleCell = projectTitleRow.createCell(0);
        projectTitleCell.setCellValue("项目报销排名");
        projectTitleCell.setCellStyle(headerStyle);
        
        Row projectHeaderRow = sheet.createRow(rowIdx++);
        String[] projectHeaders = {"排名", "项目名称", "报销金额"};
        for (int i = 0; i < projectHeaders.length; i++) {
            Cell cell = projectHeaderRow.createCell(i);
            cell.setCellValue(projectHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        
        for (ProjectRankingDTO ranking : reimbursementStat.getProjectRanking()) {
            Row dataRow = sheet.createRow(rowIdx++);
            int colIdx = 0;
            
            dataRow.createCell(colIdx++).setCellValue(ranking.getRank());
            dataRow.createCell(colIdx++).setCellValue(ranking.getProjectName());
            dataRow.createCell(colIdx++).setCellValue(ranking.getReimbursementAmount().doubleValue());
            
            for (int i = 0; i < colIdx; i++) {
                dataRow.getCell(i).setCellStyle(dataStyle);
            }
        }
    }
    
    /**
     * 添加数据行（键值对）
     */
    private void addDataRow(Sheet sheet, int rowIdx, CellStyle dataStyle, String label, Object value) {
        Row row = sheet.createRow(rowIdx);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(dataStyle);
        
        Cell valueCell = row.createCell(1);
        if (value instanceof BigDecimal) {
            valueCell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Number) {
            valueCell.setCellValue(((Number) value).doubleValue());
        } else if (value != null) {
            valueCell.setCellValue(value.toString());
        } else {
            valueCell.setCellValue("N/A");
        }
        valueCell.setCellStyle(dataStyle);
    }
    
    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    /**
     * 创建日期样式
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd"));
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    // ========== 辅助方法 ==========
    
    /**
     * 获取指定时间范围内的所有预算
     */
    private List<Budget> getAllBudgetsInRange(Date startDate, Date endDate) {
        if (startDate != null && endDate != null) {
            return budgetRepository.findByStartDateBetweenOrEndDateBetween(
                    startDate, endDate, startDate, endDate);
        } else {
            return budgetRepository.findAll();
        }
    }
    
    /**
     * 获取指定时间范围内的报销总金额
     */
    private BigDecimal getReimbursementTotalAmount(Date startDate, Date endDate) {
        List<ReimbursementRequest> reimbursements;
        
        if (startDate != null && endDate != null) {
            // 转换Date为LocalDateTime
            java.time.LocalDateTime startDateTime = new java.sql.Timestamp(startDate.getTime()).toLocalDateTime();
            java.time.LocalDateTime endDateTime = new java.sql.Timestamp(endDate.getTime()).toLocalDateTime();
            reimbursements = reimbursementRequestRepository.findAll().stream()
                    .filter(r -> r.getCreateTime() != null 
                            && !r.getCreateTime().isBefore(startDateTime)
                            && !r.getCreateTime().isAfter(endDateTime))
                    .collect(Collectors.toList());
        } else {
            reimbursements = reimbursementRequestRepository.findAll();
        }
        
        return reimbursements.stream()
                .filter(r -> r.getStatus() == ReimbursementStatus.APPROVED)
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 根据时间粒度格式化时间周期
     */
    private String formatPeriod(Calendar cal, String granularity) {
        int year = cal.get(Calendar.YEAR);
        
        if ("YEAR".equalsIgnoreCase(granularity)) {
            return String.valueOf(year);
        } else if ("QUARTER".equalsIgnoreCase(granularity)) {
            int quarter = (cal.get(Calendar.MONTH) / 3) + 1;
            return year + "-Q" + quarter;
        } else { // MONTH
            int month = cal.get(Calendar.MONTH) + 1;
            return String.format("%d-%02d", year, month);
        }
    }
    
    /**
     * 计算平均审批时长（天）
     */
    private Double calculateAvgApprovalDays(List<ReimbursementRequest> reimbursements) {
        List<Long> approvalDays = new ArrayList<>();
        
        for (ReimbursementRequest r : reimbursements) {
            if (r.getStatus() == ReimbursementStatus.APPROVED 
                    && r.getCreateTime() != null 
                    && r.getUpdateTime() != null) {
                long days = java.time.Duration.between(r.getCreateTime(), r.getUpdateTime()).toDays();
                approvalDays.add(days);
            }
        }
        
        if (approvalDays.isEmpty()) {
            return 0.0;
        }
        
        double sum = approvalDays.stream().mapToLong(Long::longValue).sum();
        return sum / approvalDays.size();
    }
    
    /**
     * 构建状态分布
     */
    private List<StatusDistributionDTO> buildStatusDistribution(List<ReimbursementRequest> reimbursements) {
        Map<ReimbursementStatus, List<ReimbursementRequest>> byStatus = reimbursements.stream()
                .filter(r -> r.getStatus() != null)
                .collect(Collectors.groupingBy(ReimbursementRequest::getStatus));
        
        BigDecimal totalAmount = reimbursements.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<StatusDistributionDTO> result = new ArrayList<>();
        
        for (ReimbursementStatus status : ReimbursementStatus.values()) {
            List<ReimbursementRequest> statusReimbursements = byStatus.getOrDefault(status, new ArrayList<>());
            
            BigDecimal statusAmount = statusReimbursements.stream()
                    .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Double percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? statusAmount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;
            
            result.add(StatusDistributionDTO.builder()
                    .status(status.name())
                    .statusLabel(getStatusLabel(status))
                    .count(statusReimbursements.size())
                    .amount(statusAmount)
                    .percentage(percentage)
                    .build());
        }
        
        return result;
    }
    
    /**
     * 构建类别分布
     */
    private List<CategoryDistributionDTO> buildCategoryDistribution(List<ReimbursementRequest> reimbursements) {
        // 收集所有报销明细
        List<ReimbursementItem> allItems = reimbursements.stream()
                .filter(r -> r.getItems() != null)
                .flatMap(r -> r.getItems().stream())
                .collect(Collectors.toList());
        
        // 按类别分组
        Map<String, List<ReimbursementItem>> byCategory = allItems.stream()
                .filter(item -> item.getItemCategory() != null)
                .collect(Collectors.groupingBy(ReimbursementItem::getItemCategory));
        
        BigDecimal totalAmount = allItems.stream()
                .map(item -> item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<CategoryDistributionDTO> result = new ArrayList<>();
        
        for (Map.Entry<String, List<ReimbursementItem>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<ReimbursementItem> categoryItems = entry.getValue();
            
            BigDecimal categoryAmount = categoryItems.stream()
                    .map(item -> item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Double percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? categoryAmount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;
            
            result.add(CategoryDistributionDTO.builder()
                    .category(category)
                    .amount(categoryAmount)
                    .percentage(percentage)
                    .build());
        }
        
        // 按金额降序排序
        result.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        
        return result;
    }
    
    /**
     * 构建项目排名
     */
    private List<ProjectRankingDTO> buildProjectRanking(List<ReimbursementRequest> reimbursements) {
        // 按项目分组
        Map<Long, List<ReimbursementRequest>> byProject = reimbursements.stream()
                .filter(r -> r.getProject() != null && r.getStatus() == ReimbursementStatus.APPROVED)
                .collect(Collectors.groupingBy(r -> r.getProject().getId()));
        
        List<ProjectRankingDTO> result = new ArrayList<>();
        
        for (Map.Entry<Long, List<ReimbursementRequest>> entry : byProject.entrySet()) {
            List<ReimbursementRequest> projectReimbursements = entry.getValue();
            if (projectReimbursements.isEmpty()) continue;
            
            Long projectId = entry.getKey();
            String projectName = projectReimbursements.get(0).getProject().getName();
            
            BigDecimal projectAmount = projectReimbursements.stream()
                    .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            result.add(ProjectRankingDTO.builder()
                    .projectId(projectId)
                    .projectName(projectName)
                    .reimbursementAmount(projectAmount)
                    .build());
        }
        
        // 按金额降序排序
        result.sort((a, b) -> b.getReimbursementAmount().compareTo(a.getReimbursementAmount()));
        
        // 设置排名
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }
        
        return result;
    }
    
    /**
     * 获取状态标签
     */
    private String getStatusLabel(ReimbursementStatus status) {
        switch (status) {
            case DRAFT:
                return "草稿";
            case PENDING_MANAGER_APPROVAL:
                return "待项目经理审批";
            case PENDING_FINANCE_APPROVAL:
                return "待财务审批";
            case APPROVED:
                return "已通过";
            case REJECTED:
                return "已驳回";
            default:
                return status.name();
        }
    }
    
    /**
     * 判断预警级别
     * 使用率 [0, 80%) -> NORMAL
     * 使用率 [80%, 95%) -> WARNING
     * 使用率 [95%, 100%] -> DANGER
     */
    private String determineAlertLevel(double usageRate) {
        if (usageRate < 80.0) {
            return "NORMAL";
        } else if (usageRate < 95.0) {
            return "WARNING";
        } else {
            return "DANGER";
        }
    }
}
