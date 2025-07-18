package com.example.simpleoa.repository;

import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.ReimbursementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReimbursementRequestRepository extends JpaRepository<ReimbursementRequest, Long> {
    
    @Query("SELECT r FROM ReimbursementRequest r LEFT JOIN FETCH r.applicant LEFT JOIN FETCH r.items WHERE r.applicant.id = :applicantId")
    Page<ReimbursementRequest> findByApplicantIdWithDetails(@Param("applicantId") Long applicantId, Pageable pageable);
    
    @Query("SELECT r FROM ReimbursementRequest r LEFT JOIN FETCH r.applicant LEFT JOIN FETCH r.items WHERE r.applicant.id = :applicantId AND r.createTime BETWEEN :startTime AND :endTime")
    List<ReimbursementRequest> findByApplicantIdAndCreateTimeBetweenWithDetails(@Param("applicantId") Long applicantId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    // 带筛选条件的查询
    @Query("SELECT r FROM ReimbursementRequest r LEFT JOIN FETCH r.applicant LEFT JOIN FETCH r.items " +
           "WHERE r.applicant.id = :applicantId " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:startDate IS NULL OR r.createTime >= :startDate) " +
           "AND (:endDate IS NULL OR r.createTime <= :endDate) " +
           "AND (:keyword IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY r.createTime DESC")
    Page<ReimbursementRequest> findByApplicantIdWithFilters(
            @Param("applicantId") Long applicantId,
            @Param("status") ReimbursementStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword,
            Pageable pageable);
    
    // 原始方法保留作为备用
    Page<ReimbursementRequest> findByApplicantId(Long applicantId, Pageable pageable);
    List<ReimbursementRequest> findByApplicantIdAndCreateTimeBetween(Long applicantId, LocalDateTime startTime, LocalDateTime endTime);
}
