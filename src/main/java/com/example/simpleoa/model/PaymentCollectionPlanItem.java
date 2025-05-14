package com.example.simpleoa.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "payment_collection_plan_item")
public class PaymentCollectionPlanItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private PaymentCollectionPlan plan;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dueDate;

    @Column
    private String milestone;

    @Column
    private String status;

    @ManyToOne
    @JoinColumn(name = "collection_id")
    private PaymentCollection collection;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PaymentCollectionPlan getPlan() {
        return plan;
    }

    public void setPlan(PaymentCollectionPlan plan) {
        this.plan = plan;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getMilestone() {
        return milestone;
    }

    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PaymentCollection getCollection() {
        return collection;
    }

    public void setCollection(PaymentCollection collection) {
        this.collection = collection;
    }
}
