package com.example.simpleoa.repository;

import com.example.simpleoa.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByProjectId(Long projectId);
    List<Budget> findByStatus(String status);
    List<Budget> findByStartDateBetweenOrEndDateBetween(Date startDate1, Date endDate1, Date startDate2, Date endDate2);
    
    @Query("SELECT b FROM Budget b WHERE b.project.id = ?1 AND b.status = '活跃' AND b.startDate <= ?2 AND b.endDate >= ?2")
    List<Budget> findActiveBudgetsByProjectAndDate(Long projectId, Date date);
    
    @Query("SELECT SUM(b.totalAmount) FROM Budget b WHERE b.project.id = ?1")
    Double getTotalBudgetAmountByProject(Long projectId);
    
    @Query("SELECT SUM(b.usedAmount) FROM Budget b WHERE b.project.id = ?1")
    Double getTotalUsedAmountByProject(Long projectId);
    
    // 使用悲观锁查询
    @Query("SELECT b FROM Budget b WHERE b.id = ?1")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Budget> findByIdForUpdate(Long id);

    Long countByProjectId(Long projectId);

    @Query("SELECT SUM(b.remainingAmount) FROM Budget b WHERE b.project.id = ?1")
    Double getTotalRemainingAmountByProject(Long projectId);

    @Query("SELECT SUM(b.totalAmount) FROM Budget b WHERE (b.startDate BETWEEN ?1 AND ?2) OR (b.endDate BETWEEN ?1 AND ?2)")
    Double getTotalBudgetAmountByDateRange(Date startDate, Date endDate);

    @Query("SELECT SUM(b.usedAmount) FROM Budget b WHERE (b.startDate BETWEEN ?1 AND ?2) OR (b.endDate BETWEEN ?1 AND ?2)")
    Double getTotalUsedAmountByDateRange(Date startDate, Date endDate);

    @Query("SELECT SUM(b.totalAmount) FROM Budget b")
    Double sumTotalAmount();

    @Query("SELECT SUM(b.usedAmount) FROM Budget b")
    Double sumUsedAmount();

    @Query("SELECT SUM(b.remainingAmount) FROM Budget b")
    Double sumRemainingAmount();
    
    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.project LEFT JOIN FETCH b.createdBy")
    List<Budget> findAllWithProject();

    @Query("SELECT b FROM Budget b WHERE b.project.id = ?1 AND b.remainingAmount > 0 AND b.status = 'ACTIVE'")
    List<Budget> findAvailableBudgetsByProject(Long projectId);
    
    // 根据项目ID列表查询预算
    @Query("SELECT b FROM Budget b WHERE b.project.id IN :projectIds")
    List<Budget> findByProjectIdIn(@Param("projectIds") List<Long> projectIds);
}
