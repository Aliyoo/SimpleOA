package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.LeaveRequestRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.ApprovalFlowService;
import com.example.simpleoa.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveServiceImpl implements LeaveService {
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    
    @Autowired
    private ApprovalFlowService approvalFlowService;
    
    @Autowired
    private UserRepository userRepository;

    @Override
    public LeaveRequest createLeave(LeaveRequest leaveRequest) {
        // 保存请假申请
        leaveRequest.setStatus("PENDING");
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        
        // 创建审批流程
        // 这里假设审批人是通过某种方式确定的，例如部门经理或指定的审批人
        User approver = userRepository.findById(leaveRequest.getApplicant().getId())
                .orElseThrow(() -> new IllegalArgumentException("Approver not found"));
        
        approvalFlowService.createLeaveApproval(savedRequest, approver);
        
        return savedRequest;
    }

    @Override
    public LeaveRequest updateLeave(LeaveRequest leaveRequest) {
        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public void deleteLeave(Long id) {
        leaveRequestRepository.deleteById(id);
    }

    @Override
    public LeaveRequest getLeaveById(Long id) {
        return leaveRequestRepository.findById(id).orElse(null);
    }

    @Override
    public Iterable<LeaveRequest> getAllLeaves() {
        return leaveRequestRepository.findAll();
    }

    @Override
    public Iterable<LeaveRequest> getLeavesByUser(Long userId) {
        return leaveRequestRepository.findByUserId(userId);
    }

    @Override
    public LeaveRequest approveLeave(Long id, String status, String comment) {
        LeaveRequest request = getLeaveById(id);
        if (request != null) {
            request.setStatus(status);
            request.setComment(comment);
            LeaveRequest savedRequest = leaveRequestRepository.save(request);
            
            // 更新审批流程状态
            // 获取与此请假申请相关的审批流程
            List<ApprovalFlow> flows = approvalFlowService.getApprovalFlowsByLeaveRequest(id);
            if (!flows.isEmpty()) {
                // 更新最新的审批流程状态
                ApprovalFlow latestFlow = flows.get(0); // 假设按创建时间排序，第一个是最新的
                approvalFlowService.updateApprovalFlowStatus(latestFlow.getId(), status, comment);
                
                // 通知状态变更
                approvalFlowService.notifyStatusChange(latestFlow.getId(), status);
            }
            
            return savedRequest;
        }
        return null;
    }
}