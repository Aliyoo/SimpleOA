package com.example.simpleoa.repository;

import com.example.simpleoa.model.BudgetExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BudgetExpenseRepository extends JpaRepository<BudgetExpense, Long> {
    
    // ======= 预算级查询接口 (Budget Level) =======
    List<BudgetExpense> findByBudgetId(Long budgetId);
    List<BudgetExpense> findByBudgetIdAndStatus(Long budgetId, String status);
    
    // ======= 预算项级查询接口 (Budget Item Level) =======
    List<BudgetExpense> findByBudgetItemId(Long budgetItemId);
    
    // ======= 其他基础查询接口 =======
    List<BudgetExpense> findByExpenseType(String expenseType);
    List<BudgetExpense> findByExpenseDateBetween(Date startDate, Date endDate);
    List<BudgetExpense> findByStatus(String status);
    
    // ======= 项目级查询接口 (Project Level) =======
    @Query("SELECT be FROM BudgetExpense be WHERE be.budget.project.id = ?1")
    List<BudgetExpense> findByProjectId(Long projectId);
    
    // ======= 预算级统计接口 (Budget Level Statistics) =======
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.id = ?1")
    Double sumAmountByBudgetId(Long budgetId);
    
    Long countByBudgetId(Long budgetId);
    
    // ======= 预算项级统计接口 (Budget Item Level Statistics) =======
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budgetItem.id = ?1")
    Double sumAmountByBudgetItemId(Long budgetItemId);
    
    Long countByBudgetItemId(Long budgetItemId);
    
    // ======= 项目级统计接口 (Project Level Statistics) =======
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.project.id = ?1")
    Double sumAmountByProjectId(Long projectId);
    
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.project.id = ?1 AND be.expenseDate BETWEEN ?2 AND ?3")
    Double sumAmountByProjectIdAndExpenseDateBetween(Long projectId, Date startDate, Date endDate);
    
    @Query("SELECT COUNT(be) FROM BudgetExpense be WHERE be.budget.project.id = ?1")
    Long countByProjectId(Long projectId);
    
    // ======= 已废弃的含糊命名方法（逐步迁移中） =======
    /**
     * @deprecated 使用 findByProjectId 替代，明确语义
     */
    @Deprecated
    List<BudgetExpense> findByBudget_ProjectId(Long projectId);
    
    /**
     * @deprecated 使用 sumAmountByBudgetId 替代，保持命名一致性
     */
    @Deprecated
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.id = ?1")
    Double getTotalExpenseAmountByBudget(Long budgetId);
    
    /**
     * @deprecated 使用 sumAmountByBudgetItemId 替代，保持命名一致性
     */
    @Deprecated
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budgetItem.id = ?1")
    Double getTotalExpenseAmountByBudgetItem(Long budgetItemId);
    
    /**
     * @deprecated 使用 sumAmountByProjectId 替代，保持命名一致性
     */
    @Deprecated
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.project.id = ?1")
    Double getTotalExpenseAmountByProject(Long projectId);
    
    /**
     * @deprecated 使用 sumAmountByProjectIdAndExpenseDateBetween 替代，保持命名一致性
     */
    @Deprecated
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.project.id = ?1 AND be.expenseDate BETWEEN ?2 AND ?3")
    Double getTotalExpenseAmountByProjectAndDateRange(Long projectId, Date startDate, Date endDate);
    
    /**
     * @deprecated 使用 sumAmountByProjectId 替代，明确语义
     */
    @Deprecated
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.project.id = ?1")
    Double sumAmountByBudget_ProjectId(Long projectId);

    /**
     * @deprecated 使用 sumAmountByProjectIdAndExpenseDateBetween 替代，明确语义
     */
    @Deprecated
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.project.id = ?1 AND be.expenseDate BETWEEN ?2 AND ?3")
    Double sumAmountByBudget_ProjectIdAndExpenseDateBetween(Long projectId, Date startDate, Date endDate);
    
    /**
     * @deprecated 使用 countByProjectId 替代，明确语义
     */
    @Deprecated
    Long countByBudget_ProjectId(Long projectId);

    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.expenseDate BETWEEN ?1 AND ?2")
    Double sumAmountByExpenseDateBetween(Date startDate, Date endDate);

    @Query("SELECT SUM(be.amount) FROM BudgetExpense be")
    Double sumTotalAmount();

    List<BudgetExpense> findByReimbursementRequestId(Long reimbursementRequestId);
    
    boolean existsByReferenceNumber(String referenceNumber);
}
