package com.example.simpleoa.service;

import com.example.simpleoa.model.*;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

public interface ApprovalFlowService {
    // 工时审批
    ApprovalFlow createApprovalFlow(WorkTimeRecord workTimeRecord, User approver);

    // 请假审批
    ApprovalFlow createLeaveApproval(LeaveRequest leaveRequest, User approver);

    // 出差审批
    ApprovalFlow createBusinessTripApproval(BusinessTripRequest tripRequest, User approver);

    // 报销审批
    ApprovalFlow createReimbursementApproval(ReimbursementRequest reimbursementRequest, User approver);

    // 更新审批状态
    ApprovalFlow updateApprovalFlowStatus(Long flowId, String status, String comment);

    // 获取工时记录相关的审批流程
    List<ApprovalFlow> getApprovalFlowsByWorkTimeRecord(Long workTimeRecordId);

    // 获取请假申请相关的审批流程
    List<ApprovalFlow> getApprovalFlowsByLeaveRequest(Long leaveRequestId);

    // 获取出差申请相关的审批流程
    List<ApprovalFlow> getApprovalFlowsByBusinessTripRequest(Long businessTripRequestId);

    // 获取报销申请相关的审批流程
    List<ApprovalFlow> getApprovalFlowsByReimbursementRequest(Long reimbursementRequestId);

    // 获取审批人相关的审批流程
    List<ApprovalFlow> getApprovalFlowsByApprover(Long approverId);

    // 获取审批人和状态相关的审批流程
    List<ApprovalFlow> getApprovalFlowsByApproverAndStatus(Long approverId, String status);

    // 获取特定状态的审批流程
    List<ApprovalFlow> getApprovalFlowsByStatus(String status);

    // 获取特定类型的审批流程
    List<ApprovalFlow> getApprovalFlowsByType(String requestType);

    // 根据ID获取审批流程
    ApprovalFlow getApprovalFlowById(Long id);

    // 获取审批历史
    List<ApprovalFlow> getApprovalHistory(Long flowId);

    // 通知状态变更
    void notifyStatusChange(Long flowId, String newStatus);

    // 获取所有审批流程
    List<ApprovalFlow> getAllApprovals();

    // 新增：按创建时间范围查询审批流程
    List<ApprovalFlow> getApprovalFlowsByDateRange(Date startDate, Date endDate);

    // 新增：按审批人ID和创建时间范围查询审批流程
    List<ApprovalFlow> getApprovalFlowsByApproverAndDateRange(Long approverId, Date startDate, Date endDate);

    // 新增：按审批人ID、请求类型和状态查询审批流程
    List<ApprovalFlow> getApprovalFlowsByApproverAndTypeAndStatus(Long approverId, String requestType, String status);

    // 新增：按审批人ID和请求类型查询审批流程
    List<ApprovalFlow> getApprovalFlowsByApproverAndType(Long approverId, String requestType);

    // 新增：按审批人ID、创建时间范围和请求类型查询审批流程
    List<ApprovalFlow> getApprovalFlowsByApproverAndDateRangeAndType(
            Long approverId, Date startDate, Date endDate, String requestType);

    // 新增：按审批人ID、创建时间范围和状态查询审批流程
    List<ApprovalFlow> getApprovalFlowsByApproverAndDateRangeAndStatus(
            Long approverId, Date startDate, Date endDate, String status);

    // 新增：按审批人ID、创建时间范围、请求类型和状态查询审批流程
    List<ApprovalFlow> getApprovalFlowsByApproverAndDateRangeAndTypeAndStatus(
            Long approverId, Date startDate, Date endDate, String requestType, String status);

    // 新增：分页查询我的审批
    Page<ApprovalFlow> getMyApprovalsPaged(Long approverId, int page, int size);

    // 新增：按工时记录项目ID查询审批流程
    List<ApprovalFlow> getApprovalFlowsByProjectId(Long projectId);

    // 新增：批量更新审批状态
    int batchUpdateApprovalStatus(List<Long> flowIds, String status, String comment);
}