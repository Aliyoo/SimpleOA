package com.example.simpleoa.repository;

import com.example.simpleoa.model.BudgetExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BudgetExpenseRepository extends JpaRepository<BudgetExpense, Long> {
    List<BudgetExpense> findByBudgetId(Long budgetId);
    List<BudgetExpense> findByBudgetItemId(Long budgetItemId);
    List<BudgetExpense> findByExpenseType(String expenseType);
    List<BudgetExpense> findByExpenseDateBetween(Date startDate, Date endDate);
    List<BudgetExpense> findByStatus(String status);
    
    @Query("SELECT be FROM BudgetExpense be WHERE be.budget.project.id = ?1")
    List<BudgetExpense> findByProjectId(Long projectId);
    
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.id = ?1")
    Double getTotalExpenseAmountByBudget(Long budgetId);
    
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budgetItem.id = ?1")
    Double getTotalExpenseAmountByBudgetItem(Long budgetItemId);
    
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.project.id = ?1")
    Double getTotalExpenseAmountByProject(Long projectId);
    
    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.project.id = ?1 AND be.expenseDate BETWEEN ?2 AND ?3")
    Double getTotalExpenseAmountByProjectAndDateRange(Long projectId, Date startDate, Date endDate);

    List<BudgetExpense> findByBudget_ProjectId(Long projectId);

    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.id = ?1")
    Double sumAmountByBudgetId(Long budgetId);

    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budgetItem.id = ?1")
    Double sumAmountByBudgetItemId(Long budgetItemId);

    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.project.id = ?1")
    Double sumAmountByBudget_ProjectId(Long projectId);

    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.budget.project.id = ?1 AND be.expenseDate BETWEEN ?2 AND ?3")
    Double sumAmountByBudget_ProjectIdAndExpenseDateBetween(Long projectId, Date startDate, Date endDate);

    Long countByBudgetId(Long budgetId);

    Long countByBudgetItemId(Long budgetItemId);

    Long countByBudget_ProjectId(Long projectId);

    @Query("SELECT SUM(be.amount) FROM BudgetExpense be WHERE be.expenseDate BETWEEN ?1 AND ?2")
    Double sumAmountByExpenseDateBetween(Date startDate, Date endDate);

    @Query("SELECT SUM(be.amount) FROM BudgetExpense be")
    Double sumTotalAmount();
}
