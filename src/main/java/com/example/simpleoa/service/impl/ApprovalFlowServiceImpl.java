package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.ApprovalFlowRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.ApprovalFlowService;
import com.example.simpleoa.service.WorkTimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ApprovalFlowServiceImpl implements ApprovalFlowService {
    private final ApprovalFlowRepository approvalFlowRepository;

    private final UserRepository userRepository;

    public ApprovalFlowServiceImpl(ApprovalFlowRepository approvalFlowRepository, UserRepository userRepository) {
        this.approvalFlowRepository = approvalFlowRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ApprovalFlow createApprovalFlow(WorkTimeRecord workTimeRecord, User approver) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setWorkTimeRecord(workTimeRecord);
        flow.setApprover(approver);
        flow.setStatus("PENDING");
        flow.setCreateTime(new Date());
        flow.setRequestType("WORKTIME");
        return approvalFlowRepository.save(flow);
    }

    @Autowired
    @Lazy
    private WorkTimeService workTimeService;

    @Override
    @Transactional
    public ApprovalFlow updateApprovalFlowStatus(Long flowId, String status, String comment) {
        ApprovalFlow flow = approvalFlowRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Approval flow not found"));
        flow.setStatus(status);
        flow.setComment(comment);
        flow.setUpdateTime(new Date());

        // 如果是工时审批，同步更新工时记录的状态
        if ("WORKTIME".equals(flow.getRequestType()) && flow.getWorkTimeRecord() != null) {
            WorkTimeRecord workTimeRecord = flow.getWorkTimeRecord();
            if ("APPROVED".equals(status)) {
                // 如果审批通过，将工时记录的approved字段设置为true
                workTimeRecord.setApproved(true);
                System.out.println("工时记录ID " + workTimeRecord.getId() + " 已被批准");
            } else if ("REJECTED".equals(status)) {
                // 如果审批拒绝，可以设置其他状态
                workTimeRecord.setApproved(false);
                System.out.println("工时记录ID " + workTimeRecord.getId() + " 已被拒绝");
            }

            // 保存工时记录
            if (workTimeService != null) {
                try {
                    workTimeService.saveWorkTimeRecord(workTimeRecord);
                } catch (Exception e) {
                    System.err.println("更新工时记录状态失败: " + e.getMessage());
                }
            } else {
                System.err.println("无法更新工时记录状态: workTimeService 为 null");
            }
        }

        return approvalFlowRepository.save(flow);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByWorkTimeRecord(Long workTimeRecordId) {
        return approvalFlowRepository.findByWorkTimeRecordId(workTimeRecordId);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByApprover(Long approverId) {
        // 使用新的查询方法，带有JOIN FETCH加载关联实体
        return approvalFlowRepository.findByApproverIdWithDetails(approverId);
    }

    @Override
    public ApprovalFlow getApprovalFlowById(Long id) {
        return approvalFlowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Approval flow not found"));
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByLeaveRequest(Long leaveRequestId) {
        return approvalFlowRepository.findByLeaveRequestId(leaveRequestId);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByBusinessTripRequest(Long businessTripRequestId) {
        return approvalFlowRepository.findByBusinessTripRequestId(businessTripRequestId);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByReimbursementRequest(Long reimbursementRequestId) {
        return approvalFlowRepository.findByReimbursementRequestId(reimbursementRequestId);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByStatus(String status) {
        return approvalFlowRepository.findByStatus(status);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByType(String requestType) {
        return approvalFlowRepository.findByRequestType(requestType);
    }

    @Override
    public List<ApprovalFlow> getApprovalHistory(Long flowId) {
        return approvalFlowRepository.findHistoryById(flowId);
    }

    @Override
    public void notifyStatusChange(Long flowId, String newStatus) {
        ApprovalFlow flow = getApprovalFlowById(flowId);
        if (flow == null) {
            return;
        }

        // 获取相关用户信息
        User requester = null;
        User approver = flow.getApprover();
        String requestType = flow.getRequestType();
        String requestTitle = "";

        // 根据不同类型的申请获取申请人和申请标题
        if ("WORKTIME".equals(requestType) && flow.getWorkTimeRecord() != null) {
            requester = flow.getWorkTimeRecord().getUser();
            requestTitle = "工时审批 - " + flow.getWorkTimeRecord().getProject().getName();
        } else if ("LEAVE".equals(requestType) && flow.getLeaveRequest() != null) {
            requester = userRepository.findById(flow.getLeaveRequest().getUserId()).orElse(null);
            requestTitle = "请假审批 - " + flow.getLeaveRequest().getLeaveType();
        } else if ("BUSINESS_TRIP".equals(requestType) && flow.getBusinessTripRequest() != null) {
            requester = userRepository.findById(flow.getBusinessTripRequest().getUserId()).orElse(null);
            requestTitle = "出差审批 - " + flow.getBusinessTripRequest().getDestination();
        } else if ("REIMBURSEMENT".equals(requestType) && flow.getReimbursementRequest() != null) {
            requester = userRepository.findById(flow.getReimbursementRequest().getUserId()).orElse(null);
            requestTitle = "报销审批 - " + flow.getReimbursementRequest().getDescription();
        }

        // 构建通知消息
        String statusText = "";
        switch (newStatus) {
            case "APPROVED":
                statusText = "已批准";
                break;
            case "REJECTED":
                statusText = "已拒绝";
                break;
            case "PENDING":
                statusText = "待审批";
                break;
            default:
                statusText = newStatus;
        }

        // 发送通知给申请人
        if (requester != null) {
            String message = String.format("您的申请 '%s' %s", requestTitle, statusText);
            // TODO: 调用实际的通知服务发送消息，如邮件、短信或系统内消息
            System.out.println("通知申请人: " + requester.getUsername() + ", 消息: " + message);
        }

        // 如果是新建的审批流程，通知审批人
        if ("PENDING".equals(newStatus) && approver != null) {
            String message = String.format("您有一个新的审批请求 '%s'", requestTitle);
            // TODO: 调用实际的通知服务发送消息
            System.out.println("通知审批人: " + approver.getUsername() + ", 消息: " + message);
        }
    }

    @Override
    @Transactional
    public ApprovalFlow createLeaveApproval(LeaveRequest leaveRequest, User approver) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setLeaveRequest(leaveRequest);
        flow.setApprover(approver);
        flow.setStatus("PENDING");
        flow.setCreateTime(new Date());
        flow.setRequestType("LEAVE");
        return approvalFlowRepository.save(flow);
    }

    @Override
    @Transactional
    public ApprovalFlow createBusinessTripApproval(BusinessTripRequest tripRequest, User approver) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setBusinessTripRequest(tripRequest);
        flow.setApprover(approver);
        flow.setStatus("PENDING");
        flow.setCreateTime(new Date());
        flow.setRequestType("BUSINESS_TRIP");
        return approvalFlowRepository.save(flow);
    }

    @Override
    @Transactional
    public ApprovalFlow createReimbursementApproval(ReimbursementRequest reimbursementRequest, User approver) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setReimbursementRequest(reimbursementRequest);
        flow.setApprover(approver);
        flow.setStatus("PENDING");
        flow.setCreateTime(new Date());
        flow.setRequestType("REIMBURSEMENT");
        return approvalFlowRepository.save(flow);
    }

    @Override
    public List<ApprovalFlow> getAllApprovals() {
        return approvalFlowRepository.findAll();
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByDateRange(Date startDate, Date endDate) {
        return approvalFlowRepository.findByCreateTimeBetween(startDate, endDate);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByApproverAndDateRange(Long approverId, Date startDate, Date endDate) {
        return approvalFlowRepository.findByApproverIdAndCreateTimeBetween(approverId, startDate, endDate);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByApproverAndTypeAndStatus(Long approverId, String requestType, String status) {
        return approvalFlowRepository.findByApproverIdAndRequestTypeAndStatus(approverId, requestType, status);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByApproverAndType(Long approverId, String requestType) {
        return approvalFlowRepository.findByApproverIdAndRequestType(approverId, requestType);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByApproverAndStatus(Long approverId, String status) {
        return approvalFlowRepository.findByApproverIdAndStatus(approverId, status);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByApproverAndDateRangeAndType(
            Long approverId, Date startDate, Date endDate, String requestType) {
        return approvalFlowRepository.findByApproverIdAndCreateTimeBetweenAndRequestType(
                approverId, startDate, endDate, requestType);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByApproverAndDateRangeAndStatus(
            Long approverId, Date startDate, Date endDate, String status) {
        return approvalFlowRepository.findByApproverIdAndCreateTimeBetweenAndStatus(
                approverId, startDate, endDate, status);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByApproverAndDateRangeAndTypeAndStatus(
            Long approverId, Date startDate, Date endDate, String requestType, String status) {
        return approvalFlowRepository.findByApproverIdAndCreateTimeBetweenAndRequestTypeAndStatus(
                approverId, startDate, endDate, requestType, status);
    }

    @Override
    public Page<ApprovalFlow> getMyApprovalsPaged(Long approverId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return approvalFlowRepository.findByApproverIdWithDetailsPaged(approverId, pageable);
    }

    @Override
    public List<ApprovalFlow> getApprovalFlowsByProjectId(Long projectId) {
        return approvalFlowRepository.findByWorkTimeRecordProjectId(projectId);
    }

    @Override
    @Transactional
    public int batchUpdateApprovalStatus(List<Long> flowIds, String status, String comment) {
        if (flowIds == null || flowIds.isEmpty()) {
            return 0;
        }

        // 首先获取所有审批流程，以便我们可以获取关联的工时记录ID
        List<ApprovalFlow> approvalFlows = approvalFlowRepository.findAllById(flowIds);
        List<Long> workTimeRecordIds = new ArrayList<>();

        // 收集所有工时记录ID
        for (ApprovalFlow flow : approvalFlows) {
            if ("WORKTIME".equals(flow.getRequestType()) && flow.getWorkTimeRecord() != null) {
                workTimeRecordIds.add(flow.getWorkTimeRecord().getId());
            }
        }

        // 批量更新审批流程状态
        int updatedCount = approvalFlowRepository.batchUpdateStatus(flowIds, status, new Date());

        // 如果是工时审批，还需要更新相关的工时记录状态
        if (updatedCount > 0 && workTimeService != null && !workTimeRecordIds.isEmpty()) {
            try {
                // 对每个工时记录进行处理
                for (Long recordId : workTimeRecordIds) {
                    if ("APPROVED".equals(status)) {
                        // 使用专门的审批方法
                        workTimeService.approveWorkTime(recordId);
                        System.out.println("工时记录ID " + recordId + " 已被批准");
                    } else if ("REJECTED".equals(status)) {
                        // 使用专门的拒绝方法
                        workTimeService.rejectWorkTime(recordId, comment != null ? comment : "批量拒绝");
                        System.out.println("工时记录ID " + recordId + " 已被拒绝");
                    }
                }

                System.out.println("成功更新 " + workTimeRecordIds.size() + " 条工时记录的状态");
            } catch (Exception e) {
                System.err.println("批量更新工时记录状态失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return updatedCount;
    }
}