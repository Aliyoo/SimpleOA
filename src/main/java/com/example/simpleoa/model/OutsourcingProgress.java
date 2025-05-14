package com.example.simpleoa.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "outsourcing_progress")
public class OutsourcingProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "outsourcing_id", nullable = false)
    private Outsourcing outsourcing;

    @Column(nullable = false)
    private Date reportDate;

    @Column(nullable = false)
    private Integer completionPercentage;

    @Column(columnDefinition = "TEXT")
    private String progressDescription;

    @Column
    private String milestone;

    @Column
    private String issues;

    @Column
    private String nextSteps;

    @ManyToOne
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Outsourcing getOutsourcing() {
        return outsourcing;
    }

    public void setOutsourcing(Outsourcing outsourcing) {
        this.outsourcing = outsourcing;
    }

    public Date getReportDate() {
        return reportDate;
    }

    public void setReportDate(Date reportDate) {
        this.reportDate = reportDate;
    }

    public Integer getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Integer completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public String getProgressDescription() {
        return progressDescription;
    }

    public void setProgressDescription(String progressDescription) {
        this.progressDescription = progressDescription;
    }

    public String getMilestone() {
        return milestone;
    }

    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }

    public String getIssues() {
        return issues;
    }

    public void setIssues(String issues) {
        this.issues = issues;
    }

    public String getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(String nextSteps) {
        this.nextSteps = nextSteps;
    }

    public User getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(User reportedBy) {
        this.reportedBy = reportedBy;
    }
}
