package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "payment_collection")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date collectionDate;

    @Column
    private String collectionMethod;

    @Column
    private String paymentReference;

    @Column
    private String status;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date recordTime;
}
