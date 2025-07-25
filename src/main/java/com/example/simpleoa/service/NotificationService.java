package com.example.simpleoa.service;

import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.model.ReimbursementRequest;

public interface NotificationService {
    
    // 请假相关通知
    void sendLeaveApplicationNotification(LeaveRequest leaveRequest);
    void sendLeaveApprovalNotification(LeaveRequest leaveRequest);
    void sendLeaveRejectionNotification(LeaveRequest leaveRequest);
    
    // 报销相关通知
    void sendReimbursementApplicationNotification(ReimbursementRequest reimbursementRequest);
    void sendReimbursementApprovalNotification(ReimbursementRequest reimbursementRequest);
    void sendReimbursementRejectionNotification(ReimbursementRequest reimbursementRequest);
    
    // 通用通知
    void sendEmail(String to, String subject, String content);
    void sendSystemNotification(Long userId, String title, String content);
}
