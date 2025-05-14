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
public class BusinessTripRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String purpose;
    private BigDecimal estimatedCost;
    private String status;
    private String comment;

    @ManyToOne
    private User applicant;

    public Long getUserId() {
        return applicant != null ? applicant.getId() : null;
    }
}