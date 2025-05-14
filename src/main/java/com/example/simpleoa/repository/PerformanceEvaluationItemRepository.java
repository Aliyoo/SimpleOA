package com.example.simpleoa.repository;

import com.example.simpleoa.model.PerformanceEvaluationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceEvaluationItemRepository extends JpaRepository<PerformanceEvaluationItem, Long> {
    List<PerformanceEvaluationItem> findByEvaluationId(Long evaluationId);
    List<PerformanceEvaluationItem> findByCriteriaId(Long criteriaId);
    
    @Query("SELECT AVG(pei.score) FROM PerformanceEvaluationItem pei WHERE pei.criteria.id = ?1")
    Double getAverageScoreByCriteria(Long criteriaId);
    
    @Query("SELECT AVG(pei.score) FROM PerformanceEvaluationItem pei WHERE pei.evaluation.user.id = ?1 AND pei.criteria.id = ?2")
    Double getAverageScoreByUserAndCriteria(Long userId, Long criteriaId);
}
