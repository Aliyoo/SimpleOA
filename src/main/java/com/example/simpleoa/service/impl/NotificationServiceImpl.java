package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
    
    @Override
    public void sendLeaveApplicationNotification(LeaveRequest leaveRequest) {
        log.info("发送请假申请通知: 申请人={}, 请假类型={}", 
            leaveRequest.getApplicant().getRealName(), 
            leaveRequest.getLeaveType().getDisplayName());
        // TODO: 实现实际的通知逻辑（如发送邮件、短信等）
    }
    
    @Override
    public void sendLeaveApprovalNotification(LeaveRequest leaveRequest) {
        log.info("发送请假审批通过通知: 申请人={}, 请假类型={}", 
            leaveRequest.getApplicant().getRealName(), 
            leaveRequest.getLeaveType().getDisplayName());
        // TODO: 实现实际的通知逻辑
    }
    
    @Override
    public void sendLeaveRejectionNotification(LeaveRequest leaveRequest) {
        log.info("发送请假审批拒绝通知: 申请人={}, 请假类型={}", 
            leaveRequest.getApplicant().getRealName(), 
            leaveRequest.getLeaveType().getDisplayName());
        // TODO: 实现实际的通知逻辑
    }
    
    @Override
    public void sendReimbursementApplicationNotification(ReimbursementRequest reimbursementRequest) {
        log.info("发送报销申请通知: 申请人={}, 报销金额={}", 
            reimbursementRequest.getApplicant().getRealName(), 
            reimbursementRequest.getTotalAmount());
        // TODO: 实现实际的通知逻辑
    }
    
    @Override
    public void sendReimbursementApprovalNotification(ReimbursementRequest reimbursementRequest) {
        log.info("发送报销审批通过通知: 申请人={}, 报销金额={}", 
            reimbursementRequest.getApplicant().getRealName(), 
            reimbursementRequest.getTotalAmount());
        // TODO: 实现实际的通知逻辑
    }
    
    @Override
    public void sendReimbursementRejectionNotification(ReimbursementRequest reimbursementRequest) {
        log.info("发送报销审批拒绝通知: 申请人={}, 报销金额={}", 
            reimbursementRequest.getApplicant().getRealName(), 
            reimbursementRequest.getTotalAmount());
        // TODO: 实现实际的通知逻辑
    }
    
    @Override
    public void sendEmail(String to, String subject, String content) {
        log.info("发送邮件: 收件人={}, 主题={}", to, subject);
        // TODO: 实现实际的邮件发送逻辑
    }
    
    @Override
    public void sendSystemNotification(Long userId, String title, String content) {
        log.info("发送系统通知: 用户ID={}, 标题={}", userId, title);
        // TODO: 实现实际的系统通知逻辑
    }
}
