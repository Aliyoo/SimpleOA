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
    List<BudgetAlert> findByAlertDateBetween(Date startDate, Date endDate);
    
    @Query("SELECT ba FROM BudgetAlert ba WHERE ba.budget.project.id = ?1")
    List<BudgetAlert> findByProjectId(Long projectId);
    
    @Query("SELECT ba FROM BudgetAlert ba WHERE ba.budget.project.id = ?1 AND ba.status = ?2")
    List<BudgetAlert> findByProjectIdAndStatus(Long projectId, String status);
    
    @Query("SELECT COUNT(ba) FROM BudgetAlert ba WHERE ba.budget.id = ?1 AND ba.status = '未解决'")
    Long countUnresolvedAlertsByBudget(Long budgetId);
    
    @Query("SELECT COUNT(ba) FROM BudgetAlert ba WHERE ba.budget.project.id = ?1 AND ba.status = '未解决'")
    Long countUnresolvedAlertsByProject(Long projectId);

    List<BudgetAlert> findByBudget_ProjectId(Long projectId);

    List<BudgetAlert> findByBudget_ProjectIdAndStatus(Long projectId, String status);

    Long countByBudgetIdAndStatus(Long budgetId, String status);

    Long countByBudget_ProjectIdAndStatus(Long projectId, String status);

    Long countByBudgetId(Long budgetId);

    Long countByBudgetItemId(Long budgetItemId);

    Long countByAlertDateBetween(Date startDate, Date endDate);

    Long countByStatus(String status);
}
