package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Table(name = "budget_expense")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetExpense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne
    @JoinColumn(name = "budget_item_id")
    private BudgetItem budgetItem;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date expenseDate;

    @Column(nullable = false)
    private String expenseType;

    @Column
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reimbursement_request_id")
    private ReimbursementRequest reimbursementRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reimbursement_item_id")
    private ReimbursementItem reimbursementItem;

    @Column
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date recordTime;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateTime;
}
