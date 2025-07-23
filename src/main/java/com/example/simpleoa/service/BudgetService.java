package com.example.simpleoa.service;

import com.example.simpleoa.model.Budget;
import com.example.simpleoa.model.BudgetAlert;
import com.example.simpleoa.model.BudgetExpense;
import com.example.simpleoa.model.BudgetItem;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface BudgetService {
    // 预算基本管理
    Budget createBudget(Budget budget);
    Budget updateBudget(Budget budget);
    void deleteBudget(Long id);
    Budget getBudgetById(Long id);
    List<Budget> getAllBudgets();
    List<Budget> getBudgetsByProject(Long projectId);
    List<Budget> getBudgetsByStatus(String status);
    List<Budget> getBudgetsByDateRange(Date startDate, Date endDate);
    List<Budget> getActiveBudgetsByProjectAndDate(Long projectId, Date date);
    Double getTotalBudgetAmountByProject(Long projectId);
    Double getTotalUsedAmountByProject(Long projectId);
    
    // 预算项目管理
    BudgetItem createBudgetItem(BudgetItem budgetItem);
    BudgetItem updateBudgetItem(BudgetItem budgetItem);
    void deleteBudgetItem(Long id);
    BudgetItem getBudgetItemById(Long id);
    List<BudgetItem> getBudgetItemsByBudget(Long budgetId);
    List<BudgetItem> getBudgetItemsByCategory(String category);
    List<BudgetItem> getBudgetItemsByStatus(String status);
    List<BudgetItem> getBudgetItemsByProject(Long projectId);
    List<BudgetItem> getLowRemainingItems(Long budgetId);
    Double getTotalAmountByBudget(Long budgetId);
    Double getTotalUsedAmountByBudget(Long budgetId);
    
    // 预算支出管理
    BudgetExpense createBudgetExpense(BudgetExpense budgetExpense);
    BudgetExpense updateBudgetExpense(BudgetExpense budgetExpense);
    void deleteBudgetExpense(Long id);
    BudgetExpense getBudgetExpenseById(Long id);
    List<BudgetExpense> getAllBudgetExpenses();
    List<BudgetExpense> getBudgetExpensesByBudget(Long budgetId);
    List<BudgetExpense> getBudgetExpensesByBudgetItem(Long budgetItemId);
    List<BudgetExpense> getBudgetExpensesByExpenseType(String expenseType);
    List<BudgetExpense> getBudgetExpensesByDateRange(Date startDate, Date endDate);
    List<BudgetExpense> getBudgetExpensesByStatus(String status);
    List<BudgetExpense> getBudgetExpensesByProject(Long projectId);
    Double getTotalExpenseAmountByBudget(Long budgetId);
    Double getTotalExpenseAmountByBudgetItem(Long budgetItemId);
    Double getTotalExpenseAmountByProject(Long projectId);
    Double getTotalExpenseAmountByProjectAndDateRange(Long projectId, Date startDate, Date endDate);
    
    // 预算监控
    Map<String, Object> getBudgetMonitorStats(Long budgetId);
    Map<String, Object> getProjectBudgetMonitorStats(Long projectId);
    
    // 预算预警管理
    BudgetAlert createBudgetAlert(BudgetAlert budgetAlert);
    BudgetAlert updateBudgetAlert(BudgetAlert budgetAlert);
    void deleteBudgetAlert(Long id);
    BudgetAlert getBudgetAlertById(Long id);
    List<BudgetAlert> getBudgetAlertsByBudget(Long budgetId);
    List<BudgetAlert> getBudgetAlertsByBudgetItem(Long budgetItemId);
    List<BudgetAlert> getBudgetAlertsByAlertType(String alertType);
    List<BudgetAlert> getBudgetAlertsByAlertLevel(String alertLevel);
    List<BudgetAlert> getBudgetAlertsByStatus(String status);
    List<BudgetAlert> getBudgetAlertsByDateRange(Date startDate, Date endDate);
    List<BudgetAlert> getBudgetAlertsByProject(Long projectId);
    List<BudgetAlert> getUnresolvedAlertsByProject(Long projectId);
    Long countUnresolvedAlertsByBudget(Long budgetId);
    Long countUnresolvedAlertsByProject(Long projectId);
    BudgetAlert resolveBudgetAlert(Long alertId, String resolution, Long resolvedById);
    
    // 预算检查
    boolean checkBudgetAvailability(Long projectId, Double amount);
    boolean checkBudgetItemAvailability(Long budgetItemId, Double amount);
    
    // 预算统计分析
    Map<String, Object> getBudgetStatsByProject(Long projectId);
    Map<String, Object> getBudgetStatsByDateRange(Date startDate, Date endDate);
    Map<String, Object> getBudgetPerformanceStats();
    Map<String, Object> getBudgetTrend(int months);
}
