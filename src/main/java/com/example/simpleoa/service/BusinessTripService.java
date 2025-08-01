package com.example.simpleoa.service;

import com.example.simpleoa.dto.BusinessTripRequestDTO;
import com.example.simpleoa.model.BusinessTripRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public interface BusinessTripService {
    // 创建出差申请（新的DTO版本）
    BusinessTripRequest createBusinessTrip(BusinessTripRequestDTO dto, Long applicantId);
    
    // 保留原有版本用于兼容性
    BusinessTripRequest createBusinessTrip(BusinessTripRequest businessTripRequest);
    
    // 更新出差申请
    BusinessTripRequest updateBusinessTrip(Long id, BusinessTripRequestDTO dto);
    BusinessTripRequest updateBusinessTrip(BusinessTripRequest businessTripRequest);
    
    // 提交出差申请（从草稿状态提交到待审批状态）
    BusinessTripRequest submitBusinessTrip(Long id);
    
    void deleteBusinessTrip(Long id);
    BusinessTripRequest getBusinessTripById(Long id);
    Iterable<BusinessTripRequest> getAllBusinessTrips();
    Iterable<BusinessTripRequest> getBusinessTripsByUser(Long userId);
    
    // 审批相关方法
    BusinessTripRequest approveBusinessTrip(Long id, String status, String comment);
    BusinessTripRequest rejectBusinessTrip(Long id, String comment);
    
    List<BusinessTripRequest> getMyApplications();
    Object getBusinessTripStatistics(String startDate, String endDate);
    
    // 根据项目获取出差申请
    List<BusinessTripRequest> getBusinessTripsByProject(Long projectId);
}