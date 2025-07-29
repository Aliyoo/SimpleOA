package com.example.simpleoa.repository;

import com.example.simpleoa.model.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

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

    Long countByBudgetId(Long budgetId);

    @Query("SELECT COUNT(bi) FROM BudgetItem bi WHERE bi.budget.project.id = ?1")
    Long countByProjectId(Long projectId);

    // 使用悲观锁查询
    @Query("SELECT bi FROM BudgetItem bi WHERE bi.id = ?1")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BudgetItem> findByIdForUpdate(Long id);
}
