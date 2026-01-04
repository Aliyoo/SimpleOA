package com.example.simpleoa.service;

import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.ReimbursementRequestDTO;
import com.example.simpleoa.model.ReimbursementStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ReimbursementService {
    ReimbursementRequest createReimbursement(ReimbursementRequestDTO dto, Long applicantId);
    ReimbursementRequest updateReimbursement(Long id, ReimbursementRequestDTO dto);
    void deleteReimbursement(Long id);
    ReimbursementRequest getReimbursementById(Long id);
    Page<ReimbursementRequest> getReimbursements(Long userId, int page, int size, ReimbursementStatus status, String startDate, String endDate, String keyword);
    ReimbursementRequest submitForApproval(Long id, Long submitterId);
    ReimbursementRequest approveOrReject(Long id, String decision, String comment, Long approverId);
    ReimbursementRequest financeReview(Long id, String decision, String comment, Long financeUserId);
    Page<ReimbursementRequest> getByStatus(ReimbursementStatus status, int page, int size);
    Page<ReimbursementRequest> getByStatuses(List<ReimbursementStatus> statuses, int page, int size);
    Page<ReimbursementRequest> getForFinanceReview(int page, int size);
    Map<String, Object> getReimbursementStatistics(String startDate, String endDate, Long userId);
    Map<String, Object> getFinanceStatistics();
    boolean checkBudgetAvailability(Long reimbursementId);
    boolean validateReimbursementBudget(ReimbursementRequestDTO dto);
}
