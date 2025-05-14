package com.example.simpleoa.model;

import jakarta.persistence.*;

@Entity
@Table(name = "performance_evaluation_item")
public class PerformanceEvaluationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "evaluation_id", nullable = false)
    private PerformanceEvaluation evaluation;

    @ManyToOne
    @JoinColumn(name = "criteria_id", nullable = false)
    private PerformanceCriteria criteria;

    @Column(nullable = false)
    private Double score;

    @Column
    private Double weightedScore;

    @Column(columnDefinition = "TEXT")
    private String comments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PerformanceEvaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(PerformanceEvaluation evaluation) {
        this.evaluation = evaluation;
    }

    public PerformanceCriteria getCriteria() {
        return criteria;
    }

    public void setCriteria(PerformanceCriteria criteria) {
        this.criteria = criteria;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getWeightedScore() {
        return weightedScore;
    }

    public void setWeightedScore(Double weightedScore) {
        this.weightedScore = weightedScore;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
