package com.example.simpleoa.repository;

import com.example.simpleoa.model.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    // 分页查询所有预算（带关联查询）
    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.project LEFT JOIN FETCH b.createdBy")
    Page<Budget> findAllWithProject(Pageable pageable);
    
    // 根据项目ID列表分页查询预算
    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.project LEFT JOIN FETCH b.createdBy WHERE b.project.id IN :projectIds")
    Page<Budget> findByProjectIdInWithProject(@Param("projectIds") List<Long> projectIds, Pageable pageable);
    
    // 复杂条件分页查询
    @Query("""
        SELECT b FROM Budget b 
        WHERE (:keyword IS NULL OR :keyword = '' OR b.name LIKE %:keyword%)
        AND (:projectId IS NULL OR b.project.id = :projectId)
        AND (:status IS NULL OR :status = '' OR b.status = :status)
        AND (:startDateFrom IS NULL OR b.startDate >= :startDateFrom)
        AND (:startDateTo IS NULL OR b.startDate <= :startDateTo)
        AND (:endDateFrom IS NULL OR b.endDate >= :endDateFrom)
        AND (:endDateTo IS NULL OR b.endDate <= :endDateTo)
        AND (:totalAmountFrom IS NULL OR b.totalAmount >= :totalAmountFrom)
        AND (:totalAmountTo IS NULL OR b.totalAmount <= :totalAmountTo)
        AND (:usedAmountFrom IS NULL OR b.usedAmount >= :usedAmountFrom)
        AND (:usedAmountTo IS NULL OR b.usedAmount <= :usedAmountTo)
        """)
    Page<Budget> findBudgetsWithConditions(
        @Param("keyword") String keyword,
        @Param("projectId") Long projectId,
        @Param("status") String status,
        @Param("startDateFrom") Date startDateFrom,
        @Param("startDateTo") Date startDateTo,
        @Param("endDateFrom") Date endDateFrom,
        @Param("endDateTo") Date endDateTo,
        @Param("totalAmountFrom") Double totalAmountFrom,
        @Param("totalAmountTo") Double totalAmountTo,
        @Param("usedAmountFrom") Double usedAmountFrom,
        @Param("usedAmountTo") Double usedAmountTo,
        Pageable pageable
    );
    
    // 根据项目ID列表的复杂条件分页查询（用于项目经理权限）
    @Query("""
        SELECT b FROM Budget b 
        WHERE b.project.id IN :projectIds
        AND (:keyword IS NULL OR :keyword = '' OR b.name LIKE %:keyword%)
        AND (:projectId IS NULL OR b.project.id = :projectId)
        AND (:status IS NULL OR :status = '' OR b.status = :status)
        AND (:startDateFrom IS NULL OR b.startDate >= :startDateFrom)
        AND (:startDateTo IS NULL OR b.startDate <= :startDateTo)
        AND (:endDateFrom IS NULL OR b.endDate >= :endDateFrom)
        AND (:endDateTo IS NULL OR b.endDate <= :endDateTo)
        AND (:totalAmountFrom IS NULL OR b.totalAmount >= :totalAmountFrom)
        AND (:totalAmountTo IS NULL OR b.totalAmount <= :totalAmountTo)
        AND (:usedAmountFrom IS NULL OR b.usedAmount >= :usedAmountFrom)
        AND (:usedAmountTo IS NULL OR b.usedAmount <= :usedAmountTo)
        """)
    Page<Budget> findBudgetsWithConditionsForManager(
        @Param("projectIds") List<Long> projectIds,
        @Param("keyword") String keyword,
        @Param("projectId") Long projectId,
        @Param("status") String status,
        @Param("startDateFrom") Date startDateFrom,
        @Param("startDateTo") Date startDateTo,
        @Param("endDateFrom") Date endDateFrom,
        @Param("endDateTo") Date endDateTo,
        @Param("totalAmountFrom") Double totalAmountFrom,
        @Param("totalAmountTo") Double totalAmountTo,
        @Param("usedAmountFrom") Double usedAmountFrom,
        @Param("usedAmountTo") Double usedAmountTo,
        Pageable pageable
    );
    
    // 简化版本的查询方法用于测试
    @Query("SELECT b FROM Budget b WHERE b.project.id IN :projectIds")
    Page<Budget> findSimpleBudgetsForManager(@Param("projectIds") List<Long> projectIds, Pageable pageable);
}
