package com.example.simpleoa.repository;

import com.example.simpleoa.model.ReimbursementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReimbursementRequestRepository extends JpaRepository<ReimbursementRequest, Long> {
    @Query("SELECT r FROM ReimbursementRequest r WHERE r.applicant.id = :userId")
    List<ReimbursementRequest> findByUserId(@Param("userId") Long userId);
    
    // 按状态查询
    List<ReimbursementRequest> findByStatus(String status);
    
    // 按报销类型查询
    List<ReimbursementRequest> findByType(String type);
    
    // 按申请日期范围查询
    //List<ReimbursementRequest> findByApplyDateBetween(Date startDate, Date endDate);
    
    // 按金额范围查询
    List<ReimbursementRequest> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);
    
    // 按项目ID查询
    //List<ReimbursementRequest> findByProjectId(Long projectId);
    
    // 按部门查询
    @Query("SELECT r FROM ReimbursementRequest r WHERE r.applicant.department = :department")
    List<ReimbursementRequest> findByDepartment(@Param("department") String department);
    
    // 统计用户的报销总金额
    @Query("SELECT SUM(r.amount) FROM ReimbursementRequest r WHERE r.applicant.id = :userId AND r.status = 'APPROVED'")
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);
    
    // 按报销类型统计用户的报销总金额
    @Query("SELECT SUM(r.amount) FROM ReimbursementRequest r WHERE r.applicant.id = :userId AND r.type = :type AND r.status = 'APPROVED'")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);
    
    // 统计部门的报销申请数量
    @Query("SELECT r.applicant.department, COUNT(r) FROM ReimbursementRequest r GROUP BY r.applicant.department")
    List<Object[]> countReimbursementsByDepartment();
    
    // 统计部门的报销总金额
    @Query("SELECT r.applicant.department, SUM(r.amount) FROM ReimbursementRequest r WHERE r.status = 'APPROVED' GROUP BY r.applicant.department")
    List<Object[]> sumAmountByDepartment();
    
    // 查询待处理的报销申请
    //@Query("SELECT r FROM ReimbursementRequest r WHERE r.status = 'PENDING' ORDER BY r.applyDate ASC")
    //List<ReimbursementRequest> findPendingReimbursements();
    
    // 查询超过指定金额的报销申请
    @Query("SELECT r FROM ReimbursementRequest r WHERE r.amount > :amount")
    List<ReimbursementRequest> findByAmountGreaterThan(@Param("amount") BigDecimal amount);

    // 分页查询，支持多条件过滤
    @Query("SELECT r FROM ReimbursementRequest r WHERE " +
            "(:userId IS NULL OR r.applicant.id = :userId) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:startDate IS NULL OR r.expenseDate >= :startDate) AND " +
            "(:endDate IS NULL OR r.expenseDate <= :endDate)")
    Page<ReimbursementRequest> findWithFilters(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
}