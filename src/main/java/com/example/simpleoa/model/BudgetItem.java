package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Table(name = "budget_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double amount;

    @Column
    private Double usedAmount;

    @Column
    private Double remainingAmount;

    @Column
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateTime;
}
