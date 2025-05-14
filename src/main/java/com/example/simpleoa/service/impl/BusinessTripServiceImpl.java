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
        // 保存出差申请
        businessTripRequest.setStatus("PENDING");
        BusinessTripRequest savedRequest = businessTripRequestRepository.save(businessTripRequest);
        
        // 创建审批流程
        // 这里假设审批人是通过某种方式确定的，例如部门经理或指定的审批人
        // 在实际应用中，可能需要根据业务规则或配置来确定审批人
        User approver = userRepository.findById(businessTripRequest.getApplicant().getId())
                .orElseThrow(() -> new IllegalArgumentException("Approver not found"));
        
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
}