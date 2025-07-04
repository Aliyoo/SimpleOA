package com.example.simpleoa.service;

import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.ReimbursementRequestDTO;
import org.springframework.data.domain.Page;

public interface ReimbursementService {
    ReimbursementRequest createReimbursement(ReimbursementRequestDTO dto, Long applicantId);
    ReimbursementRequest updateReimbursement(Long id, ReimbursementRequestDTO dto);
    void deleteReimbursement(Long id);
    ReimbursementRequest getReimbursementById(Long id);
    Page<ReimbursementRequest> getReimbursements(Long userId, int page, int size);
    ReimbursementRequest approveOrReject(Long id, String decision, String comment, Long approverId);
}
