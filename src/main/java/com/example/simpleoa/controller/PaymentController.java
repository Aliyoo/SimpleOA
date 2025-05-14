package com.example.simpleoa.controller;

import com.example.simpleoa.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @PostMapping("/record")
    public ResponseEntity<?> recordPayment(@RequestBody Map<String, Object> paymentData) {
        paymentService.recordPayment(paymentData);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<?> getPaymentStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(paymentService.getPaymentStatistics(startDate, endDate));
    }
    
    @GetMapping("/reminder-list")
    public ResponseEntity<?> getPaymentReminderList() {
        return ResponseEntity.ok(paymentService.getPaymentReminderList());
    }
    
    @PostMapping("/mark-paid/{id}")
    public ResponseEntity<?> markAsPaid(@PathVariable String id) {
        paymentService.markAsPaid(id);
        return ResponseEntity.ok().build();
    }
}