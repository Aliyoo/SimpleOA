package com.example.simpleoa.repository;

import com.example.simpleoa.model.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {
    List<BudgetItem> findByBudgetId(Long budgetId);
    List<BudgetItem> findByCategory(String category);
    List<BudgetItem> findByStatus(String status);
    
    @Query("SELECT bi FROM BudgetItem bi WHERE bi.budget.project.id = ?1")
    List<BudgetItem> findByProjectId(Long projectId);
    
    @Query("SELECT bi FROM BudgetItem bi WHERE bi.budget.id = ?1 AND bi.remainingAmount < (bi.amount * 0.1)")
    List<BudgetItem> findLowRemainingItems(Long budgetId);
    
    @Query("SELECT SUM(bi.amount) FROM BudgetItem bi WHERE bi.budget.id = ?1")
    Double getTotalAmountByBudget(Long budgetId);
    
    @Query("SELECT SUM(bi.usedAmount) FROM BudgetItem bi WHERE bi.budget.id = ?1")
    Double getTotalUsedAmountByBudget(Long budgetId);
}
