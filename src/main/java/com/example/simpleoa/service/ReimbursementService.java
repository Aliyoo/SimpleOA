package com.example.simpleoa.service;

import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.ReimbursementRequestDTO;
import com.example.simpleoa.model.ReimbursementStatus;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface ReimbursementService {
    ReimbursementRequest createReimbursement(ReimbursementRequestDTO dto, Long applicantId);
    ReimbursementRequest updateReimbursement(Long id, ReimbursementRequestDTO dto);
    void deleteReimbursement(Long id);
    ReimbursementRequest getReimbursementById(Long id);
    Page<ReimbursementRequest> getReimbursements(Long userId, int page, int size, ReimbursementStatus status, String startDate, String endDate, String keyword);
    ReimbursementRequest approveOrReject(Long id, String decision, String comment, Long approverId);
    Map<String, Object> getReimbursementStatistics(String startDate, String endDate, Long userId);
}
