package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.Payment;
import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.PaymentRepository;
import com.example.simpleoa.repository.ProjectRepository;
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

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public void recordPayment(Map<String, Object> paymentData) {
        // 验证支付数据
        if (paymentData == null || paymentData.isEmpty()) {
            throw new IllegalArgumentException("支付数据不能为空");
        }
        
        // 提取并验证必要字段
        Integer projectIdInt = (Integer) paymentData.get("project");
        if (projectIdInt == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        Long projectId = projectIdInt.longValue();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));

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
        
        // 验证支付日期
        String paymentDateStr = (String) paymentData.get("date");
        if (paymentDateStr == null || paymentDateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("支付日期不能为空");
        }
        LocalDate paymentDate = LocalDate.parse(paymentDateStr);
        
        String method = (String) paymentData.get("method");
        String remark = (String) paymentData.get("remark");
        
        // 创建支付记录并保存到数据库
        Payment payment = new Payment();
        payment.setPaymentId("PMT-" + System.currentTimeMillis());
        payment.setAmount(amount);
        payment.setPaymentDate(paymentDate);
        payment.setMethod(method);
        payment.setRemark(remark);
        payment.setProject(project);
        payment.setStatus("PENDING");
        // 'payer' is not provided by frontend, so we can set a default or get from auth context
        payment.setPayer("System");
        
        paymentRepository.save(payment);
    }

    @Override
    public List<?> getPaymentStatistics(String startDate, String endDate) {
        // 验证日期参数
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("日期范围不能为空");
        }
        LocalDate start = LocalDate.parse(startDate + "-01");
        LocalDate end = LocalDate.parse(endDate + "-01").withDayOfMonth(LocalDate.parse(endDate + "-01").lengthOfMonth());

        // 查询指定日期范围内的支付记录
        List<Payment> payments = paymentRepository.findByPaymentDateBetween(start, end);
        
        // 按项目分组
        Map<Project, List<Payment>> paymentsByProject = payments.stream()
                .filter(p -> p.getProject() != null)
                .collect(java.util.stream.Collectors.groupingBy(Payment::getProject));

        // 为每个项目计算统计数据
        List<Map<String, Object>> statisticsList = new ArrayList<>();
        for (Map.Entry<Project, List<Payment>> entry : paymentsByProject.entrySet()) {
            Project project = entry.getKey();
            List<Payment> projectPayments = entry.getValue();

            BigDecimal totalAmount = projectPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int paymentCount = projectPayments.size();

            Map<String, Object> projectStat = new HashMap<>();
            projectStat.put("projectName", project.getName());
            projectStat.put("totalAmount", totalAmount);
            projectStat.put("paymentCount", paymentCount);
            statisticsList.add(projectStat);
        }

        return statisticsList;
    }

    @Override
    public List<?> getPaymentReminderList() {
        List<Payment> pendingPayments = paymentRepository.findPendingPayments();
        List<Map<String, Object>> reminderList = new ArrayList<>();

        for (Payment payment : pendingPayments) {
            Map<String, Object> reminder = new HashMap<>();
            if (payment.getProject() != null) {
                reminder.put("projectName", payment.getProject().getName());
            } else {
                reminder.put("projectName", "N/A");
            }
            reminder.put("id", payment.getId());
            reminder.put("expectedAmount", payment.getAmount());
            reminder.put("expectedDate", payment.getPaymentDate().toString());

            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), payment.getPaymentDate());
            reminder.put("daysLeft", daysLeft);

            if (daysLeft < 0) {
                reminder.put("status", "逾期");
            } else {
                reminder.put("status", "待回款");
            }
            reminderList.add(reminder);
        }

        return reminderList;
    }

    @Override
    public void markAsPaid(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("支付记录不存在"));
            
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