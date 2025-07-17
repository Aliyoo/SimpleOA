package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.BusinessTripRequest;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.BusinessTripRequestRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.ApprovalFlowService;
import com.example.simpleoa.service.BusinessTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class BusinessTripServiceImpl implements BusinessTripService {
    @Autowired
    private BusinessTripRequestRepository businessTripRequestRepository;
    
    @Autowired
    private ApprovalFlowService approvalFlowService;
    
    @Autowired
    private UserRepository userRepository;

    @Override
    public BusinessTripRequest createBusinessTrip(BusinessTripRequest businessTripRequest) {
        // 基本数据验证
        if (businessTripRequest.getDestination() == null || businessTripRequest.getDestination().trim().isEmpty()) {
            throw new IllegalArgumentException("出差地点不能为空");
        }
        if (businessTripRequest.getStartTime() == null || businessTripRequest.getEndTime() == null) {
            throw new IllegalArgumentException("出差时间不能为空");
        }
        if (businessTripRequest.getStartTime().isAfter(businessTripRequest.getEndTime())) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
        
        // 设置默认状态
        businessTripRequest.setStatus("PENDING");
        
        // 如果没有设置申请人，需要从当前用户获取
        if (businessTripRequest.getApplicant() == null) {
            // 这里需要获取当前登录用户，实际项目中应该从SecurityContext获取
            // 暂时使用第一个用户作为申请人
            User currentUser = userRepository.findAll().iterator().next();
            businessTripRequest.setApplicant(currentUser);
        }
        
        // 保存出差申请
        BusinessTripRequest savedRequest = businessTripRequestRepository.save(businessTripRequest);
        
        // 创建审批流程
        // 这里假设审批人是通过某种方式确定的，例如部门经理或指定的审批人
        // 在实际应用中，可能需要根据业务规则或配置来确定审批人
        User approver = userRepository.findAll().iterator().next(); // 暂时使用第一个用户作为审批人
        
        approvalFlowService.createBusinessTripApproval(savedRequest, approver);
        
        return savedRequest;
    }

    @Override
    public BusinessTripRequest updateBusinessTrip(BusinessTripRequest businessTripRequest) {
        return businessTripRequestRepository.save(businessTripRequest);
    }

    @Override
    public void deleteBusinessTrip(Long id) {
        businessTripRequestRepository.deleteById(id);
    }

    @Override
    public BusinessTripRequest getBusinessTripById(Long id) {
        return businessTripRequestRepository.findById(id).orElse(null);
    }

    @Override
    public Iterable<BusinessTripRequest> getAllBusinessTrips() {
        return businessTripRequestRepository.findAll();
    }

    @Override
    public Iterable<BusinessTripRequest> getBusinessTripsByUser(Long userId) {
        return businessTripRequestRepository.findByUserId(userId);
    }

    @Override
    public BusinessTripRequest approveBusinessTrip(Long id, String status, String comment) {
        BusinessTripRequest request = getBusinessTripById(id);
        if (request != null) {
            request.setStatus(status);
            request.setComment(comment);
            BusinessTripRequest savedRequest = businessTripRequestRepository.save(request);
            
            // 更新审批流程状态
            // 获取与此出差申请相关的审批流程
            List<ApprovalFlow> flows = approvalFlowService.getApprovalFlowsByBusinessTripRequest(id);
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
    
    @Override
    public List<BusinessTripRequest> getMyApplications() {
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("未找到当前登录用户");
        }
        
        // 返回当前用户的所有申请
        return businessTripRequestRepository.findByApplicantId(currentUser.getId());
    }
    
    @Override
    public Object getBusinessTripStatistics(String startDate, String endDate) {
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
            startDateTime = LocalDateTime.parse(startDate + "-01 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            // 计算月份的最后一天
            endDateTime = LocalDateTime.parse(endDate + "-01 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    .plusMonths(1).minusDays(1).withHour(23).withMinute(59).withSecond(59);
        } else { // 格式为 YYYY-MM-DD
            startDateTime = LocalDateTime.parse(startDate + " 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            endDateTime = LocalDateTime.parse(endDate + " 23:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        
        List<BusinessTripRequest> trips = businessTripRequestRepository.findByApplicantIdAndDateRange(currentUser.getId(), startDateTime, endDateTime);
        
        // 计算汇总信息
        int totalDays = trips.stream().mapToInt(BusinessTripRequest::getDurationInDays).sum();
        int totalCount = trips.size();
        
        // 计算通过率
        long approvedCount = trips.stream().filter(trip -> "APPROVED".equals(trip.getStatus())).count();
        String approvalRate = totalCount > 0 ? String.format("%.1f%%", (double)approvedCount / totalCount * 100) : "0%";
        
        // 找出最常用的目的地
        String mostUsedDestination = trips.stream()
                .collect(Collectors.groupingBy(BusinessTripRequest::getDestination, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
        
        // 计算各目的地的详细统计
        Map<String, Integer> destinationCountMap = trips.stream()
                .collect(Collectors.groupingBy(BusinessTripRequest::getDestination, Collectors.summingInt(BusinessTripRequest::getDurationInDays)));
        
        List<Map<String, Object>> details = destinationCountMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("destination", entry.getKey());
                    detail.put("totalDays", entry.getValue());
                    detail.put("totalCount", trips.stream().filter(trip -> trip.getDestination().equals(entry.getKey())).count());
                    detail.put("percentage", totalDays > 0 ? String.format("%.1f%%", (double)entry.getValue() / totalDays * 100) : "0%");
                    // 计算总预算
                    double totalBudget = trips.stream()
                            .filter(trip -> trip.getDestination().equals(entry.getKey()))
                            .mapToDouble(trip -> trip.getEstimatedCost() != null ? trip.getEstimatedCost().doubleValue() : 0.0)
                            .sum();
                    detail.put("totalBudget", totalBudget);
                    return detail;
                })
                .collect(Collectors.toList());
        
        // 计算总预算
        double totalBudget = trips.stream()
                .mapToDouble(trip -> trip.getEstimatedCost() != null ? trip.getEstimatedCost().doubleValue() : 0.0)
                .sum();
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        
        // 汇总信息
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDays", totalDays);
        summary.put("totalCount", totalCount);
        summary.put("approvalRate", approvalRate);
        summary.put("mostUsedDestination", mostUsedDestination);
        summary.put("totalBudget", totalBudget);
        
        result.put("summary", summary);
        result.put("details", details);
        
        return result;
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return userRepository.findByUsername(authentication.getName());
        }
        // 如果没有安全上下文，返回第一个用户用于测试
        return userRepository.findAll().iterator().next();
    }
}
