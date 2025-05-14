package com.example.simpleoa.service;

import com.example.simpleoa.model.LeaveRequest;
import org.springframework.stereotype.Service;

@Service
public interface LeaveService {
    LeaveRequest createLeave(LeaveRequest leaveRequest);
    LeaveRequest updateLeave(LeaveRequest leaveRequest);
    void deleteLeave(Long id);
    LeaveRequest getLeaveById(Long id);
    Iterable<LeaveRequest> getAllLeaves();
    Iterable<LeaveRequest> getLeavesByUser(Long userId);
    LeaveRequest approveLeave(Long id, String status, String comment);
}