package com.example.simpleoa.repository;

import com.example.simpleoa.model.ApprovalFlow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, Long> {
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
           "WHERE af.approver.id = :approverId")
    List<ApprovalFlow> findByApproverIdWithDetails(@Param("approverId") Long approverId);

    List<ApprovalFlow> findByRequestType(String requestType);

    List<ApprovalFlow> findByStatus(String status);

    List<ApprovalFlow> findByApproverIdAndStatus(Long approverId, String status);

    List<ApprovalFlow> findHistoryById(Long flowId);

    // 新增：按创建时间范围查询
    List<ApprovalFlow> findByCreateTimeBetween(Date startDate, Date endDate);

    // 新增：按审批人ID和创建时间范围查询
    List<ApprovalFlow> findByApproverIdAndCreateTimeBetween(Long approverId, Date startDate, Date endDate);

    // 新增：按审批人ID、请求类型和状态查询
    List<ApprovalFlow> findByApproverIdAndRequestTypeAndStatus(Long approverId, String requestType, String status);

    // 新增：按审批人ID和请求类型查询
    List<ApprovalFlow> findByApproverIdAndRequestType(Long approverId, String requestType);

    // 新增：按审批人ID、创建时间范围和请求类型查询
    List<ApprovalFlow> findByApproverIdAndCreateTimeBetweenAndRequestType(
            Long approverId, Date startDate, Date endDate, String requestType);

    // 新增：按审批人ID、创建时间范围和状态查询
    List<ApprovalFlow> findByApproverIdAndCreateTimeBetweenAndStatus(
            Long approverId, Date startDate, Date endDate, String status);

    // 新增：按审批人ID、创建时间范围、请求类型和状态查询
    List<ApprovalFlow> findByApproverIdAndCreateTimeBetweenAndRequestTypeAndStatus(
            Long approverId, Date startDate, Date endDate, String requestType, String status);

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
}
