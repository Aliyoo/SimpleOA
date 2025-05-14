package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "performance_bonus")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceBonus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "evaluation_id")
    private PerformanceEvaluation evaluation;

    @Column(nullable = false)
    private String bonusPeriod;

    @Column(nullable = false)
    private Double amount;

    @Column
    private String bonusType;

    @Column
    private String status;

    @Column
    @Temporal(TemporalType.DATE)
    private Date paymentDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date approvalTime;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
}
