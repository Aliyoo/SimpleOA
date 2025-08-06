package com.example.simpleoa.service;

import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.*;
import com.example.simpleoa.service.ProjectService;
import com.example.simpleoa.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final BudgetService budgetService;

    @Autowired
    public DashboardService(UserRepository userRepository,
                           ProjectRepository projectRepository,
                           WorkTimeRecordRepository workTimeRecordRepository,
                           BudgetRepository budgetRepository,
                           ReimbursementRequestRepository reimbursementRequestRepository,
                           LeaveRepository leaveRepository,
                           TaskRepository taskRepository,
                           ProjectService projectService,
                           BudgetService budgetService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.workTimeRecordRepository = workTimeRecordRepository;
        this.budgetRepository = budgetRepository;
        this.reimbursementRequestRepository = reimbursementRequestRepository;
        this.leaveRepository = leaveRepository;
        this.taskRepository = taskRepository;
        this.projectService = projectService;
        this.budgetService = budgetService;
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
        long activeProjectCount = projectRepository.countByStatus(ProjectStatus.DEVELOPMENT);
        
        // 添加调试日志
        System.out.println("=== 项目统计调试信息 ===");
        System.out.println("项目总数: " + projectCount);
        System.out.println("开发阶段项目数: " + activeProjectCount);
        
        // 查看所有项目的状态分布
        try {
            List<Object[]> statusCounts = projectRepository.countProjectsByStatus();
            System.out.println("项目状态分布:");
            for (Object[] statusCount : statusCounts) {
                System.out.println("  " + statusCount[0] + ": " + statusCount[1]);
            }
        } catch (Exception e) {
            System.out.println("获取项目状态分布失败: " + e.getMessage());
        }
        System.out.println("=== 调试信息结束 ===");
        
        // 待审批统计 - 目前使用工时记录的审批状态
        long pendingApprovalCount = workTimeRecordRepository.countByApproved(false);
        long todayApprovalCount = workTimeRecordRepository.countByApprovedAndDateRange(false, LocalDate.now(), LocalDate.now());
        
        // 预算统计 - 根据用户权限获取
        double totalBudget = 0.0;
        double totalExpenses = 0.0;
        double budgetUtilization = 0.0;
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User currentUser = (User) authentication.getPrincipal();
                
                // 检查用户角色
                boolean hasAdminRole = currentUser.getRoles() != null && currentUser.getRoles().stream()
                        .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || role.getName().contains("管理员"));
                        
                boolean hasFinanceRole = currentUser.getRoles() != null && currentUser.getRoles().stream()
                        .anyMatch(role -> "ROLE_FINANCE".equals(role.getName()) || role.getName().contains("财务"));
                        
                // 检查用户权限（只有 budget:view:all 权限才能查看所有预算）
                boolean hasBudgetViewAllPermission = authentication.getAuthorities().stream()
                        .anyMatch(authority -> {
                            String authName = authority.getAuthority();
                            return "budget:view:all".equals(authName);
                        });
                        
                boolean hasViewAllPermission = hasAdminRole || hasFinanceRole || hasBudgetViewAllPermission;
                
                // 检查是否是项目经理
                boolean isManager = currentUser.getRoles() != null && currentUser.getRoles().stream()
                        .anyMatch(role -> {
                            String roleName = role.getName();
                            return "ROLE_MANAGER".equals(roleName) || 
                                   "MANAGER".equals(roleName) || 
                                   "项目经理".equals(roleName);
                        });
                
                if (hasViewAllPermission) {
                    // 管理员和财务可以查看所有预算统计
                    Double totalBudgetObj = budgetRepository.sumTotalAmount();
                    Double totalExpensesObj = budgetRepository.sumUsedAmount();
                    totalBudget = totalBudgetObj != null ? totalBudgetObj : 0.0;
                    totalExpenses = totalExpensesObj != null ? totalExpensesObj : 0.0;
                } else if (isManager) {
                    // 项目经理只能看到自己管辖项目的预算统计
                    try {
                        List<Project> managedProjects = projectService.getProjectsByManagerId(currentUser.getId());
                        if (managedProjects != null && !managedProjects.isEmpty()) {
                            for (Project project : managedProjects) {
                                Double projectBudget = budgetService.getTotalBudgetAmountByProject(project.getId());
                                Double projectExpenses = budgetService.getTotalUsedAmountByProject(project.getId());
                                totalBudget += projectBudget != null ? projectBudget : 0.0;
                                totalExpenses += projectExpenses != null ? projectExpenses : 0.0;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("获取项目经理预算统计失败: " + e.getMessage());
                        totalBudget = 0.0;
                        totalExpenses = 0.0;
                    }
                }
                // 其他用户无权查看预算统计，保持默认值0
                
                budgetUtilization = totalBudget > 0 ? Math.round(((totalExpenses / totalBudget) * 100) * 100.0) / 100.0 : 0;
            }
        } catch (Exception e) {
            System.out.println("获取预算统计失败: " + e.getMessage());
            totalBudget = 0.0;
            totalExpenses = 0.0;
            budgetUtilization = 0.0;
        }
        
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
        
        // 任务统计 - 获取当前用户的任务统计
        long myTaskCount = 0;
        long pendingTaskCount = 0;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User currentUser = (User) authentication.getPrincipal();
                myTaskCount = taskRepository.countByAssignee(currentUser);
                pendingTaskCount = taskRepository.countPendingTasksByAssignee(currentUser);
            }
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
        stats.put("myTaskCount", myTaskCount);
        stats.put("pendingTaskCount", pendingTaskCount);
        
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
