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
     * Link a reimbursement request to a payment record
     * 
     * @param reimbursementId ID of the reimbursement request
     * @param paymentId ID of the payment record
     * @return Map containing the linked reimbursement and payment objects
     */
    Map<String, Object> linkToPayment(Long reimbursementId, Long paymentId);
}