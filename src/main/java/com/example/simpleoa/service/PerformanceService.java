package com.example.simpleoa.service;

import com.example.simpleoa.model.PerformanceBonus;
import com.example.simpleoa.model.PerformanceCriteria;
import com.example.simpleoa.model.PerformanceEvaluation;
import com.example.simpleoa.model.PerformanceEvaluationItem;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface PerformanceService {
    // 绩效考核标准管理
    PerformanceCriteria createCriteria(PerformanceCriteria criteria);
    PerformanceCriteria updateCriteria(PerformanceCriteria criteria);
    void deleteCriteria(Long id);
    PerformanceCriteria getCriteriaById(Long id);
    List<PerformanceCriteria> getAllCriteria();
    List<PerformanceCriteria> getCriteriaByCategory(String category);
    List<PerformanceCriteria> getActiveCriteria();
    
    // 绩效评估管理
    PerformanceEvaluation createEvaluation(PerformanceEvaluation evaluation);
    PerformanceEvaluation updateEvaluation(PerformanceEvaluation evaluation);
    void deleteEvaluation(Long id);
    PerformanceEvaluation getEvaluationById(Long id);
    List<PerformanceEvaluation> getAllEvaluations();
    List<PerformanceEvaluation> getEvaluationsByUser(Long userId);
    List<PerformanceEvaluation> getEvaluationsByPeriod(String evaluationPeriod);
    List<PerformanceEvaluation> getEvaluationsByStatus(String status);
    List<PerformanceEvaluation> getEvaluationsByDateRange(Date startDate, Date endDate);
    List<PerformanceEvaluation> getEvaluationsByEvaluator(Long evaluatedById);
    List<PerformanceEvaluation> getLatestEvaluationsByUser(Long userId);
    Double getAverageScoreByUser(Long userId);
    
    // 绩效评估明细管理
    PerformanceEvaluationItem createEvaluationItem(PerformanceEvaluationItem item);
    PerformanceEvaluationItem updateEvaluationItem(PerformanceEvaluationItem item);
    void deleteEvaluationItem(Long id);
    PerformanceEvaluationItem getEvaluationItemById(Long id);
    List<PerformanceEvaluationItem> getEvaluationItemsByEvaluation(Long evaluationId);
    List<PerformanceEvaluationItem> getEvaluationItemsByCriteria(Long criteriaId);
    Double getAverageScoreByCriteria(Long criteriaId);
    Double getAverageScoreByUserAndCriteria(Long userId, Long criteriaId);
    
    // 绩效计算
    PerformanceEvaluation calculatePerformance(Long evaluationId);
    Map<String, Object> calculatePerformanceByUser(Long userId, String period, Date startDate, Date endDate);
    Map<String, Object> calculateTeamPerformance(List<Long> userIds, String period, Date startDate, Date endDate);
    
    // 绩效奖金管理
    PerformanceBonus createBonus(PerformanceBonus bonus);
    PerformanceBonus updateBonus(PerformanceBonus bonus);
    void deleteBonus(Long id);
    PerformanceBonus getBonusById(Long id);
    List<PerformanceBonus> getAllBonuses();
    List<PerformanceBonus> getBonusesByUser(Long userId);
    List<PerformanceBonus> getBonusesByEvaluation(Long evaluationId);
    List<PerformanceBonus> getBonusesByPeriod(String bonusPeriod);
    List<PerformanceBonus> getBonusesByStatus(String status);
    List<PerformanceBonus> getBonusesByDateRange(Date startDate, Date endDate);
    List<PerformanceBonus> getBonusesByType(String bonusType);
    Double getTotalBonusAmountByUser(Long userId);
    Double getTotalBonusAmountByPeriod(String bonusPeriod);
    Double getTotalBonusAmountByDateRange(Date startDate, Date endDate);
    
    // 绩效发放
    PerformanceBonus approveBonus(Long bonusId, Long approvedById);
    PerformanceBonus distributeBonus(Long bonusId, Date paymentDate);
    Map<String, Object> generateBonusDistributionPlan(String period, Date startDate, Date endDate);
    
    // 绩效统计分析
    Map<String, Object> getPerformanceStatsByUser(Long userId);
    Map<String, Object> getPerformanceStatsByPeriod(String period);
    Map<String, Object> getPerformanceStatsByDateRange(Date startDate, Date endDate);
    Map<String, Object> getPerformanceTrend(int months);
    Map<String, Object> getPerformanceDistribution();
}
