package com.example.simpleoa.repository;

import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.ReimbursementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReimbursementRequestRepository extends JpaRepository<ReimbursementRequest, Long> {

    /**
     * 查询申请人的报销申请(带关联加载)
     * 使用EntityGraph避免LEFT JOIN FETCH导致的分页问题
     */
    @EntityGraph(value = "withApplicantAndItems")
    @Query("SELECT r FROM ReimbursementRequest r WHERE r.applicant.id = :applicantId ORDER BY r.createTime DESC")
    Page<ReimbursementRequest> findByApplicantIdWithDetails(@Param("applicantId") Long applicantId, Pageable pageable);

    /**
     * 查询申请人在指定时间范围内的报销申请
     */
    @EntityGraph(value = "withApplicantAndItems")
    @Query("SELECT r FROM ReimbursementRequest r WHERE r.applicant.id = :applicantId AND r.createTime BETWEEN :startTime AND :endTime")
    List<ReimbursementRequest> findByApplicantIdAndCreateTimeBetweenWithDetails(
            @Param("applicantId") Long applicantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 带筛选条件的查询(用于普通用户的报销列表)
     * 使用EntityGraph避免LEFT JOIN FETCH导致的分页问题
     */
    @EntityGraph(value = "withApplicantAndItems")
    @Query("SELECT r FROM ReimbursementRequest r " +
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

    /**
     * 按状态查询(用于财务审查)
     * 使用EntityGraph避免LEFT JOIN FETCH导致的分页问题
     * 这是最关键的修复,解决了总数与列表数量不对应的问题
     */
    @EntityGraph(value = "withApplicantAndItems")
    @Query("SELECT r FROM ReimbursementRequest r WHERE r.status = :status ORDER BY r.createTime DESC")
    Page<ReimbursementRequest> findByStatus(@Param("status") ReimbursementStatus status, Pageable pageable);

    /**
     * 按多个状态查询(用于财务人员全流程查看)
     * 使用EntityGraph避免LEFT JOIN FETCH导致的分页问题
     */
    @EntityGraph(value = "withApplicantAndItems")
    @Query("SELECT r FROM ReimbursementRequest r WHERE r.status IN :statuses ORDER BY r.createTime DESC")
    Page<ReimbursementRequest> findByStatusIn(@Param("statuses") List<ReimbursementStatus> statuses, Pageable pageable);
}
