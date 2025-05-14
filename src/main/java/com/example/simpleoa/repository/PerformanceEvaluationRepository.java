package com.example.simpleoa.repository;

import com.example.simpleoa.model.PerformanceEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PerformanceEvaluationRepository extends JpaRepository<PerformanceEvaluation, Long> {
    List<PerformanceEvaluation> findByUserId(Long userId);
    List<PerformanceEvaluation> findByEvaluationPeriod(String evaluationPeriod);
    List<PerformanceEvaluation> findByStatus(String status);
    List<PerformanceEvaluation> findByStartDateBetweenOrEndDateBetween(Date startDate1, Date endDate1, Date startDate2, Date endDate2);
    List<PerformanceEvaluation> findByEvaluatedById(Long evaluatedById);
    
    @Query("SELECT AVG(pe.totalScore) FROM PerformanceEvaluation pe WHERE pe.user.id = ?1")
    Double getAverageScoreByUser(Long userId);
    
    @Query("SELECT pe FROM PerformanceEvaluation pe WHERE pe.user.id = ?1 ORDER BY pe.endDate DESC")
    List<PerformanceEvaluation> findLatestEvaluationsByUser(Long userId);
}
