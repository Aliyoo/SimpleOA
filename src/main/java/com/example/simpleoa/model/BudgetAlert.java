package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Table(name = "budget_alert")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAlert {
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
    private String alertType;

    @Column(nullable = false)
    private String alertLevel;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date alertDate;

    @Column
    private String status;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date resolvedDate;

    @ManyToOne
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateTime;
}
