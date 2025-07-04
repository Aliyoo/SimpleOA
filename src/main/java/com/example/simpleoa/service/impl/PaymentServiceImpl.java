package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.Payment;
import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.PaymentRepository;
import com.example.simpleoa.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class PaymentServiceImpl implements PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public void recordPayment(Map<String, Object> paymentData) {
        // 验证支付数据
        if (paymentData == null || paymentData.isEmpty()) {
            throw new IllegalArgumentException("支付数据不能为空");
        }
        
        // 提取并验证必要字段
        String paymentId = (String) paymentData.get("paymentId");
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("支付ID不能为空");
        }
        
        // 验证金额
        Object amountObj = paymentData.get("amount");
        if (amountObj == null) {
            throw new IllegalArgumentException("支付金额不能为空");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountObj.toString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("支付金额必须大于0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("支付金额格式无效");
        }
        
        // 验证付款人
        String payer = (String) paymentData.get("payer");
        if (payer == null || payer.trim().isEmpty()) {
            throw new IllegalArgumentException("付款人不能为空");
        }
        
        // 验证支付日期
        String paymentDate = (String) paymentData.get("paymentDate");
        if (paymentDate == null || paymentDate.trim().isEmpty()) {
            throw new IllegalArgumentException("支付日期不能为空");
        }
        
        String description = (String) paymentData.get("description");
        
        // 创建支付记录并保存到数据库
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setAmount(amount);
        payment.setPayer(payer);
        payment.setPaymentDate(LocalDate.parse(paymentDate));
        payment.setDescription(description);
        payment.setStatus("PENDING");
        
        paymentRepository.save(payment);
    }

    @Override
    public List<?> getPaymentStatistics(String startDate, String endDate) {
        // 验证日期参数
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("日期范围不能为空");
        }
        
        // 查询指定日期范围内的支付记录
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(
            LocalDate.parse(startDate), 
            LocalDate.parse(endDate)
        );
        
        // 统计支付金额和数量
        Map<String, Object> statistics = new HashMap<>();
        BigDecimal totalAmount = payments.stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int paymentCount = payments.size();
        
        statistics.put("totalAmount", totalAmount);
        statistics.put("paymentCount", paymentCount);
        statistics.put("payments", payments);
        
        return Collections.singletonList(statistics);
    }

    @Override
    public List<?> getPaymentReminderList() {
        // 查询状态为"PENDING"且支付日期超过3天的记录
        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
        return paymentRepository.findByStatus("PENDING").stream()
            .filter(payment -> payment.getPaymentDate().isBefore(threeDaysAgo))
            .toList();
    }

    @Override
    public void markAsPaid(String id) {
        Payment payment = paymentRepository.findByPaymentId(id);
        if (payment == null) {
            throw new IllegalArgumentException("支付记录不存在");
        }
            
        payment.setStatus("PAID");
        paymentRepository.save(payment);
    }
    
    @Override
    public List<Payment> getPaymentsByReimbursementRequestId(Long reimbursementRequestId) {
        return paymentRepository.findByReimbursementRequestId(reimbursementRequestId);
    }
    
    @Override
    public Payment createReimbursementPayment(ReimbursementRequest reimbursementRequest, User processor) {
        Payment payment = new Payment();
        payment.setPaymentId("PMT-" + System.currentTimeMillis());
        payment.setAmount(reimbursementRequest.getTotalAmount());
        payment.setPayer(processor.getUsername());
        payment.setPaymentDate(LocalDate.now());
        payment.setDescription("报销: " + reimbursementRequest.getTitle());
        payment.setStatus("PENDING");
        payment.setReimbursementRequest(reimbursementRequest);
        payment.setProcessor(processor);
        
        return paymentRepository.save(payment);
    }
}