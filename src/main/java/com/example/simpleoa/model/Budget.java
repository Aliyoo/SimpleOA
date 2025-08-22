package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "budget")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column
    private String budgetType;

    @Column(nullable = false)
    private Double totalAmount;

    @Column
    private Double usedAmount;

    @Column
    private Double remainingAmount;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date endDate;

    @Column
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateTime;
}
