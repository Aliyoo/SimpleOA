package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String paymentId;
    private BigDecimal amount;
    private String payer;
    private LocalDate paymentDate;
    private String description;
    private String status; // PENDING, PAID, CANCELLED

    @ManyToOne
    private ReimbursementRequest reimbursementRequest;

    @ManyToOne
    private User processor;
}