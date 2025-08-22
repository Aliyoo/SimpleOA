package com.example.simpleoa.service;

import com.example.simpleoa.dto.BudgetSearchDTO;
import com.example.simpleoa.dto.BulkBudgetCreateRequestDTO;
import com.example.simpleoa.dto.PagedResponse;
import com.example.simpleoa.model.Budget;
import com.example.simpleoa.model.BudgetAlert;
import com.example.simpleoa.model.BudgetExpense;
import com.example.simpleoa.model.BudgetItem;
import com.example.simpleoa.model.User;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface BudgetService {
    // 预算基本管理
    Budget createBudget(Budget budget);
    void createBulkBudgets(BulkBudgetCreateRequestDTO request, User currentUser);
    Budget updateBudget(Budget budget);
    void deleteBudget(Long id);
    Budget getBudgetById(Long id);
    List<Budget> getAllBudgets();
    List<Budget> getBudgetsByProject(Long projectId);
    List<Budget> getBudgetsByProjects(List<Long> projectIds);
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
    
    // 报销相关方法
    List<BudgetExpense> getBudgetExpensesByReimbursement(Long reimbursementId);
    List<Budget> getAvailableBudgetsForProject(Long projectId);
    
    // 预算扣减方法（支持幂等性）
    boolean decreaseBudget(Long budgetId, Double amount, String referenceNumber) throws Exception;
    boolean decreaseBudgetItem(Long budgetItemId, Double amount, String referenceNumber) throws Exception;
    boolean isBudgetExpenseExists(String referenceNumber);
    
    // 预算统计分析
    Map<String, Object> getBudgetStatsByProject(Long projectId);
    Map<String, Object> getBudgetStatsByDateRange(Date startDate, Date endDate);
    Map<String, Object> getBudgetPerformanceStats();
    Map<String, Object> getBudgetTrend(int months);
    
    // 新增的统计更新和查询方法
    void updateBudgetAmounts(Long budgetId);
    Double getTotalUsedAmountByBudgetFromExpenses(Long budgetId);
    
    // 新增分页搜索方法
    PagedResponse<Budget> searchBudgets(BudgetSearchDTO searchDTO);
    PagedResponse<Budget> searchBudgetsForManager(BudgetSearchDTO searchDTO, List<Long> projectIds);
}
