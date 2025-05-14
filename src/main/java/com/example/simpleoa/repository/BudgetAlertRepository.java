package com.example.simpleoa.repository;

import com.example.simpleoa.model.BudgetAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, Long> {
    List<BudgetAlert> findByBudgetId(Long budgetId);
    List<BudgetAlert> findByBudgetItemId(Long budgetItemId);
    List<BudgetAlert> findByAlertType(String alertType);
    List<BudgetAlert> findByAlertLevel(String alertLevel);
    List<BudgetAlert> findByStatus(String status);
    List<BudgetAlert> findByAlertTimeBetween(Date startDate, Date endDate);
    
    @Query("SELECT ba FROM BudgetAlert ba WHERE ba.budget.project.id = ?1")
    List<BudgetAlert> findByProjectId(Long projectId);
    
    @Query("SELECT ba FROM BudgetAlert ba WHERE ba.budget.project.id = ?1 AND ba.status = ?2")
    List<BudgetAlert> findByProjectIdAndStatus(Long projectId, String status);
    
    @Query("SELECT COUNT(ba) FROM BudgetAlert ba WHERE ba.budget.id = ?1 AND ba.status = '未解决'")
    Long countUnresolvedAlertsByBudget(Long budgetId);
    
    @Query("SELECT COUNT(ba) FROM BudgetAlert ba WHERE ba.budget.project.id = ?1 AND ba.status = '未解决'")
    Long countUnresolvedAlertsByProject(Long projectId);
}
