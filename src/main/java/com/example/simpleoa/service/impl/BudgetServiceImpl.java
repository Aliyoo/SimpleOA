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
    public BudgetExpense createBudgetExpense(BudgetExpense budgetExpense) {
        return null;
    }

    @Override
    public BudgetExpense updateBudgetExpense(BudgetExpense budgetExpense) {
        return null;
    }

    @Override
    public void deleteBudgetExpense(Long id) {

    }

    @Override
    public BudgetExpense getBudgetExpenseById(Long id) {
        return null;
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByBudget(Long budgetId) {
        return List.of();
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByBudgetItem(Long budgetItemId) {
        return List.of();
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByExpenseType(String expenseType) {
        return List.of();
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByDateRange(Date startDate, Date endDate) {
        return List.of();
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByStatus(String status) {
        return List.of();
    }

    @Override
    public List<BudgetExpense> getBudgetExpensesByProject(Long projectId) {
        return List.of();
    }

    @Override
    public Double getTotalExpenseAmountByBudget(Long budgetId) {
        return 0.0;
    }

    @Override
    public Double getTotalExpenseAmountByBudgetItem(Long budgetItemId) {
        return 0.0;
    }

    @Override
    public Double getTotalExpenseAmountByProject(Long projectId) {
        return 0.0;
    }

    @Override
    public Double getTotalExpenseAmountByProjectAndDateRange(Long projectId, Date startDate, Date endDate) {
        return 0.0;
    }

    @Override
    public Map<String, Object> getBudgetMonitorStats(Long budgetId) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getProjectBudgetMonitorStats(Long projectId) {
        return Map.of();
    }

    @Override
    public BudgetAlert createBudgetAlert(BudgetAlert budgetAlert) {
        return null;
    }

    @Override
    public BudgetAlert updateBudgetAlert(BudgetAlert budgetAlert) {
        return null;
    }

    @Override
    public void deleteBudgetAlert(Long id) {

    }

    @Override
    public BudgetAlert getBudgetAlertById(Long id) {
        return null;
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByBudget(Long budgetId) {
        return List.of();
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByBudgetItem(Long budgetItemId) {
        return List.of();
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByAlertType(String alertType) {
        return List.of();
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByAlertLevel(String alertLevel) {
        return List.of();
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByStatus(String status) {
        return List.of();
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByDateRange(Date startDate, Date endDate) {
        return List.of();
    }

    @Override
    public List<BudgetAlert> getBudgetAlertsByProject(Long projectId) {
        return List.of();
    }

    @Override
    public List<BudgetAlert> getUnresolvedAlertsByProject(Long projectId) {
        return List.of();
    }

    @Override
    public Long countUnresolvedAlertsByBudget(Long budgetId) {
        return 0L;
    }

    @Override
    public Long countUnresolvedAlertsByProject(Long projectId) {
        return 0L;
    }

    @Override
    public BudgetAlert resolveBudgetAlert(Long alertId, String resolution, Long resolvedById) {
        return null;
    }

    @Override
    public boolean checkBudgetAvailability(Long projectId, Double amount) {
        return false;
    }

    @Override
    public boolean checkBudgetItemAvailability(Long budgetItemId, Double amount) {
        return false;
    }

    @Override
    public Map<String, Object> getBudgetStatsByProject(Long projectId) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getBudgetStatsByDateRange(Date startDate, Date endDate) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getBudgetPerformanceStats() {
        return Map.of();
    }

    @Override
    public Map<String, Object> getBudgetTrend(int months) {
        return Map.of();
    }
}
