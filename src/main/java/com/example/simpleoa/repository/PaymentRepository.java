package com.example.simpleoa.repository;

import com.example.simpleoa.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStatus(String status);
    
    List<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Payment> findByReimbursementRequestId(Long reimbursementRequestId);
    
    List<Payment> findByProcessorId(Long processorId);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' ORDER BY p.paymentDate ASC")
    List<Payment> findPendingPayments();
    
    Payment findByPaymentId(String paymentId);
}