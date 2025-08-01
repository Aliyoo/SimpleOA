package com.example.simpleoa.service.impl;

import com.example.simpleoa.dto.BusinessTripRequestDTO;
import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.BusinessTripRequestRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.service.ApprovalFlowService;
import com.example.simpleoa.service.BusinessTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    @Autowired
    private ProjectRepository projectRepository;

    @Override
    @Transactional
    public BusinessTripRequest createBusinessTrip(BusinessTripRequestDTO dto, Long applicantId) {
        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + applicantId));

        BusinessTripRequest request = new BusinessTripRequest();
        request.setApplicant(applicant);
        request.setDestination(dto.getDestination());
        request.setStartTime(dto.getStartTime());
        request.setEndTime(dto.getEndTime());
        request.setPurpose(dto.getPurpose());
        request.setDays(dto.getDays());
        request.setComment(dto.getComment());
        request.setStatus(BusinessTripStatus.DRAFT);

        // 处理项目关联
        if (dto.getProjectId() != null) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found for ID: " + dto.getProjectId()));
            request.setProject(project);
        }

        return businessTripRequestRepository.save(request);
    }

    @Override
    @Transactional
    public BusinessTripRequest updateBusinessTrip(Long id, BusinessTripRequestDTO dto) {
        BusinessTripRequest request = getBusinessTripById(id);
        
        // 只有草稿状态的申请才能修改
        if (request.getStatus() != BusinessTripStatus.DRAFT) {
            throw new RuntimeException("只有草稿状态的出差申请才能修改");
        }

        request.setDestination(dto.getDestination());
        request.setStartTime(dto.getStartTime());
        request.setEndTime(dto.getEndTime());
        request.setPurpose(dto.getPurpose());
        request.setDays(dto.getDays());
        request.setComment(dto.getComment());

        // 处理项目关联更新
        if (dto.getProjectId() != null) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found for ID: " + dto.getProjectId()));
            request.setProject(project);
        } else {
            request.setProject(null); // 允许取消项目关联
        }

        return businessTripRequestRepository.save(request);
    }

    @Override
    @Transactional
    public BusinessTripRequest submitBusinessTrip(Long id) {
        BusinessTripRequest request = getBusinessTripById(id);
        
        if (request.getStatus() != BusinessTripStatus.DRAFT) {
            throw new RuntimeException("只有草稿状态的出差申请才能提交");
        }

        // 数据验证
        validateBusinessTripRequest(request);

        // 设置为待审批状态
        request.setStatus(BusinessTripStatus.PENDING);
        BusinessTripRequest savedRequest = businessTripRequestRepository.save(request);

        // 创建审批流程
        if (request.getProject() != null && request.getProject().getManager() != null) {
            approvalFlowService.createBusinessTripApproval(savedRequest, request.getProject().getManager());
        } else {
            throw new RuntimeException("出差申请必须关联项目，且项目必须有指定的项目经理");
        }

        return savedRequest;
    }



    @Override
    @Transactional
    public BusinessTripRequest rejectBusinessTrip(Long id, String comment) {
        BusinessTripRequest request = getBusinessTripById(id);
        
        if (request.getStatus() == BusinessTripStatus.DRAFT || 
            request.getStatus() == BusinessTripStatus.APPROVED || 
            request.getStatus() == BusinessTripStatus.REJECTED) {
            throw new RuntimeException("出差申请当前状态不允许拒绝操作");
        }

        // 设置为已拒绝状态
        request.setStatus(BusinessTripStatus.REJECTED);
        request.setComment(comment);
        BusinessTripRequest savedRequest = businessTripRequestRepository.save(request);

        // 更新当前审批流程状态
        updateCurrentApprovalFlow(id, "REJECTED", comment);

        return savedRequest;
    }

    @Override
    public List<BusinessTripRequest> getBusinessTripsByProject(Long projectId) {
        return businessTripRequestRepository.findByProjectId(projectId);
    }

    private void validateBusinessTripRequest(BusinessTripRequest request) {
        if (request.getDestination() == null || request.getDestination().trim().isEmpty()) {
            throw new IllegalArgumentException("出差地点不能为空");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("出差时间不能为空");
        }
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
        if (request.getPurpose() == null || request.getPurpose().trim().isEmpty()) {
            throw new IllegalArgumentException("出差事由不能为空");
        }
        if (request.getProject() == null) {
            throw new IllegalArgumentException("出差申请必须关联项目");
        }
    }

    private void updateCurrentApprovalFlow(Long businessTripId, String status, String comment) {
        List<ApprovalFlow> flows = approvalFlowService.getApprovalFlowsByBusinessTripRequest(businessTripId);
        if (!flows.isEmpty()) {
            // 更新最新的审批流程状态
            ApprovalFlow latestFlow = flows.get(0); // 假设按创建时间排序，第一个是最新的
            approvalFlowService.updateApprovalFlowStatus(latestFlow.getId(), status, comment);
            
            // 通知状态变更
            approvalFlowService.notifyStatusChange(latestFlow.getId(), status);
        }
    }

    private User getFinanceUser() {
        // 这里应该实现获取财务用户的逻辑
        // 可以通过角色查询或者配置的方式获取
        // 暂时返回第一个用户作为测试
        return userRepository.findAll().iterator().next();
    }

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
        businessTripRequest.setStatus(BusinessTripStatus.PENDING);
        
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
            // 将字符串状态转换为枚举
            BusinessTripStatus businessTripStatus;
            switch (status.toUpperCase()) {
                case "APPROVED":
                    businessTripStatus = BusinessTripStatus.APPROVED;
                    break;
                case "REJECTED":
                    businessTripStatus = BusinessTripStatus.REJECTED;
                    break;
                case "PENDING":
                    businessTripStatus = BusinessTripStatus.PENDING;
                    break;
                default:
                    businessTripStatus = BusinessTripStatus.PENDING;
            }
            
            request.setStatus(businessTripStatus);
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
        summary.put("mostUsedDestination", mostUsedDestination);
        
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
