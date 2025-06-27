package com.example.simpleoa.service;

import com.example.simpleoa.model.ReimbursementRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface ReimbursementService {
    ReimbursementRequest createReimbursement(ReimbursementRequest reimbursementRequest);
    ReimbursementRequest updateReimbursement(ReimbursementRequest reimbursementRequest);
    void deleteReimbursement(Long id);
    ReimbursementRequest getReimbursementById(Long id);
    Iterable<ReimbursementRequest> getAllReimbursements();
    Iterable<ReimbursementRequest> getReimbursementsByUser(Long userId);
    ReimbursementRequest approveReimbursement(Long id, String status, String comment);

    /**
     * 分页查询报销申请，支持过滤条件
     *
     * @param page      页码（从0开始）
     * @param size      每页大小
     * @param userId    用户ID（可选）
     * @param status    状态（可选）
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @return 包含分页数据的Map
     */
    Map<String, Object> getReimbursementsWithFilter(int page, int size, Long userId, String status, String startDate,
            String endDate);

    /**
     * Link a reimbursement request to a payment record
     *
     * @param reimbursementId ID of the reimbursement request
     * @param paymentId       ID of the payment record
     * @return Map containing the linked reimbursement and payment objects
     */
    Map<String, Object> linkToPayment(Long reimbursementId, Long paymentId);
}