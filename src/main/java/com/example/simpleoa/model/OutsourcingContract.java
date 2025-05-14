package com.example.simpleoa.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "outsourcing_contract")
public class OutsourcingContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String contractNumber;

    @ManyToOne
    @JoinColumn(name = "outsourcing_id", nullable = false)
    private Outsourcing outsourcing;

    @Column(nullable = false)
    private Date signDate;

    @Column(nullable = false)
    private Date effectiveDate;

    @Column(nullable = false)
    private Date expirationDate;

    @Column(nullable = false)
    private Double totalAmount;

    @Column
    private Double paidAmount;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Column
    private String status;

    @Column(columnDefinition = "TEXT")
    private String attachmentPath;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public Outsourcing getOutsourcing() {
        return outsourcing;
    }

    public void setOutsourcing(Outsourcing outsourcing) {
        this.outsourcing = outsourcing;
    }

    public Date getSignDate() {
        return signDate;
    }

    public void setSignDate(Date signDate) {
        this.signDate = signDate;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }
}
