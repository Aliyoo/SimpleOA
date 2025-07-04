package com.example.simpleoa.repository;

import com.example.simpleoa.model.ReimbursementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementRequestRepository extends JpaRepository<ReimbursementRequest, Long> {
    Page<ReimbursementRequest> findByApplicantId(Long applicantId, Pageable pageable);
}
