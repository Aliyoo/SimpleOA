package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.*;
import com.example.simpleoa.service.BudgetService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
        // 处理项目关联，支持前端只发送projectId的情况
        if (budget.getProject() != null && budget.getProject().getId() != null) {
            Project project = projectRepository.findById(budget.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            budget.setProject(project);
        } else {
            // 如果project为null但前端可能通过其他方式传递了projectId，这里需要额外处理
            throw new IllegalArgumentException("Project is required for budget creation");
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
        return budgetRepository.findAllWithProject();
    }

    @Override
    public List<Budget> getBudgetsByProject(Long projectId) {
        return budgetRepository.findByProjectId(projectId);
    }
    
    @Override
    public List<Budget> getBudgetsByProjects(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return new ArrayList<>();
        }
        return budgetRepository.findByProjectIdIn(projectIds);
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
            // 不再直接累加，而是在操作完成后调用统一更新方法
        }

        if (budgetExpense.getBudgetItem() != null && budgetExpense.getBudgetItem().getId() != null) {
            BudgetItem budgetItem = budgetItemRepository.findById(budgetExpense.getBudgetItem().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Budget item not found"));
            budgetExpense.setBudgetItem(budgetItem);
            // 不再直接累加，而是在操作完成后调用统一更新方法
        }

        if (budgetExpense.getExpenseDate() == null) {
            budgetExpense.setExpenseDate(new Date());
        }
        budgetExpense.setCreateTime(new Date());
        budgetExpense.setLastUpdateTime(new Date());

        BudgetExpense savedExpense = budgetExpenseRepository.save(budgetExpense);
        
        // 在保存支出后，更新预算金额
        if (savedExpense.getBudget() != null) {
            updateBudgetAmounts(savedExpense.getBudget().getId());
        }
        
        return savedExpense;
    }

    @Override
    @Transactional
    public BudgetExpense updateBudgetExpense(BudgetExpense budgetExpense) {
        BudgetExpense existingExpense = budgetExpenseRepository.findById(budgetExpense.getId())
                .orElseThrow(() -> new EntityNotFoundException("Budget expense not found"));

        // 处理预算关联变更
        Budget oldBudget = existingExpense.getBudget();
        Budget newBudget = null;
        
        if (budgetExpense.getBudget() != null && budgetExpense.getBudget().getId() != null) {
            newBudget = budgetRepository.findById(budgetExpense.getBudget().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
        }
        
        // 从旧预算中减去支出金额
        if (oldBudget != null) {
            oldBudget.setUsedAmount(oldBudget.getUsedAmount() - existingExpense.getAmount());
            oldBudget.setRemainingAmount(oldBudget.getRemainingAmount() + existingExpense.getAmount());
            oldBudget.setLastUpdateTime(new Date());
            budgetRepository.save(oldBudget);
        }
        
        // 向新预算中添加支出金额
        if (newBudget != null) {
            newBudget.setUsedAmount(newBudget.getUsedAmount() + budgetExpense.getAmount());
            newBudget.setRemainingAmount(newBudget.getRemainingAmount() - budgetExpense.getAmount());
            newBudget.setLastUpdateTime(new Date());
            budgetRepository.save(newBudget);
            existingExpense.setBudget(newBudget);
        }

        // 处理预算项目关联变更
        BudgetItem oldBudgetItem = existingExpense.getBudgetItem();
        BudgetItem newBudgetItem = null;
        
        if (budgetExpense.getBudgetItem() != null && budgetExpense.getBudgetItem().getId() != null) {
            newBudgetItem = budgetItemRepository.findById(budgetExpense.getBudgetItem().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Budget item not found"));
        }
        
        // 从旧预算项目中减去支出金额
        if (oldBudgetItem != null) {
            oldBudgetItem.setUsedAmount(oldBudgetItem.getUsedAmount() - existingExpense.getAmount());
            oldBudgetItem.setRemainingAmount(oldBudgetItem.getRemainingAmount() + existingExpense.getAmount());
            oldBudgetItem.setLastUpdateTime(new Date());
            budgetItemRepository.save(oldBudgetItem);
        }
        
        // 向新预算项目中添加支出金额
        if (newBudgetItem != null) {
            newBudgetItem.setUsedAmount(newBudgetItem.getUsedAmount() + budgetExpense.getAmount());
            newBudgetItem.setRemainingAmount(newBudgetItem.getRemainingAmount() - budgetExpense.getAmount());
            newBudgetItem.setLastUpdateTime(new Date());
            budgetItemRepository.save(newBudgetItem);
            existingExpense.setBudgetItem(newBudgetItem);
        }
        
        // 处理记录人关联
        if (budgetExpense.getRecordedBy() != null && budgetExpense.getRecordedBy().getId() != null) {
            User recordedBy = userRepository.findById(budgetExpense.getRecordedBy().getId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            existingExpense.setRecordedBy(recordedBy);
        }

        // 更新基本字段
        existingExpense.setAmount(budgetExpense.getAmount());
        existingExpense.setExpenseType(budgetExpense.getExpenseType());
        existingExpense.setDescription(budgetExpense.getDescription());
        existingExpense.setExpenseDate(budgetExpense.getExpenseDate());
        existingExpense.setStatus(budgetExpense.getStatus());
        existingExpense.setReferenceNumber(budgetExpense.getReferenceNumber());
        existingExpense.setRecordTime(budgetExpense.getRecordTime());
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
    public List<BudgetExpense> getAllBudgetExpenses() {
        // 获取所有支出记录，确保关联对象被正确加载
        List<BudgetExpense> expenses = budgetExpenseRepository.findAll();
        // 为了确保前端能获取到完整的关联对象数据，手动初始化懒加载关系
        expenses.forEach(expense -> {
            if (expense.getBudget() != null) {
                // 初始化预算和项目信息
                expense.getBudget().getName(); // 触发加载
                if (expense.getBudget().getProject() != null) {
                    expense.getBudget().getProject().getName(); // 触发项目加载
                }
            }
            if (expense.getBudgetItem() != null) {
                expense.getBudgetItem().getName(); // 触发预算项目加载
            }
            if (expense.getRecordedBy() != null) {
                expense.getRecordedBy().getRealName(); // 触发用户加载
            }
        });
        return expenses;
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
        return budgetExpenseRepository.findByProjectId(projectId);
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
        Double total = budgetExpenseRepository.sumAmountByProjectId(projectId);
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalExpenseAmountByProjectAndDateRange(Long projectId, Date startDate, Date endDate) {
        Double total = budgetExpenseRepository.sumAmountByProjectIdAndExpenseDateBetween(projectId, startDate, endDate);
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
        stats.put("budgetExpenseCount", budgetExpenseRepository.countByProjectId(projectId));
        stats.put("unresolvedAlertCount", budgetAlertRepository.countByBudget_ProjectIdAndStatus(projectId, "未解决"));

        return stats;
    }
    
    @Override
    @Transactional
    public synchronized boolean decreaseBudget(Long budgetId, Double amount, String referenceNumber) throws Exception {
        // 检查幂等性 - 如果该引用号已存在，说明已经扣减过了
        if (isBudgetExpenseExists(referenceNumber)) {
            // 幂等操作，返回成功
            return true;
        }
        
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
        
        // 检查预算余额是否充足
        if (budget.getRemainingAmount() < amount) {
            throw new IllegalStateException("预算余额不足，无法扣减");
        }
        
        // 使用悲观锁防止并发问题
        Budget lockedBudget = budgetRepository.findByIdForUpdate(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found for update"));
        
        // 再次检查余额（双重检查）
        if (lockedBudget.getRemainingAmount() < amount) {
            throw new IllegalStateException("预算余额不足，无法扣减");
        }
        
        // 更新预算使用金额和剩余金额
        lockedBudget.setUsedAmount(lockedBudget.getUsedAmount() + amount);
        lockedBudget.setRemainingAmount(lockedBudget.getRemainingAmount() - amount);
        lockedBudget.setLastUpdateTime(new Date());
        budgetRepository.save(lockedBudget);
        
        // 检查是否需要创建预算预警
        double usageRate = lockedBudget.getUsedAmount() / lockedBudget.getTotalAmount();
        if (usageRate >= 0.8 && usageRate < 1.0) {
            createBudgetUsageAlert(lockedBudget, "高", String.format("预算使用率达到%.1f%%", usageRate * 100));
        } else if (usageRate >= 1.0) {
            createBudgetUsageAlert(lockedBudget, "紧急", "预算已超支");
        }
        
        return true;
    }
    
    @Override
    @Transactional
    public synchronized boolean decreaseBudgetItem(Long budgetItemId, Double amount, String referenceNumber) throws Exception {
        // 检查幂等性
        if (isBudgetExpenseExists(referenceNumber)) {
            return true;
        }
        
        BudgetItem budgetItem = budgetItemRepository.findById(budgetItemId)
                .orElseThrow(() -> new EntityNotFoundException("Budget item not found"));
        
        // 检查预算项余额是否充足
        if (budgetItem.getRemainingAmount() < amount) {
            throw new IllegalStateException("预算项余额不足，无法扣减");
        }
        
        // 使用悲观锁防止并发问题
        BudgetItem lockedBudgetItem = budgetItemRepository.findByIdForUpdate(budgetItemId)
                .orElseThrow(() -> new EntityNotFoundException("Budget item not found for update"));
        
        // 再次检查余额
        if (lockedBudgetItem.getRemainingAmount() < amount) {
            throw new IllegalStateException("预算项余额不足，无法扣减");
        }
        
        // 更新预算项使用金额和剩余金额
        lockedBudgetItem.setUsedAmount(lockedBudgetItem.getUsedAmount() + amount);
        lockedBudgetItem.setRemainingAmount(lockedBudgetItem.getRemainingAmount() - amount);
        lockedBudgetItem.setLastUpdateTime(new Date());
        budgetItemRepository.save(lockedBudgetItem);
        
        // 同时需要更新对应的总预算
        if (lockedBudgetItem.getBudget() != null) {
            decreaseBudget(lockedBudgetItem.getBudget().getId(), amount, referenceNumber + "-BUDGET");
        }
        
        // 检查是否需要创建预算预警
        double usageRate = lockedBudgetItem.getUsedAmount() / lockedBudgetItem.getAmount();
        if (usageRate >= 0.8 && usageRate < 1.0) {
            createBudgetItemUsageAlert(lockedBudgetItem, "高", String.format("预算项使用率达到%.1f%%", usageRate * 100));
        } else if (usageRate >= 1.0) {
            createBudgetItemUsageAlert(lockedBudgetItem, "紧急", "预算项已超支");
        }
        
        return true;
    }
    
    @Override
    public boolean isBudgetExpenseExists(String referenceNumber) {
        return budgetExpenseRepository.existsByReferenceNumber(referenceNumber);
    }
    
    private void createBudgetUsageAlert(Budget budget, String alertLevel, String alertContent) {
        BudgetAlert alert = new BudgetAlert();
        alert.setBudget(budget);
        alert.setAlertType("预算使用");
        alert.setAlertLevel(alertLevel);
        alert.setMessage(alertContent);
        alert.setAlertDate(new Date());
        alert.setStatus("未解决");
        alert.setCreateTime(new Date());
        alert.setLastUpdateTime(new Date());
        budgetAlertRepository.save(alert);
    }
    
    private void createBudgetItemUsageAlert(BudgetItem budgetItem, String alertLevel, String alertContent) {
        BudgetAlert alert = new BudgetAlert();
        alert.setBudget(budgetItem.getBudget());
        alert.setBudgetItem(budgetItem);
        alert.setAlertType("预算项使用");
        alert.setAlertLevel(alertLevel);
        alert.setMessage(alertContent);
        alert.setAlertDate(new Date());
        alert.setStatus("未解决");
        alert.setCreateTime(new Date());
        alert.setLastUpdateTime(new Date());
        budgetAlertRepository.save(alert);
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

    @Override
    public List<BudgetExpense> getBudgetExpensesByReimbursement(Long reimbursementId) {
        return budgetExpenseRepository.findByReimbursementRequestId(reimbursementId);
    }

    @Override
    public List<Budget> getAvailableBudgetsForProject(Long projectId) {
        return budgetRepository.findAvailableBudgetsByProject(projectId);
    }
    
    /**
     * 更新预算的已使用金额和剩余金额
     * 从 BudgetExpense 表重新计算，确保数据准确性和幂等性
     */
    @Override
    @Transactional
    public void updateBudgetAmounts(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found"));
        
        // 从 BudgetExpense 重新计算已使用金额
        Double totalUsedAmount = budgetExpenseRepository.sumAmountByBudgetId(budgetId);
        if (totalUsedAmount == null) {
            totalUsedAmount = 0.0;
        }
        
        // 更新预算使用金额和剩余金额
        budget.setUsedAmount(totalUsedAmount);
        budget.setRemainingAmount(budget.getTotalAmount() - totalUsedAmount);
        budget.setLastUpdateTime(new Date());
        
        budgetRepository.save(budget);
    }
    
    /**
     * 从支出记录获取预算的总使用金额
     */
    @Override
    public Double getTotalUsedAmountByBudgetFromExpenses(Long budgetId) {
        Double total = budgetExpenseRepository.sumAmountByBudgetId(budgetId);
        return total != null ? total : 0.0;
    }
}
