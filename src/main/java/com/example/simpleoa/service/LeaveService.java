package com.example.simpleoa.service;

import com.example.simpleoa.model.LeaveBalance;
import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.model.LeaveStatus;
import com.example.simpleoa.model.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface LeaveService {
    // 基础CRUD操作
    LeaveRequest createLeave(LeaveRequest leaveRequest);
    LeaveRequest updateLeave(LeaveRequest leaveRequest);
    void deleteLeave(Long id);
    LeaveRequest getLeaveById(Long id);
    List<LeaveRequest> getAllLeaves();
    List<LeaveRequest> getLeavesByUser(Long userId);
    
    // 申请和审批相关
    LeaveRequest applyLeave(LeaveRequest leaveRequest);
    LeaveRequest approveLeave(Long id, String comment);
    LeaveRequest rejectLeave(Long id, String comment);
    LeaveRequest cancelLeave(Long id);
    
    // 查询相关
    List<LeaveRequest> getApprovalList();
    List<LeaveRequest> getMyApplications();
    Page<LeaveRequest> getMyApplicationsPage(Pageable pageable);
    List<LeaveRequest> getLeavesByStatus(LeaveStatus status);
    List<LeaveRequest> getLeavesByType(LeaveType type);
    
    // 统计相关
    Map<String, Object> getLeaveStatistics(String startDate, String endDate);
    Map<String, Object> getDepartmentStatistics();
    Map<String, Object> getPersonalStatistics(Long userId, Integer year);
    
    // 请假余额相关
    List<LeaveBalance> getUserLeaveBalance(Long userId, Integer year);
    LeaveBalance updateLeaveBalance(Long userId, LeaveType leaveType, Integer year, Double usedDays);
    void initializeUserLeaveBalance(Long userId, Integer year);
    boolean checkLeaveBalance(Long userId, LeaveType leaveType, Integer year, Double requestDays);
    
    // 冲突检查
    boolean hasLeaveConflict(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    // 导出功能
    byte[] exportLeaveRecords(Long userId, String startDate, String endDate);
}
