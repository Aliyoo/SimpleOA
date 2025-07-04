package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.ReimbursementRequestRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.ReimbursementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReimbursementServiceImpl implements ReimbursementService {

    private final ReimbursementRequestRepository reimbursementRequestRepository;
    private final UserRepository userRepository;

    public ReimbursementServiceImpl(ReimbursementRequestRepository reimbursementRequestRepository, UserRepository userRepository) {
        this.reimbursementRequestRepository = reimbursementRequestRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ReimbursementRequest createReimbursement(ReimbursementRequestDTO dto, Long applicantId) {
        User applicant = userRepository.findById(applicantId).orElseThrow(() -> new RuntimeException("User not found"));

        ReimbursementRequest request = new ReimbursementRequest();
        request.setApplicant(applicant);
        request.setTitle(dto.getTitle());
        request.setAttachments(dto.getAttachments());
        request.setStatus(ReimbursementStatus.DRAFT);

        if (dto.getItems() != null) {
            dto.getItems().forEach(item -> item.setReimbursementRequest(request));
            request.setItems(dto.getItems());
            request.calculateTotalAmount();
        }

        return reimbursementRequestRepository.save(request);
    }

    @Override
    @Transactional
    public ReimbursementRequest updateReimbursement(Long id, ReimbursementRequestDTO dto) {
        ReimbursementRequest request = getReimbursementById(id);
        // Add authorization logic here to ensure only the applicant can update in DRAFT status

        request.setTitle(dto.getTitle());
        request.setAttachments(dto.getAttachments());

        if (dto.getItems() != null) {
            request.getItems().clear();
            dto.getItems().forEach(item -> item.setReimbursementRequest(request));
            request.getItems().addAll(dto.getItems());
            request.calculateTotalAmount();
        }

        return reimbursementRequestRepository.save(request);
    }

    @Override
    public void deleteReimbursement(Long id) {
        reimbursementRequestRepository.deleteById(id);
    }

    @Override
    public ReimbursementRequest getReimbursementById(Long id) {
        return reimbursementRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Reimbursement request not found"));
    }

    @Override
    public Page<ReimbursementRequest> getReimbursements(Long userId, int page, int size) {
        return reimbursementRequestRepository.findByApplicantId(userId, PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public ReimbursementRequest approveOrReject(Long id, String decision, String comment, Long approverId) {
        ReimbursementRequest request = getReimbursementById(id);
        User approver = userRepository.findById(approverId).orElseThrow(() -> new RuntimeException("User not found"));

        boolean isManager = approver.getRoles().stream().anyMatch(role -> "ROLE_MANAGER".equals(role.getName()));
        boolean isFinance = approver.getRoles().stream().anyMatch(role -> "ROLE_FINANCE".equals(role.getName()));

        switch (request.getStatus()) {
            case PENDING_MANAGER_APPROVAL:
                if (!isManager) {
                    throw new SecurityException("User does not have permission to approve as a manager.");
                }
                if ("approve".equalsIgnoreCase(decision)) {
                    request.setStatus(ReimbursementStatus.PENDING_FINANCE_APPROVAL);
                } else {
                    request.setStatus(ReimbursementStatus.REJECTED);
                }
                break;
            case PENDING_FINANCE_APPROVAL:
                if (!isFinance) {
                    throw new SecurityException("User does not have permission to approve as finance.");
                }
                if ("approve".equalsIgnoreCase(decision)) {
                    request.setStatus(ReimbursementStatus.APPROVED);
                } else {
                    request.setStatus(ReimbursementStatus.REJECTED);
                }
                break;
            default:
                throw new IllegalStateException("Request is not in a state that can be approved or rejected.");
        }

        request.setComment(comment);
        return reimbursementRequestRepository.save(request);
    }
}
