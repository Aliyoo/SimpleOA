package com.example.simpleoa.repository;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.ApprovalStatus;
import com.example.simpleoa.model.ApprovalStage;
import com.example.simpleoa.model.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, Long> {

    // ========== 新架构查询方法（Phase 1+） ==========

    /**
     * 根据实体类型和实体ID查询审批流程
     *
     * @param entityType 实体类型
     * @param entityId   实体ID
     * @return 审批流程列表
     */
    List<ApprovalFlow> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);

    /**
     * 根据实体类型、实体ID和审批状态查询
     *
     * @param entityType 实体类型
     * @param entityId   实体ID
     * @param status     审批状态
     * @return 审批流程列表
     */
    List<ApprovalFlow> findByEntityTypeAndEntityIdAndApprovalStatus(
            EntityType entityType, Long entityId, ApprovalStatus status);

    /**
     * 根据审批状态查询（使用新枚举）
     *
     * @param approvalStatus 审批状态
     * @return 审批流程列表
     */
    List<ApprovalFlow> findByApprovalStatus(ApprovalStatus approvalStatus);

    /**
     * 根据审批阶段查询
     *
     * @param approvalStage 审批阶段
     * @return 审批流程列表
     */
    List<ApprovalFlow> findByApprovalStage(ApprovalStage approvalStage);

    // ========== 遗留查询方法（过渡期保留） ==========

    List<ApprovalFlow> findByWorkTimeRecordId(Long workTimeRecordId);

    List<ApprovalFlow> findByLeaveRequestId(Long leaveRequestId);

    List<ApprovalFlow> findByBusinessTripRequestId(Long businessTripRequestId);

    List<ApprovalFlow> findByReimbursementRequestId(Long reimbursementRequestId);

    List<ApprovalFlow> findByApproverId(Long approverId);

    @Query("SELECT af FROM ApprovalFlow af " +
           "LEFT JOIN FETCH af.approver " +
           "LEFT JOIN FETCH af.workTimeRecord wtr " +
           "LEFT JOIN FETCH wtr.user " +
           "LEFT JOIN FETCH wtr.project " +
           "LEFT JOIN FETCH af.leaveRequest lr " +
           "LEFT JOIN FETCH lr.applicant " +
           "LEFT JOIN FETCH af.businessTripRequest btr " +
           "LEFT JOIN FETCH btr.applicant " +
           "LEFT JOIN FETCH af.reimbursementRequest rr " +
           "LEFT JOIN FETCH rr.applicant " +
           "LEFT JOIN FETCH rr.project " +
           "WHERE af.approver.id = :approverId")
    List<ApprovalFlow> findByApproverIdWithDetails(@Param("approverId") Long approverId);

    List<ApprovalFlow> findByRequestType(String requestType);

    List<ApprovalFlow> findByStatus(String status);

    @Query("SELECT af FROM ApprovalFlow af " +
           "LEFT JOIN FETCH af.approver " +
           "LEFT JOIN FETCH af.workTimeRecord wtr " +
           "LEFT JOIN FETCH wtr.user " +
           "LEFT JOIN FETCH wtr.project " +
           "LEFT JOIN FETCH af.leaveRequest lr " +
           "LEFT JOIN FETCH lr.applicant " +
           "LEFT JOIN FETCH af.businessTripRequest btr " +
           "LEFT JOIN FETCH btr.applicant " +
           "LEFT JOIN FETCH af.reimbursementRequest rr " +
           "LEFT JOIN FETCH rr.applicant " +
           "LEFT JOIN FETCH rr.project " +
           "WHERE af.approver.id = :approverId " +
           "AND af.status = :status")
    List<ApprovalFlow> findByApproverIdAndStatus(@Param("approverId") Long approverId, 
                                                 @Param("status") String status);

    List<ApprovalFlow> findHistoryById(Long flowId);

    // 新增：按创建时间范围查询
    List<ApprovalFlow> findByCreateTimeBetween(Date startDate, Date endDate);

    // 新增：按审批人ID和创建时间范围查询
    @Query("SELECT af FROM ApprovalFlow af " +
           "LEFT JOIN FETCH af.approver " +
           "LEFT JOIN FETCH af.workTimeRecord wtr " +
           "LEFT JOIN FETCH wtr.user " +
           "LEFT JOIN FETCH wtr.project " +
           "LEFT JOIN FETCH af.leaveRequest lr " +
           "LEFT JOIN FETCH lr.applicant " +
           "LEFT JOIN FETCH af.businessTripRequest btr " +
           "LEFT JOIN FETCH btr.applicant " +
           "LEFT JOIN FETCH af.reimbursementRequest rr " +
           "LEFT JOIN FETCH rr.applicant " +
           "LEFT JOIN FETCH rr.project " +
           "WHERE af.approver.id = :approverId " +
           "AND af.createTime BETWEEN :startDate AND :endDate")
    List<ApprovalFlow> findByApproverIdAndCreateTimeBetween(@Param("approverId") Long approverId, 
                                                            @Param("startDate") Date startDate, 
                                                            @Param("endDate") Date endDate);

    // 新增：按审批人ID、请求类型和状态查询
    @Query("SELECT af FROM ApprovalFlow af " +
           "LEFT JOIN FETCH af.approver " +
           "LEFT JOIN FETCH af.workTimeRecord wtr " +
           "LEFT JOIN FETCH wtr.user " +
           "LEFT JOIN FETCH wtr.project " +
           "LEFT JOIN FETCH af.leaveRequest lr " +
           "LEFT JOIN FETCH lr.applicant " +
           "LEFT JOIN FETCH af.businessTripRequest btr " +
           "LEFT JOIN FETCH btr.applicant " +
           "LEFT JOIN FETCH af.reimbursementRequest rr " +
           "LEFT JOIN FETCH rr.applicant " +
           "LEFT JOIN FETCH rr.project " +
           "WHERE af.approver.id = :approverId " +
           "AND af.requestType = :requestType " +
           "AND af.status = :status")
    List<ApprovalFlow> findByApproverIdAndRequestTypeAndStatus(@Param("approverId") Long approverId, 
                                                               @Param("requestType") String requestType, 
                                                               @Param("status") String status);

    // 新增：按审批人ID和请求类型查询
    @Query("SELECT af FROM ApprovalFlow af " +
           "LEFT JOIN FETCH af.approver " +
           "LEFT JOIN FETCH af.workTimeRecord wtr " +
           "LEFT JOIN FETCH wtr.user " +
           "LEFT JOIN FETCH wtr.project " +
           "LEFT JOIN FETCH af.leaveRequest lr " +
           "LEFT JOIN FETCH lr.applicant " +
           "LEFT JOIN FETCH af.businessTripRequest btr " +
           "LEFT JOIN FETCH btr.applicant " +
           "LEFT JOIN FETCH af.reimbursementRequest rr " +
           "LEFT JOIN FETCH rr.applicant " +
           "LEFT JOIN FETCH rr.project " +
           "WHERE af.approver.id = :approverId " +
           "AND af.requestType = :requestType")
    List<ApprovalFlow> findByApproverIdAndRequestType(@Param("approverId") Long approverId, 
                                                      @Param("requestType") String requestType);

    // 新增：按审批人ID、创建时间范围和请求类型查询
    @Query("SELECT af FROM ApprovalFlow af " +
           "LEFT JOIN FETCH af.approver " +
           "LEFT JOIN FETCH af.workTimeRecord wtr " +
           "LEFT JOIN FETCH wtr.user " +
           "LEFT JOIN FETCH wtr.project " +
           "LEFT JOIN FETCH af.leaveRequest lr " +
           "LEFT JOIN FETCH lr.applicant " +
           "LEFT JOIN FETCH af.businessTripRequest btr " +
           "LEFT JOIN FETCH btr.applicant " +
           "LEFT JOIN FETCH af.reimbursementRequest rr " +
           "LEFT JOIN FETCH rr.applicant " +
           "LEFT JOIN FETCH rr.project " +
           "WHERE af.approver.id = :approverId " +
           "AND af.createTime BETWEEN :startDate AND :endDate " +
           "AND af.requestType = :requestType")
    List<ApprovalFlow> findByApproverIdAndCreateTimeBetweenAndRequestType(
            @Param("approverId") Long approverId, 
            @Param("startDate") Date startDate, 
            @Param("endDate") Date endDate, 
            @Param("requestType") String requestType);

    // 新增：按审批人ID、创建时间范围和状态查询
    @Query("SELECT af FROM ApprovalFlow af " +
           "LEFT JOIN FETCH af.approver " +
           "LEFT JOIN FETCH af.workTimeRecord wtr " +
           "LEFT JOIN FETCH wtr.user " +
           "LEFT JOIN FETCH wtr.project " +
           "LEFT JOIN FETCH af.leaveRequest lr " +
           "LEFT JOIN FETCH lr.applicant " +
           "LEFT JOIN FETCH af.businessTripRequest btr " +
           "LEFT JOIN FETCH btr.applicant " +
           "LEFT JOIN FETCH af.reimbursementRequest rr " +
           "LEFT JOIN FETCH rr.applicant " +
           "LEFT JOIN FETCH rr.project " +
           "WHERE af.approver.id = :approverId " +
           "AND af.createTime BETWEEN :startDate AND :endDate " +
           "AND af.status = :status")
    List<ApprovalFlow> findByApproverIdAndCreateTimeBetweenAndStatus(
            @Param("approverId") Long approverId, 
            @Param("startDate") Date startDate, 
            @Param("endDate") Date endDate, 
            @Param("status") String status);

    // 新增：按审批人ID、创建时间范围、请求类型和状态查询
    @Query("SELECT af FROM ApprovalFlow af " +
           "LEFT JOIN FETCH af.approver " +
           "LEFT JOIN FETCH af.workTimeRecord wtr " +
           "LEFT JOIN FETCH wtr.user " +
           "LEFT JOIN FETCH wtr.project " +
           "LEFT JOIN FETCH af.leaveRequest lr " +
           "LEFT JOIN FETCH lr.applicant " +
           "LEFT JOIN FETCH af.businessTripRequest btr " +
           "LEFT JOIN FETCH btr.applicant " +
           "LEFT JOIN FETCH af.reimbursementRequest rr " +
           "LEFT JOIN FETCH rr.applicant " +
           "LEFT JOIN FETCH rr.project " +
           "WHERE af.approver.id = :approverId " +
           "AND af.createTime BETWEEN :startDate AND :endDate " +
           "AND af.requestType = :requestType " +
           "AND af.status = :status")
    List<ApprovalFlow> findByApproverIdAndCreateTimeBetweenAndRequestTypeAndStatus(
            @Param("approverId") Long approverId, 
            @Param("startDate") Date startDate, 
            @Param("endDate") Date endDate, 
            @Param("requestType") String requestType, 
            @Param("status") String status);

    // 新增：分页查询我的审批
    @Query("SELECT af FROM ApprovalFlow af " +
           "LEFT JOIN FETCH af.approver " +
           "LEFT JOIN FETCH af.workTimeRecord wtr " +
           "LEFT JOIN FETCH wtr.user " +
           "LEFT JOIN FETCH wtr.project " +
           "LEFT JOIN FETCH af.leaveRequest lr " +
           "LEFT JOIN FETCH lr.applicant " +
           "LEFT JOIN FETCH af.businessTripRequest btr " +
           "LEFT JOIN FETCH btr.applicant " +
           "LEFT JOIN FETCH af.reimbursementRequest rr " +
           "LEFT JOIN FETCH rr.applicant " +
           "LEFT JOIN FETCH rr.project " +
           "WHERE af.approver.id = :approverId")
    Page<ApprovalFlow> findByApproverIdWithDetailsPaged(@Param("approverId") Long approverId, Pageable pageable);

    // 新增：按工时记录项目ID查询
    @Query("SELECT af FROM ApprovalFlow af " +
           "WHERE af.requestType = 'WORKTIME' " +
           "AND af.workTimeRecord.project.id = :projectId")
    List<ApprovalFlow> findByWorkTimeRecordProjectId(@Param("projectId") Long projectId);

    // 新增：批量更新审批状态
    @Modifying
    @Query("UPDATE ApprovalFlow af SET af.status = :status, af.updateTime = :updateTime " +
           "WHERE af.id IN :ids AND af.status = 'PENDING'")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status, @Param("updateTime") Date updateTime);
    
    // 新增：根据报销请求ID、请求类型和状态查询审批流程
    @Query("SELECT af FROM ApprovalFlow af " +
           "LEFT JOIN FETCH af.approver " +
           "LEFT JOIN FETCH af.approver.roles " +
           "WHERE af.reimbursementRequest.id = :reimbursementRequestId " +
           "AND af.requestType = :requestType " +
           "AND af.status = :status")
    List<ApprovalFlow> findByReimbursementRequestIdAndRequestTypeAndStatus(
            @Param("reimbursementRequestId") Long reimbursementRequestId,
            @Param("requestType") String requestType,
            @Param("status") String status);
}
