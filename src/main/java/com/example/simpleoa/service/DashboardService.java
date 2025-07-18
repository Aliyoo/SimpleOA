package com.example.simpleoa.service;

import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final WorkTimeRecordRepository workTimeRecordRepository;
    private final BudgetRepository budgetRepository;
    private final ReimbursementRequestRepository reimbursementRequestRepository;
    private final LeaveRepository leaveRepository;

    @Autowired
    public DashboardService(UserRepository userRepository,
                           ProjectRepository projectRepository,
                           WorkTimeRecordRepository workTimeRecordRepository,
                           BudgetRepository budgetRepository,
                           ReimbursementRequestRepository reimbursementRequestRepository,
                           LeaveRepository leaveRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.workTimeRecordRepository = workTimeRecordRepository;
        this.budgetRepository = budgetRepository;
        this.reimbursementRequestRepository = reimbursementRequestRepository;
        this.leaveRepository = leaveRepository;
    }

    /**
     * 获取仪表盘基础统计数据
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 用户统计
        long userCount = userRepository.count();
        long activeUserCount = userRepository.countByEnabled(1);
        
        // 项目统计
        long projectCount = projectRepository.count();
        long activeProjectCount = projectRepository.countByStatus(ProjectStatus.IN_PROGRESS);
        
        // 待审批统计 - 目前使用工时记录的审批状态
        long pendingApprovalCount = workTimeRecordRepository.countByApproved(false);
        long todayApprovalCount = workTimeRecordRepository.countByApprovedAndDateRange(false, LocalDate.now(), LocalDate.now());
        
        // 预算统计
        Double totalBudgetObj = budgetRepository.sumTotalAmount();
        Double totalExpensesObj = budgetRepository.sumUsedAmount();
        double totalBudget = totalBudgetObj != null ? totalBudgetObj : 0.0;
        double totalExpenses = totalExpensesObj != null ? totalExpensesObj : 0.0;
        double budgetUtilization = totalBudget > 0 ? (totalExpenses / totalBudget) * 100 : 0;
        
        // 工时统计
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate monthStart = today.withDayOfMonth(1);
        
        Double todayWorkHoursObj = workTimeRecordRepository.sumHoursByDateRange(today, today);
        Double weekWorkHoursObj = workTimeRecordRepository.sumHoursByDateRange(weekStart, today);
        Double monthWorkHoursObj = workTimeRecordRepository.sumHoursByDateRange(monthStart, today);
        
        double todayWorkHours = todayWorkHoursObj != null ? todayWorkHoursObj : 0.0;
        double weekWorkHours = weekWorkHoursObj != null ? weekWorkHoursObj : 0.0;
        double monthWorkHours = monthWorkHoursObj != null ? monthWorkHoursObj : 0.0;
        
        // 报销统计 - 使用简化版本
        double monthReimbursementAmount = 0.0;
        long monthReimbursementCount = 0;
        try {
            monthReimbursementCount = reimbursementRequestRepository.count();
        } catch (Exception e) {
            // 忽略错误，使用默认值
        }
        
        // 请假统计 - 使用简化版本
        long monthLeaveCount = 0;
        try {
            monthLeaveCount = leaveRepository.count();
        } catch (Exception e) {
            // 忽略错误，使用默认值
        }
        
        stats.put("userCount", userCount);
        stats.put("activeUserCount", activeUserCount);
        stats.put("projectCount", projectCount);
        stats.put("activeProjectCount", activeProjectCount);
        stats.put("pendingApprovalCount", pendingApprovalCount);
        stats.put("todayApprovalCount", todayApprovalCount);
        stats.put("budgetUtilization", budgetUtilization);
        stats.put("totalBudget", totalBudget);
        stats.put("totalExpenses", totalExpenses);
        stats.put("todayWorkHours", todayWorkHours);
        stats.put("weekWorkHours", weekWorkHours);
        stats.put("monthWorkHours", monthWorkHours);
        stats.put("monthReimbursementAmount", monthReimbursementAmount);
        stats.put("monthReimbursementCount", monthReimbursementCount);
        stats.put("monthLeaveCount", monthLeaveCount);
        
        return stats;
    }

    /**
     * 获取工时趋势数据
     */
    public Map<String, Object> getWorktimeTrends(int days) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> dailyTrends = new ArrayList<>();
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Double hoursObj = workTimeRecordRepository.sumHoursByDateRange(date, date);
            double hours = hoursObj != null ? hoursObj : 0.0;
            
            Map<String, Object> trend = new HashMap<>();
            trend.put("date", date.getMonthValue() + "/" + date.getDayOfMonth());
            trend.put("hours", hours);
            
            dailyTrends.add(trend);
        }
        
        result.put("dailyTrends", dailyTrends);
        return result;
    }

    /**
     * 获取项目统计数据
     */
    public Map<String, Object> getProjectStats() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有项目
        List<Project> projects = projectRepository.findAll();
        
        List<Map<String, Object>> projectWorkHours = new ArrayList<>();
        
        for (Project project : projects) {
            Double totalHoursObj = workTimeRecordRepository.sumHoursByProject(project);
            double totalHours = totalHoursObj != null ? totalHoursObj : 0.0;
            if (totalHours > 0) {
                Map<String, Object> projectStat = new HashMap<>();
                projectStat.put("name", project.getName());
                projectStat.put("value", totalHours);
                
                projectWorkHours.add(projectStat);
            }
        }
        
        // 按工时降序排序
        projectWorkHours.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
        
        result.put("projectWorkHours", projectWorkHours);
        return result;
    }

    /**
     * 获取审批统计数据
     */
    public Map<String, Object> getApprovalStats() {
        Map<String, Object> result = new HashMap<>();
        
        // 模拟审批类型分布数据
        List<Map<String, Object>> approvalTypeDistribution = new ArrayList<>();
        
        // 工时审批
        long worktimeApprovalCount = workTimeRecordRepository.countByApproved(false);
        if (worktimeApprovalCount > 0) {
            Map<String, Object> worktimeApproval = new HashMap<>();
            worktimeApproval.put("name", "工时审批");
            worktimeApproval.put("value", worktimeApprovalCount);
            approvalTypeDistribution.add(worktimeApproval);
        }
        
        // 请假申请 - 使用简化版本
        try {
            long leaveApprovalCount = leaveRepository.count();
            if (leaveApprovalCount > 0) {
                Map<String, Object> leaveApproval = new HashMap<>();
                leaveApproval.put("name", "请假申请");
                leaveApproval.put("value", leaveApprovalCount);
                approvalTypeDistribution.add(leaveApproval);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        
        // 报销申请 - 使用简化版本
        try {
            long reimbursementApprovalCount = reimbursementRequestRepository.count();
            if (reimbursementApprovalCount > 0) {
                Map<String, Object> reimbursementApproval = new HashMap<>();
                reimbursementApproval.put("name", "报销申请");
                reimbursementApproval.put("value", reimbursementApprovalCount);
                approvalTypeDistribution.add(reimbursementApproval);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        
        result.put("approvalTypeDistribution", approvalTypeDistribution);
        return result;
    }
}
