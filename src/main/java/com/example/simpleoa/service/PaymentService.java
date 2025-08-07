package com.example.simpleoa.service;

import com.example.simpleoa.model.Payment;
import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public interface PaymentService {
    
    void recordPayment(Map<String, Object> paymentData);
    
    List<?> getPaymentStatistics(String startDate, String endDate);
    
    List<?> getPaymentReminderList();
    
    void markAsPaid(Long id);
    
    List<Payment> getPaymentsByReimbursementRequestId(Long reimbursementRequestId);
    
    Payment createReimbursementPayment(ReimbursementRequest reimbursementRequest, User processor);
}