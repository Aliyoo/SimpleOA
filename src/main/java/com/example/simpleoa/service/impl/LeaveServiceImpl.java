package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.LeaveRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Override
    public LeaveRequest createLeave(LeaveRequest leaveRequest) {
        return leaveRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest updateLeave(LeaveRequest leaveRequest) {
        return leaveRepository.save(leaveRequest);
    }

    @Override
    public void deleteLeave(Long id) {
        leaveRepository.deleteById(id);
    }

    @Override
    public LeaveRequest getLeaveById(Long id) {
        return leaveRepository.findById(id).orElse(null);
    }

    @Override
    public List<LeaveRequest> getAllLeaves() {
        return leaveRepository.findAll();
    }

    @Override
    public List<LeaveRequest> getLeavesByUser(Long userId) {
        return leaveRepository.findByApplicantId(userId);
    }

    @Override
    public LeaveRequest approveLeave(Long id, String status, String comment) {
        LeaveRequest leaveRequest = getLeaveById(id);
        if (leaveRequest != null) {
            leaveRequest.setStatus(status);
            leaveRequest.setComment(comment);
            return leaveRepository.save(leaveRequest);
        }
        return null;
    }

    @Override
    public List<LeaveRequest> getApprovalList() {
        // In a real application, this should be more complex, 
        // for example, only return requests that need to be approved by the current user.
        return leaveRepository.findAll();
    }

    @Override
    public List<LeaveRequest> getMyApplications() {
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("未找到当前登录用户");
        }
        
        // 返回当前用户的所有申请
        return leaveRepository.findByApplicantId(currentUser.getId());
    }

    @Override
    public Object getLeaveStatistics(String startDate, String endDate) {
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("未找到当前登录用户");
        }

        // 实现统计逻辑
        // 将字符串日期转换为LocalDateTime
        // 处理月份格式(YYYY-MM)，转换为月份的第一天和最后一天
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        
        if (startDate.length() == 7) { // 格式为 YYYY-MM
            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            startDateTime = LocalDateTime.parse(startDate + "-01 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            // 计算月份的最后一天
            endDateTime = LocalDateTime.parse(endDate + "-01 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    .plusMonths(1).minusDays(1).withHour(23).withMinute(59).withSecond(59);
        } else { // 格式为 YYYY-MM-DD
            startDateTime = LocalDateTime.parse(startDate + " 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            endDateTime = LocalDateTime.parse(endDate + " 23:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        
        List<LeaveRequest> leaves = leaveRepository.findByApplicantIdAndDateRange(currentUser.getId(), startDateTime, endDateTime);

        // 计算汇总信息
        int totalDays = leaves.stream().mapToInt(LeaveRequest::getDurationInDays).sum();
        int totalCount = leaves.size();
        
        // 计算通过率
        long approvedCount = leaves.stream().filter(leave -> "已通过".equals(leave.getStatus())).count();
        String approvalRate = totalCount > 0 ? String.format("%.1f%%", (double)approvedCount / totalCount * 100) : "0%";
        
        // 找出最常用的请假类型
        String mostUsedType = leaves.stream()
                .collect(Collectors.groupingBy(LeaveRequest::getLeaveType, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
        
        // 计算各类型的详细统计
        Map<String, Integer> typeCountMap = leaves.stream()
                .collect(Collectors.groupingBy(LeaveRequest::getLeaveType, Collectors.summingInt(LeaveRequest::getDurationInDays)));
        
        List<Map<String, Object>> details = typeCountMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("type", entry.getKey());
                    detail.put("totalDays", entry.getValue());
                    detail.put("totalCount", leaves.stream().filter(leave -> leave.getLeaveType().equals(entry.getKey())).count());
                    detail.put("percentage", totalDays > 0 ? String.format("%.1f%%", (double)entry.getValue() / totalDays * 100) : "0%");
                    return detail;
                })
                .collect(Collectors.toList());
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        
        // 汇总信息
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDays", totalDays);
        summary.put("totalCount", totalCount);
        summary.put("approvalRate", approvalRate);
        summary.put("mostUsedType", mostUsedType);
        
        result.put("summary", summary);
        result.put("details", details);
        
        return result;
    }

    @Override
    public LeaveRequest applyLeave(LeaveRequest leaveRequest) {
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("未找到当前登录用户");
        }
        
        leaveRequest.setApplicant(currentUser);
        leaveRequest.setStatus("待审批");
        return leaveRepository.save(leaveRequest);
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return userRepository.findByUsername(authentication.getName());
        }
        return null;
    }

    @Override
    public LeaveRequest rejectLeave(Long id, String comment) {
        LeaveRequest leaveRequest = getLeaveById(id);
        if (leaveRequest != null) {
            leaveRequest.setStatus("已拒绝");
            leaveRequest.setComment(comment);
            return leaveRepository.save(leaveRequest);
        }
        return null;
    }
}
