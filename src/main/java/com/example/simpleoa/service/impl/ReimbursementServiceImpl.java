package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.Payment;
import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.PaymentRepository;
import com.example.simpleoa.repository.ReimbursementRequestRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.ApprovalFlowService;
import com.example.simpleoa.service.PaymentService;
import com.example.simpleoa.service.ReimbursementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReimbursementServiceImpl implements ReimbursementService {
    @Autowired
    private ReimbursementRequestRepository reimbursementRequestRepository;
    
    @Autowired
    private ApprovalFlowService approvalFlowService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PaymentService paymentService;

    @Override
    public ReimbursementRequest createReimbursement(ReimbursementRequest reimbursementRequest) {
        // 保存报销申请
        reimbursementRequest.setStatus("PENDING");
        ReimbursementRequest savedRequest = reimbursementRequestRepository.save(reimbursementRequest);
        
        // 创建审批流程
        // 这里假设审批人是通过某种方式确定的，例如部门经理或指定的审批人
        User approver = userRepository.findById(reimbursementRequest.getApplicant().getId())
                .orElseThrow(() -> new IllegalArgumentException("Approver not found"));
        
        approvalFlowService.createReimbursementApproval(savedRequest, approver);
        
        return savedRequest;
    }

    @Override
    public ReimbursementRequest updateReimbursement(ReimbursementRequest reimbursementRequest) {
        return reimbursementRequestRepository.save(reimbursementRequest);
    }

    @Override
    public void deleteReimbursement(Long id) {
        reimbursementRequestRepository.deleteById(id);
    }

    @Override
    public ReimbursementRequest getReimbursementById(Long id) {
        return reimbursementRequestRepository.findById(id).orElse(null);
    }

    @Override
    public Iterable<ReimbursementRequest> getAllReimbursements() {
        return reimbursementRequestRepository.findAll();
    }

    @Override
    public Iterable<ReimbursementRequest> getReimbursementsByUser(Long userId) {
        return reimbursementRequestRepository.findByUserId(userId);
    }

    @Override
    public ReimbursementRequest approveReimbursement(Long id, String status, String comment) {
        ReimbursementRequest request = getReimbursementById(id);
        if (request != null) {
            request.setStatus(status);
            request.setComment(comment);
            ReimbursementRequest savedRequest = reimbursementRequestRepository.save(request);
            
            // 更新审批流程状态
            // 获取与此报销申请相关的审批流程
            List<ApprovalFlow> flows = approvalFlowService.getApprovalFlowsByReimbursementRequest(id);
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
    @Transactional
    public Map<String, Object> linkToPayment(Long reimbursementId, Long paymentId) {
        // Retrieve the reimbursement request
        ReimbursementRequest reimbursement = reimbursementRequestRepository.findById(reimbursementId)
                .orElseThrow(() -> new IllegalArgumentException("Reimbursement not found with id: " + reimbursementId));
        
        // Retrieve the payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + paymentId));
        
        // Validate the status of reimbursement
        if (!"APPROVED".equals(reimbursement.getStatus())) {
            throw new IllegalStateException("Cannot link payment to a reimbursement that is not approved");
        }
        
        // Update the payment with reimbursement information
        payment.setReimbursementRequest(reimbursement);
        //payment.setLastUpdateTime(new Date());
        payment.setDescription("Payment for reimbursement #" + reimbursementId + ": " + reimbursement.getDescription());
        
        // If payment amount is not set, use the reimbursement amount
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            payment.setAmount(reimbursement.getAmount());
        }
        
        // Save the updated payment
        Payment savedPayment = paymentRepository.save(payment);
        
        // Update the reimbursement with payment reference
        //reimbursement.setPaymentId(paymentId);
        //reimbursement.setPaymentStatus("LINKED");
        //reimbursement.setLastUpdateTime(new Date());
        ReimbursementRequest savedReimbursement = reimbursementRequestRepository.save(reimbursement);
        
        // Return both updated entities
        Map<String, Object> result = new HashMap<>();
        result.put("reimbursement", savedReimbursement);
        result.put("payment", savedPayment);
        result.put("linkedAt", new Date());
        result.put("status", "SUCCESS");
        
        return result;
    }
}