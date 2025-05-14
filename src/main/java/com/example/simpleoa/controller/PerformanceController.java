package com.example.simpleoa.controller;

import com.example.simpleoa.model.PerformanceBonus;
import com.example.simpleoa.model.PerformanceCriteria;
import com.example.simpleoa.model.PerformanceEvaluation;
import com.example.simpleoa.model.PerformanceEvaluationItem;
import com.example.simpleoa.service.PerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {
    private final PerformanceService performanceService;

    @Autowired
    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    // 绩效考核标准管理
    @PostMapping("/criteria")
    public PerformanceCriteria createCriteria(@RequestBody PerformanceCriteria criteria) {
        return performanceService.createCriteria(criteria);
    }

    @PutMapping("/criteria/{id}")
    public PerformanceCriteria updateCriteria(@PathVariable Long id, @RequestBody PerformanceCriteria criteria) {
        criteria.setId(id);
        return performanceService.updateCriteria(criteria);
    }

    @DeleteMapping("/criteria/{id}")
    public void deleteCriteria(@PathVariable Long id) {
        performanceService.deleteCriteria(id);
    }

    @GetMapping("/criteria/{id}")
    public PerformanceCriteria getCriteriaById(@PathVariable Long id) {
        return performanceService.getCriteriaById(id);
    }

    @GetMapping("/criteria")
    public List<PerformanceCriteria> getAllCriteria() {
        return performanceService.getAllCriteria();
    }

    @GetMapping("/criteria/category/{category}")
    public List<PerformanceCriteria> getCriteriaByCategory(@PathVariable String category) {
        return performanceService.getCriteriaByCategory(category);
    }

    @GetMapping("/criteria/active")
    public List<PerformanceCriteria> getActiveCriteria() {
        return performanceService.getActiveCriteria();
    }

    // 绩效评估管理
    @PostMapping("/evaluations")
    public PerformanceEvaluation createEvaluation(@RequestBody PerformanceEvaluation evaluation) {
        return performanceService.createEvaluation(evaluation);
    }

    @PutMapping("/evaluations/{id}")
    public PerformanceEvaluation updateEvaluation(@PathVariable Long id, @RequestBody PerformanceEvaluation evaluation) {
        evaluation.setId(id);
        return performanceService.updateEvaluation(evaluation);
    }

    @DeleteMapping("/evaluations/{id}")
    public void deleteEvaluation(@PathVariable Long id) {
        performanceService.deleteEvaluation(id);
    }

    @GetMapping("/evaluations/{id}")
    public PerformanceEvaluation getEvaluationById(@PathVariable Long id) {
        return performanceService.getEvaluationById(id);
    }

    @GetMapping("/evaluations")
    public List<PerformanceEvaluation> getAllEvaluations() {
        return performanceService.getAllEvaluations();
    }

    @GetMapping("/evaluations/user/{userId}")
    public List<PerformanceEvaluation> getEvaluationsByUser(@PathVariable Long userId) {
        return performanceService.getEvaluationsByUser(userId);
    }

    @GetMapping("/evaluations/period/{period}")
    public List<PerformanceEvaluation> getEvaluationsByPeriod(@PathVariable String period) {
        return performanceService.getEvaluationsByPeriod(period);
    }

    @GetMapping("/evaluations/status/{status}")
    public List<PerformanceEvaluation> getEvaluationsByStatus(@PathVariable String status) {
        return performanceService.getEvaluationsByStatus(status);
    }

    @GetMapping("/evaluations/date-range")
    public List<PerformanceEvaluation> getEvaluationsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return performanceService.getEvaluationsByDateRange(startDate, endDate);
    }

    @GetMapping("/evaluations/evaluator/{evaluatorId}")
    public List<PerformanceEvaluation> getEvaluationsByEvaluator(@PathVariable Long evaluatorId) {
        return performanceService.getEvaluationsByEvaluator(evaluatorId);
    }

    @GetMapping("/evaluations/user/{userId}/latest")
    public List<PerformanceEvaluation> getLatestEvaluationsByUser(@PathVariable Long userId) {
        return performanceService.getLatestEvaluationsByUser(userId);
    }

    @GetMapping("/evaluations/user/{userId}/average-score")
    public Double getAverageScoreByUser(@PathVariable Long userId) {
        return performanceService.getAverageScoreByUser(userId);
    }

    // 绩效评估明细管理
    @PostMapping("/evaluations/{evaluationId}/items")
    public PerformanceEvaluationItem createEvaluationItem(
            @PathVariable Long evaluationId,
            @RequestBody PerformanceEvaluationItem item) {
        PerformanceEvaluation evaluation = new PerformanceEvaluation();
        evaluation.setId(evaluationId);
        item.setEvaluation(evaluation);
        return performanceService.createEvaluationItem(item);
    }

    @PutMapping("/evaluation-items/{id}")
    public PerformanceEvaluationItem updateEvaluationItem(
            @PathVariable Long id,
            @RequestBody PerformanceEvaluationItem item) {
        item.setId(id);
        return performanceService.updateEvaluationItem(item);
    }

    @DeleteMapping("/evaluation-items/{id}")
    public void deleteEvaluationItem(@PathVariable Long id) {
        performanceService.deleteEvaluationItem(id);
    }

    @GetMapping("/evaluation-items/{id}")
    public PerformanceEvaluationItem getEvaluationItemById(@PathVariable Long id) {
        return performanceService.getEvaluationItemById(id);
    }

    @GetMapping("/evaluations/{evaluationId}/items")
    public List<PerformanceEvaluationItem> getEvaluationItemsByEvaluation(@PathVariable Long evaluationId) {
        return performanceService.getEvaluationItemsByEvaluation(evaluationId);
    }

    @GetMapping("/criteria/{criteriaId}/items")
    public List<PerformanceEvaluationItem> getEvaluationItemsByCriteria(@PathVariable Long criteriaId) {
        return performanceService.getEvaluationItemsByCriteria(criteriaId);
    }

    @GetMapping("/criteria/{criteriaId}/average-score")
    public Double getAverageScoreByCriteria(@PathVariable Long criteriaId) {
        return performanceService.getAverageScoreByCriteria(criteriaId);
    }

    @GetMapping("/user/{userId}/criteria/{criteriaId}/average-score")
    public Double getAverageScoreByUserAndCriteria(
            @PathVariable Long userId,
            @PathVariable Long criteriaId) {
        return performanceService.getAverageScoreByUserAndCriteria(userId, criteriaId);
    }

    // 绩效计算
    @PostMapping("/calculate/{evaluationId}")
    public PerformanceEvaluation calculatePerformance(@PathVariable Long evaluationId) {
        return performanceService.calculatePerformance(evaluationId);
    }

    @PostMapping("/calculate/user/{userId}")
    public Map<String, Object> calculatePerformanceByUser(
            @PathVariable Long userId,
            @RequestParam String period,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return performanceService.calculatePerformanceByUser(userId, period, startDate, endDate);
    }

    @PostMapping("/calculate/team")
    public Map<String, Object> calculateTeamPerformance(
            @RequestBody List<Long> userIds,
            @RequestParam String period,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return performanceService.calculateTeamPerformance(userIds, period, startDate, endDate);
    }

    // 绩效奖金管理
    @PostMapping("/bonuses")
    public PerformanceBonus createBonus(@RequestBody PerformanceBonus bonus) {
        return performanceService.createBonus(bonus);
    }

    @PutMapping("/bonuses/{id}")
    public PerformanceBonus updateBonus(@PathVariable Long id, @RequestBody PerformanceBonus bonus) {
        bonus.setId(id);
        return performanceService.updateBonus(bonus);
    }

    @DeleteMapping("/bonuses/{id}")
    public void deleteBonus(@PathVariable Long id) {
        performanceService.deleteBonus(id);
    }

    @GetMapping("/bonuses/{id}")
    public PerformanceBonus getBonusById(@PathVariable Long id) {
        return performanceService.getBonusById(id);
    }

    @GetMapping("/bonuses")
    public List<PerformanceBonus> getAllBonuses() {
        return performanceService.getAllBonuses();
    }

    @GetMapping("/bonuses/user/{userId}")
    public List<PerformanceBonus> getBonusesByUser(@PathVariable Long userId) {
        return performanceService.getBonusesByUser(userId);
    }

    @GetMapping("/bonuses/evaluation/{evaluationId}")
    public List<PerformanceBonus> getBonusesByEvaluation(@PathVariable Long evaluationId) {
        return performanceService.getBonusesByEvaluation(evaluationId);
    }

    @GetMapping("/bonuses/period/{period}")
    public List<PerformanceBonus> getBonusesByPeriod(@PathVariable String period) {
        return performanceService.getBonusesByPeriod(period);
    }

    @GetMapping("/bonuses/status/{status}")
    public List<PerformanceBonus> getBonusesByStatus(@PathVariable String status) {
        return performanceService.getBonusesByStatus(status);
    }

    @GetMapping("/bonuses/date-range")
    public List<PerformanceBonus> getBonusesByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return performanceService.getBonusesByDateRange(startDate, endDate);
    }

    @GetMapping("/bonuses/type/{type}")
    public List<PerformanceBonus> getBonusesByType(@PathVariable String type) {
        return performanceService.getBonusesByType(type);
    }

    @GetMapping("/bonuses/user/{userId}/total")
    public Double getTotalBonusAmountByUser(@PathVariable Long userId) {
        return performanceService.getTotalBonusAmountByUser(userId);
    }

    @GetMapping("/bonuses/period/{period}/total")
    public Double getTotalBonusAmountByPeriod(@PathVariable String period) {
        return performanceService.getTotalBonusAmountByPeriod(period);
    }

    @GetMapping("/bonuses/date-range/total")
    public Double getTotalBonusAmountByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return performanceService.getTotalBonusAmountByDateRange(startDate, endDate);
    }

    // 绩效发放
    @PostMapping("/bonuses/{bonusId}/approve")
    public PerformanceBonus approveBonus(
            @PathVariable Long bonusId,
            @RequestParam Long approvedById) {
        return performanceService.approveBonus(bonusId, approvedById);
    }

    @PostMapping("/bonuses/{bonusId}/distribute")
    public PerformanceBonus distributeBonus(
            @PathVariable Long bonusId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date paymentDate) {
        return performanceService.distributeBonus(bonusId, paymentDate);
    }

    @PostMapping("/distribute")
    public Map<String, Object> generateBonusDistributionPlan(
            @RequestParam String period,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return performanceService.generateBonusDistributionPlan(period, startDate, endDate);
    }

    // 绩效统计分析
    @GetMapping("/stats/user/{userId}")
    public Map<String, Object> getPerformanceStatsByUser(@PathVariable Long userId) {
        return performanceService.getPerformanceStatsByUser(userId);
    }

    @GetMapping("/stats/period/{period}")
    public Map<String, Object> getPerformanceStatsByPeriod(@PathVariable String period) {
        return performanceService.getPerformanceStatsByPeriod(period);
    }

    @GetMapping("/stats/date-range")
    public Map<String, Object> getPerformanceStatsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return performanceService.getPerformanceStatsByDateRange(startDate, endDate);
    }

    @GetMapping("/stats/trend")
    public Map<String, Object> getPerformanceTrend(@RequestParam(defaultValue = "12") int months) {
        return performanceService.getPerformanceTrend(months);
    }

    @GetMapping("/stats/distribution")
    public Map<String, Object> getPerformanceDistribution() {
        return performanceService.getPerformanceDistribution();
    }
}
