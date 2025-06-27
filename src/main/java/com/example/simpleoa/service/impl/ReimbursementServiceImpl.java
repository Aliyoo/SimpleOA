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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        // 查找合适的审批人（这里简化为查找管理员角色的用户）
        User approver = findApproverForReimbursement(savedRequest);

        if (approver != null) {
            approvalFlowService.createReimbursementApproval(savedRequest, approver);
        }

        return savedRequest;
    }

    /**
     * 查找报销申请的审批人
     * 这里简化实现，实际项目中可能需要根据部门、金额等条件确定审批人
     */
    private User findApproverForReimbursement(ReimbursementRequest request) {
        // 简化实现：查找第一个管理员用户作为审批人
        // 实际项目中应该根据业务规则确定审批人
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            // 假设用户名包含"admin"或"manager"的是管理员
            if (user.getUsername().toLowerCase().contains("admin") ||
                    user.getUsername().toLowerCase().contains("manager")) {
                return user;
            }
        }
        // 如果没找到管理员，返回第一个用户（这只是临时方案）
        return allUsers.isEmpty() ? null : allUsers.get(0);
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
        Iterable<ReimbursementRequest> result = reimbursementRequestRepository.findAll();
        System.out.println("getAllReimbursements - 查询到记录数: " + ((List<ReimbursementRequest>) result).size());
        return result;
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
            
            // 如果审批通过，更新项目预算成本
            if ("APPROVED".equals(status)) {
                // 假设有项目ID与报销申请关联，需要更新项目预算
                // 这里需要调用预算服务来更新成本
                // budgetService.updateProjectCost(request.getProjectId(), request.getAmount());
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

    @Override
    public Map<String, Object> getReimbursementsWithFilter(int page, int size, Long userId, String status,
            String startDate, String endDate) {
        // 创建分页对象，按ID倒序排列（更可靠的排序字段）
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // 处理空字符串参数
        if (status != null && status.trim().isEmpty()) {
            status = null;
        }
        if (startDate != null && startDate.trim().isEmpty()) {
            startDate = null;
        }
        if (endDate != null && endDate.trim().isEmpty()) {
            endDate = null;
        }

        // 转换日期字符串为LocalDate
        LocalDate start = null;
        LocalDate end = null;

        try {
            if (startDate != null) {
                start = LocalDate.parse(startDate);
            }
            if (endDate != null) {
                end = LocalDate.parse(endDate);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误，请使用 yyyy-MM-dd 格式");
        }

        // 添加调试日志
        System.out.println("查询参数 - userId: " + userId + ", status: " + status + ", start: " + start + ", end: " + end);

        // 执行分页查询
        Page<ReimbursementRequest> pageResult = reimbursementRequestRepository.findWithFilters(
                userId, status, start, end, pageable);

        System.out.println(
                "查询结果 - 总记录数: " + pageResult.getTotalElements() + ", 当前页记录数: " + pageResult.getContent().size());

        // 构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", pageResult.getContent());
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("currentPage", pageResult.getNumber());
        result.put("size", pageResult.getSize());
        result.put("hasNext", pageResult.hasNext());
        result.put("hasPrevious", pageResult.hasPrevious());

        return result;
    }
}