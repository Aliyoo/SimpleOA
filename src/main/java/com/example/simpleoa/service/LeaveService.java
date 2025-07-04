package com.example.simpleoa.service;

import com.example.simpleoa.model.LeaveRequest;

import java.util.List;

public interface LeaveService {
    LeaveRequest createLeave(LeaveRequest leaveRequest);

    LeaveRequest updateLeave(LeaveRequest leaveRequest);

    void deleteLeave(Long id);

    LeaveRequest getLeaveById(Long id);

    List<LeaveRequest> getAllLeaves();

    List<LeaveRequest> getLeavesByUser(Long userId);

    LeaveRequest approveLeave(Long id, String status, String comment);

    List<LeaveRequest> getApprovalList();

    Object getLeaveStatistics(String startDate, String endDate);

    LeaveRequest applyLeave(LeaveRequest leaveRequest);

    LeaveRequest rejectLeave(Long id, String comment);
}
