package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.*;
import com.example.simpleoa.service.BudgetService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final BudgetExpenseRepository budgetExpenseRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Autowired
    public BudgetServiceImpl(
            BudgetRepository budgetRepository,
            BudgetItemRepository budgetItemRepository,
            BudgetExpenseRepository budgetExpenseRepository,
            BudgetAlertRepository budgetAlertRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.budgetItemRepository = budgetItemRepository;
        this.budgetExpenseRepository = budgetExpenseRepository;
        this.budgetAlertRepository = budgetAlertRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // 预算基本管理
    @Override
    @Transactional
    public Budget createBudget(Budget budget) {
        if (budget.getProject() != null && budget.getProject().getId() != null) {
            Project project = projectRepository.findById(budget.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            budget.setProject(project);
        }
        
        if (budget.getUsedAmount() == null) {
            budget.setUsedAmount(0.0);
        }
        
        if (budget.getRemainingAmount() == null) {
            budget.setRemainingAmount(budget.getTotalAmount());
        }
        
        if (budget.getStatus() == null) {
            budget.setStatus("活跃");
        }
        
        if (budget.getCreateTime() == null) {
            budget.setCreateTime(new Date());
        }
        
        budget.setLastUpdateTime(new Date());
        
        return budgetRepository.save(budget);
    }

    @Override
    @Transactional
    public Budget updateBudget(Budget budget) {
        Budget existingBudget = budgetRepository.findById(budget.getId())
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
        
        if (budget.getProject() != null && budget.getProject().getId() != null) {
            Project project = projectRepository.findById(budget.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            existingBudget.setProject(project);
        }
        
        existingBudget.setName(budget.getName());
        existingBudget.setTotalAmount(budget.getTotalAmount());
        existingBudget.setStartDate(budget.getStartDate());
        existingBudget.setEndDate(budget.getEndDate());
        existingBudget.setStatus(budget.getStatus());
        existingBudget.setDescription(budget.getDescription());
        
        // 重新计算剩余金额
        if (existingBudget.getUsedAmount() != null) {
            existingBudget.setRemainingAmount(existingBudget.getTotalAmount() - existingBudget.getUsedAmount());
        } else {
            existingBudget.setRemainingAmount(existingBudget.getTotalAmount());
        }
        
        existingBudget.setLastUpdateTime(new Date());
        
        return budgetRepository.save(existingBudget);
    }

    @Override
    @Transactional
    public void deleteBudget(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
        
        // 删除相关的预算项目
        List<BudgetItem> budgetItems = budgetItemRepository.findByBudgetId(id);
        budgetItemRepository.deleteAll(budgetItems);
        
        // 删除相关的预算支出
        List<BudgetExpense> budgetExpenses = budgetExpenseRepository.findByBudgetId(id);
        budgetExpenseRepository.deleteAll(budgetExpenses);
        
        // 删除相关的预算预警
        List<BudgetAlert> budgetAlerts = budgetAlertRepository.findByBudgetId(id);
        budgetAlertRepository.deleteAll(budgetAlerts);
        
        // 删除预算
        budgetRepository.delete(budget);
    }

    @Override
    public Budget getBudgetById(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
    }

    @Override
    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    @Override
    public List<Budget> getBudgetsByProject(Long projectId) {
        return budgetRepository.findByProjectId(projectId);
    }

    @Override
    public List<Budget> getBudgetsByStatus(String status) {
        return budgetRepository.findByStatus(status);
    }

    @Override
    public List<Budget> getBudgetsByDateRange(Date startDate, Date endDate) {
        return budgetRepository.findByStartDateBetweenOrEndDateBetween(startDate, endDate, startDate, endDate);
    }

    @Override
    public List<Budget> getActiveBudgetsByProjectAndDate(Long projectId, Date date) {
        return budgetRepository.findActiveBudgetsByProjectAndDate(projectId, date);
    }

    @Override
    public Double getTotalBudgetAmountByProject(Long projectId) {
        Double amount = budgetRepository.getTotalBudgetAmountByProject(projectId);
        return amount != null ? amount : 0.0;
    }

    @Override
    public Double getTotalUsedAmountByProject(Long projectId) {
        Double amount = budgetRepository.getTotalUsedAmountByProject(projectId);
        return amount != null ? amount : 0.0;
    }

    // 预算项目管理
    @Override
    @Transactional
    public BudgetItem createBudgetItem(BudgetItem budgetItem) {
        if (budgetItem.getBudget() != null && budgetItem.getBudget().getId() != null) {
            Budget budget = budgetRepository.findById(budgetItem.getBudget().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
            budgetItem.setBudget(budget);
        }
        
        if (budgetItem.getUsedAmount() == null) {
            budgetItem.setUsedAmount(0.0);
        }
        
        if (budgetItem.getRemainingAmount() == null) {
            budgetItem.setRemainingAmount(budgetItem.getAmount());
        }
        
        if (budgetItem.getStatus() == null) {
            budgetItem.setStatus("活跃");
        }
        
        budgetItem.setLastUpdateTime(new Date());
        
        return budgetItemRepository.save(budgetItem);
    }

    @Override
    @Transactional
    public BudgetItem updateBudgetItem(BudgetItem budgetItem) {
        BudgetItem existingBudgetItem = budgetItemRepository.findById(budgetItem.getId())
                .orElseThrow(() -> new EntityNotFoundException("Budget item not found"));
        
        existingBudgetItem.setName(budgetItem.getName());
        existingBudgetItem.setCategory(budgetItem.getCategory());
        existingBudgetItem.setAmount(budgetItem.getAmount());
        existingBudgetItem.setStatus(budgetItem.getStatus());
        existingBudgetItem.setDescription(budgetItem.getDescription());
        
        // 重新计算剩余金额
        if (existingBudgetItem.getUsedAmount() != null) {
            existingBudgetItem.setRemainingAmount(existingBudgetItem.getAmount() - existingBudgetItem.getUsedAmount());
        } else {
            existingBudgetItem.setRemainingAmount(existingBudgetItem.getAmount());
        }
        
        existingBudgetItem.setLastUpdateTime(new Date());
        
        return budgetItemRepository.save(existingBudgetItem);
    }

    @Override
    @Transactional
    public void deleteBudgetItem(Long id) {
        BudgetItem budgetItem = budgetItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget item not found"));
        
        // 删除相关的预算支出
        List<BudgetExpense> budgetExpenses = budgetExpenseRepository.findByBudgetItemId(id);
        budgetExpenseRepository.deleteAll(budgetExpenses);
        
        // 删除相关的预算预警
        List<BudgetAlert> budgetAlerts = budgetAlertRepository.findByBudgetItemId(id);
        budgetAlertRepository.deleteAll(budgetAlerts);
        
        // 删除预算项目
        budgetItemRepository.delete(budgetItem);
    }

    @Override
    public BudgetItem getBudgetItemById(Long id) {
        return budgetItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget item not found"));
    }

    @Override
    public List<BudgetItem> getBudgetItemsByBudget(Long budgetId) {
        return budgetItemRepository.findByBudgetId(budgetId);
    }

    @Override
    public List<BudgetItem> getBudgetItemsByCategory(String category) {
        return budgetItemRepository.findByCategory(category);
    }

    @Override
    public List<BudgetItem> getBudgetItemsByStatus(String status) {
        return budgetItemRepository.findByStatus(status);
    }

    @Override
    public List<BudgetItem> getBudgetItemsByProject(Long projectId) {
        return budgetItemRepository.findByProjectId(projectId);
    }

    @Override
    public List<BudgetItem> getLowRemainingItems(Long budgetId) {
        return budgetItemRepository.findLowRemainingItems(budgetId);
    }

    @Override
    public Double getTotalAmountByBudget(Long budgetId) {
        Double amount = budgetItemRepository.getTotalAmountByBudget(budgetId);
        return amount != null ? amount : 0.0;
    }

    @Override
    public Double getTotalUsedAmountByBudget(Long budgetId) {
        Double amount = budgetItemRepository.getTotalUsedAmountByBudget(budgetId);
        return amount != null ? amount : 0.0;
    }

    @Override
    @Transactional
    public BudgetExpense createBudgetExpense(BudgetExpense budgetExpense) {
        if (budgetExpense.getBudget() != null && budgetExpense.getBudget().getId() != null) {
            Budget budget = budgetRepository.findById(budgetExpense.getBudget().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
            budgetExpense.setBudget(budget);
            budget.setUsedAmount(budget.getUsedAmount() + budgetExpense.getAmount());
            budget.setRemainingAmount(budget.getRemainingAmount() - budgetExpense.getAmount());
            budget.setLastUpdateTime(new Date());
            budgetRepository.save(budget);
        }

        if (budgetExpense.getBudgetItem() != null && budgetExpense.getBudgetItem().getId() != null) {
            BudgetItem budgetItem = budgetItemRepository.findById(budgetExpense.getBudgetItem().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Budget item not found"));
            budgetItem.setUsedAmount(budgetItem.getUsedAmount() + budgetExpense.getAmount());
            budgetItem.setRemainingAmount(budgetItem.getRemainingAmount() - budgetExpense.getAmount());
            budgetItem.setLastUpdateTime(new Date());
            budgetItemRepository.save(budgetItem);
            budgetExpense.setBudgetItem(budgetItem);
        }

        if (budgetExpense.getExpenseDate() == null) {
            budgetExpense.setExpenseDate(new Date());
        }
        budgetExpense.setCreateTime(new Date());
        budgetExpense.setLastUpdateTime(new Date());

        return budgetExpenseRepository.save(budgetExpense);
    }

    @Override
    @Transactional
    public BudgetExpense updateBudgetExpense(BudgetExpense budgetExpense) {
        BudgetExpense existingExpense = budgetExpenseRepository.findById(budgetExpense.getId())
                .orElseThrow(() -> new EntityNotFoundException("Budget expense not found"));

        // Update associated Budget's used and remaining amounts
        if (existingExpense.getBudget() != null) {
            Budget budget = existingExpense.getBudget();
            budget.setUsedAmount(budget.getUsedAmount() - existingExpense.getAmount() + budgetExpense.getAmount());
            budget.setRemainingAmount(budget.getRemainingAmount() + existingExpense.getAmount() - budgetExpense.getAmount());
            budget.setLastUpdateTime(new Date());
            budgetRepository.save(budget);
        }

        // Update associated BudgetItem's used and remaining amounts
        if (existingExpense.getBudgetItem() != null) {
            BudgetItem budgetItem = existingExpense.getBudgetItem();
            budgetItem.setUsedAmount(budgetItem.getUsedAmount() - existingExpense.getAmount() + budgetExpense.getAmount());
            budgetItem.setRemainingAmount(budgetItem.getRemainingAmount() + existingExpense.getAmount() - budgetExpense.getAmount());
            budgetItem.setLastUpdateTime(new Date());
            budgetItemRepository.save(budgetItem);
        }

        existingExpense.setAmount(budgetExpense.getAmount());
        existingExpense.setExpenseType(budgetExpense.getExpenseType());
        existingExpense.setDescription(budgetExpense.getDescription());
        existingExpense.setExpenseDate(budgetExpense.getExpenseDate());
        existingExpense.setStatus(budgetExpense.getStatus());
        existingExpense.setLastUpdateTime(new Date());

        return budgetExpenseRepository.save(existingExpense);
    }

    @Override
    @Transactional
    public void deleteBudgetExpense(Long id) {
        BudgetExpense budgetExpense = budgetExpenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget expense not found"));

        // Update associated Budget's used and remaining amounts
        if (budgetExpense.getBudget() != null) {
            Budget budget = budgetExpense.getBudget();
            budget.setUsedAmount(budget.getUsedAmount() - budgetExpense.getAmount());
            budget.setRemainingAmount(budget.getRemainingAmount() + budgetExpense.getAmount());
            budget.setLastUpdateTime(new Date());
            budgetRepository.save(budget);
        }

        // Update associated BudgetItem's used and remaining amounts
        if (budgetExpense.getBudgetItem() != null) {
            BudgetItem budgetItem = budgetExpense.getBudgetItem();
            budgetItem.setUsedAmount(budgetItem.getUsedAmount() - budgetExpense.getAmount());
            budgetItem.setRemainingAmount(budgetItem.getRemainingAmount() + budgetExpense.getAmount());
            budgetItem.setLastUpdateTime(new Date());
            budgetItemRepository.save(budgetItem);
        }

        budgetExpenseRepository.delete(budgetExpense);
    }

    @Override
    public BudgetExpense getBudgetExpenseById(Long id) {
        return budgetExpenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget expense not found"));
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByBudget(Long budgetId) {
        return budgetExpenseRepository.findByBudgetId(budgetId);
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByBudgetItem(Long budgetItemId) {
        return budgetExpenseRepository.findByBudgetItemId(budgetItemId);
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByExpenseType(String expenseType) {
        return budgetExpenseRepository.findByExpenseType(expenseType);
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByDateRange(Date startDate, Date endDate) {
        return budgetExpenseRepository.findByExpenseDateBetween(startDate, endDate);
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByStatus(String status) {
        return budgetExpenseRepository.findByStatus(status);
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByProject(Long projectId) {
        return budgetExpenseRepository.findByBudget_ProjectId(projectId);
    }

    @Override
    public Double getTotalExpenseAmountByBudget(Long budgetId) {
        Double total = budgetExpenseRepository.sumAmountByBudgetId(budgetId);
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalExpenseAmountByBudgetItem(Long budgetItemId) {
        Double total = budgetExpenseRepository.sumAmountByBudgetItemId(budgetItemId);
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalExpenseAmountByProject(Long projectId) {
        Double total = budgetExpenseRepository.sumAmountByBudget_ProjectId(projectId);
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalExpenseAmountByProjectAndDateRange(Long projectId, Date startDate, Date endDate) {
        Double total = budgetExpenseRepository.sumAmountByBudget_ProjectIdAndExpenseDateBetween(projectId, startDate, endDate);
        return total != null ? total : 0.0;
    }

    @Override
    public Map<String, Object> getBudgetMonitorStats(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalAmount", budget.getTotalAmount());
        stats.put("usedAmount", budget.getUsedAmount());
        stats.put("remainingAmount", budget.getRemainingAmount());
        stats.put("status", budget.getStatus());
        stats.put("budgetItemCount", budgetItemRepository.countByBudgetId(budgetId));
        stats.put("budgetExpenseCount", budgetExpenseRepository.countByBudgetId(budgetId));
        stats.put("unresolvedAlertCount", budgetAlertRepository.countByBudgetIdAndStatus(budgetId, "未解决"));

        return stats;
    }

    @Override
    public Map<String, Object> getProjectBudgetMonitorStats(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("projectId", projectId);
        stats.put("projectName", project.getName());
        stats.put("totalBudgetAmount", getTotalBudgetAmountByProject(projectId));
        stats.put("totalUsedAmount", getTotalUsedAmountByProject(projectId));
        stats.put("totalExpenseAmount", getTotalExpenseAmountByProject(projectId));
        stats.put("budgetCount", budgetRepository.countByProjectId(projectId));
        stats.put("budgetItemCount", budgetItemRepository.countByProjectId(projectId));
        stats.put("budgetExpenseCount", budgetExpenseRepository.countByBudget_ProjectId(projectId));
        stats.put("unresolvedAlertCount", budgetAlertRepository.countByBudget_ProjectIdAndStatus(projectId, "未解决"));

        return stats;
    }

    @Override
    @Transactional
    public BudgetAlert createBudgetAlert(BudgetAlert budgetAlert) {
        if (budgetAlert.getBudget() != null && budgetAlert.getBudget().getId() != null) {
            Budget budget = budgetRepository.findById(budgetAlert.getBudget().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
            budgetAlert.setBudget(budget);
        }

        if (budgetAlert.getBudgetItem() != null && budgetAlert.getBudgetItem().getId() != null) {
            BudgetItem budgetItem = budgetItemRepository.findById(budgetAlert.getBudgetItem().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Budget item not found"));
            budgetAlert.setBudgetItem(budgetItem);
        }

        if (budgetAlert.getAlertDate() == null) {
            budgetAlert.setAlertDate(new Date());
        }
        if (budgetAlert.getStatus() == null) {
            budgetAlert.setStatus("未解决");
        }
        budgetAlert.setCreateTime(new Date());
        budgetAlert.setLastUpdateTime(new Date());

        return budgetAlertRepository.save(budgetAlert);
    }

    @Override
    @Transactional
    public BudgetAlert updateBudgetAlert(BudgetAlert budgetAlert) {
        BudgetAlert existingAlert = budgetAlertRepository.findById(budgetAlert.getId())
                .orElseThrow(() -> new EntityNotFoundException("Budget alert not found"));

        existingAlert.setAlertType(budgetAlert.getAlertType());
        existingAlert.setAlertLevel(budgetAlert.getAlertLevel());
        existingAlert.setMessage(budgetAlert.getMessage());
        existingAlert.setAlertDate(budgetAlert.getAlertDate());
        existingAlert.setStatus(budgetAlert.getStatus());
        existingAlert.setLastUpdateTime(new Date());

        return budgetAlertRepository.save(existingAlert);
    }

    @Override
    @Transactional
    public void deleteBudgetAlert(Long id) {
        BudgetAlert budgetAlert = budgetAlertRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget alert not found"));
        budgetAlertRepository.delete(budgetAlert);
    }

    @Override
    public BudgetAlert getBudgetAlertById(Long id) {
        return budgetAlertRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Budget alert not found"));
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByBudget(Long budgetId) {
        return budgetAlertRepository.findByBudgetId(budgetId);
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByBudgetItem(Long budgetItemId) {
        return budgetAlertRepository.findByBudgetItemId(budgetItemId);
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByAlertType(String alertType) {
        return budgetAlertRepository.findByAlertType(alertType);
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByAlertLevel(String alertLevel) {
        return budgetAlertRepository.findByAlertLevel(alertLevel);
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByStatus(String status) {
        return budgetAlertRepository.findByStatus(status);
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByDateRange(Date startDate, Date endDate) {
        return budgetAlertRepository.findByAlertDateBetween(startDate, endDate);
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByProject(Long projectId) {
        return budgetAlertRepository.findByBudget_ProjectId(projectId);
    }

    @Override
    public List<BudgetAlert> getUnresolvedAlertsByProject(Long projectId) {
        return budgetAlertRepository.findByBudget_ProjectIdAndStatus(projectId, "未解决");
    }

    @Override
    public Long countUnresolvedAlertsByBudget(Long budgetId) {
        return budgetAlertRepository.countByBudgetIdAndStatus(budgetId, "未解决");
    }

    @Override
    public Long countUnresolvedAlertsByProject(Long projectId) {
        return budgetAlertRepository.countByBudget_ProjectIdAndStatus(projectId, "未解决");
    }

    @Override
    @Transactional
    public BudgetAlert resolveBudgetAlert(Long alertId, String resolution, Long resolvedById) {
        BudgetAlert alert = budgetAlertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Budget alert not found"));
        alert.setStatus("已解决");
        alert.setResolution(resolution);
        alert.setResolvedBy(userRepository.findById(resolvedById).orElseThrow(() -> new EntityNotFoundException("User not found")));
        alert.setResolvedDate(new Date());
        alert.setLastUpdateTime(new Date());
        return budgetAlertRepository.save(alert);
    }

    @Override
    public boolean checkBudgetAvailability(Long projectId, Double amount) {
        Double totalRemaining = budgetRepository.getTotalRemainingAmountByProject(projectId);
        return totalRemaining != null && totalRemaining >= amount;
    }

    @Override
    public boolean checkBudgetItemAvailability(Long budgetItemId, Double amount) {
        BudgetItem budgetItem = budgetItemRepository.findById(budgetItemId)
                .orElseThrow(() -> new EntityNotFoundException("Budget item not found"));
        return budgetItem.getRemainingAmount() >= amount;
    }

    @Override
    public Map<String, Object> getBudgetStatsByProject(Long projectId) {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalBudgetAmount", getTotalBudgetAmountByProject(projectId));
        stats.put("totalUsedAmount", getTotalUsedAmountByProject(projectId));
        stats.put("totalExpenseAmount", getTotalExpenseAmountByProject(projectId));
        stats.put("unresolvedAlertCount", countUnresolvedAlertsByProject(projectId));
        return stats;
    }

    @Override
    public Map<String, Object> getBudgetStatsByDateRange(Date startDate, Date endDate) {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalBudgetAmount", budgetRepository.getTotalBudgetAmountByDateRange(startDate, endDate));
        stats.put("totalUsedAmount", budgetRepository.getTotalUsedAmountByDateRange(startDate, endDate));
        stats.put("totalExpenseAmount", budgetExpenseRepository.sumAmountByExpenseDateBetween(startDate, endDate));
        stats.put("totalAlertCount", budgetAlertRepository.countByAlertDateBetween(startDate, endDate));
        return stats;
    }

    @Override
    public Map<String, Object> getBudgetPerformanceStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalBudgetAmount", budgetRepository.sumTotalAmount());
        stats.put("totalUsedAmount", budgetRepository.sumUsedAmount());
        stats.put("totalRemainingAmount", budgetRepository.sumRemainingAmount());
        stats.put("totalExpenseAmount", budgetExpenseRepository.sumTotalAmount());
        stats.put("totalAlertCount", budgetAlertRepository.count());
        stats.put("unresolvedAlertCount", budgetAlertRepository.countByStatus("未解决"));
        return stats;
    }

    @Override
    public Map<String, Object> getBudgetTrend(int months) {
        Map<String, Object> trendData = new java.util.HashMap<>();
        // This is a simplified implementation. A real-world scenario would involve more complex aggregation queries.
        // For example, grouping by month and summing amounts.
        // Here, we'll just return some dummy data or aggregate based on existing methods if possible.
        trendData.put("message", "Budget trend data for " + months + " months is not fully implemented yet. This is a placeholder.");
        // Example: You might fetch all budgets and expenses and then aggregate them by month in Java
        // Or, ideally, use native queries or more advanced JPA features for monthly aggregation.
        return trendData;
    }
}
