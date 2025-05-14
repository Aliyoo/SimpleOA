package com.example.simpleoa.controller;

import com.example.simpleoa.model.PaymentCollection;
import com.example.simpleoa.model.PaymentCollectionPlan;
import com.example.simpleoa.model.PaymentCollectionPlanItem;
import com.example.simpleoa.service.PaymentCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collections")
public class PaymentCollectionController {
    private final PaymentCollectionService collectionService;

    @Autowired
    public PaymentCollectionController(PaymentCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    // 回款记录管理
    @PostMapping
    public PaymentCollection createCollection(@RequestBody PaymentCollection collection) {
        return collectionService.createCollection(collection);
    }

    @PutMapping("/{id}")
    public PaymentCollection updateCollection(@PathVariable Long id, @RequestBody PaymentCollection collection) {
        collection.setId(id);
        return collectionService.updateCollection(collection);
    }

    @DeleteMapping("/{id}")
    public void deleteCollection(@PathVariable Long id) {
        collectionService.deleteCollection(id);
    }

    @GetMapping("/{id}")
    public PaymentCollection getCollectionById(@PathVariable Long id) {
        return collectionService.getCollectionById(id);
    }

    @GetMapping
    public List<PaymentCollection> getAllCollections() {
        return collectionService.getAllCollections();
    }

    @GetMapping("/project/{projectId}")
    public List<PaymentCollection> getCollectionsByProject(@PathVariable Long projectId) {
        return collectionService.getCollectionsByProject(projectId);
    }

    @GetMapping("/status/{status}")
    public List<PaymentCollection> getCollectionsByStatus(@PathVariable String status) {
        return collectionService.getCollectionsByStatus(status);
    }

    @GetMapping("/date-range")
    public List<PaymentCollection> getCollectionsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return collectionService.getCollectionsByDateRange(startDate, endDate);
    }

    @GetMapping("/project/{projectId}/total")
    public Double getTotalCollectionAmountByProject(@PathVariable Long projectId) {
        return collectionService.getTotalCollectionAmountByProject(projectId);
    }

    @GetMapping("/project/{projectId}/total/date-range")
    public Double getTotalCollectionAmountByProjectAndDateRange(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return collectionService.getTotalCollectionAmountByProjectAndDateRange(projectId, startDate, endDate);
    }

    // 回款计划管理
    @PostMapping("/plan")
    public PaymentCollectionPlan createPlan(@RequestBody PaymentCollectionPlan plan) {
        return collectionService.createPlan(plan);
    }

    @PutMapping("/plan/{id}")
    public PaymentCollectionPlan updatePlan(@PathVariable Long id, @RequestBody PaymentCollectionPlan plan) {
        plan.setId(id);
        return collectionService.updatePlan(plan);
    }

    @DeleteMapping("/plan/{id}")
    public void deletePlan(@PathVariable Long id) {
        collectionService.deletePlan(id);
    }

    @GetMapping("/plan/{id}")
    public PaymentCollectionPlan getPlanById(@PathVariable Long id) {
        return collectionService.getPlanById(id);
    }

    @GetMapping("/plan")
    public List<PaymentCollectionPlan> getAllPlans() {
        return collectionService.getAllPlans();
    }

    @GetMapping("/plan/project/{projectId}")
    public List<PaymentCollectionPlan> getPlansByProject(@PathVariable Long projectId) {
        return collectionService.getPlansByProject(projectId);
    }

    @GetMapping("/plan/status/{status}")
    public List<PaymentCollectionPlan> getPlansByStatus(@PathVariable String status) {
        return collectionService.getPlansByStatus(status);
    }

    @GetMapping("/plan/date-range")
    public List<PaymentCollectionPlan> getPlansByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return collectionService.getPlansByDateRange(startDate, endDate);
    }

    @GetMapping("/plan/project/{projectId}/total-planned")
    public Double getTotalPlannedAmountByProject(@PathVariable Long projectId) {
        return collectionService.getTotalPlannedAmountByProject(projectId);
    }

    @GetMapping("/plan/project/{projectId}/total-collected")
    public Double getTotalCollectedAmountByProject(@PathVariable Long projectId) {
        return collectionService.getTotalCollectedAmountByProject(projectId);
    }

    // 回款计划明细管理
    @PostMapping("/plan/{planId}/item")
    public PaymentCollectionPlanItem createPlanItem(
            @PathVariable Long planId,
            @RequestBody PaymentCollectionPlanItem item) {
        PaymentCollectionPlan plan = new PaymentCollectionPlan();
        plan.setId(planId);
        item.setPlan(plan);
        return collectionService.createPlanItem(item);
    }

    @PutMapping("/plan/item/{id}")
    public PaymentCollectionPlanItem updatePlanItem(
            @PathVariable Long id,
            @RequestBody PaymentCollectionPlanItem item) {
        item.setId(id);
        return collectionService.updatePlanItem(item);
    }

    @DeleteMapping("/plan/item/{id}")
    public void deletePlanItem(@PathVariable Long id) {
        collectionService.deletePlanItem(id);
    }

    @GetMapping("/plan/item/{id}")
    public PaymentCollectionPlanItem getPlanItemById(@PathVariable Long id) {
        return collectionService.getPlanItemById(id);
    }

    @GetMapping("/plan/{planId}/items")
    public List<PaymentCollectionPlanItem> getPlanItemsByPlan(@PathVariable Long planId) {
        return collectionService.getPlanItemsByPlan(planId);
    }

    @GetMapping("/plan/item/status/{status}")
    public List<PaymentCollectionPlanItem> getPlanItemsByStatus(@PathVariable String status) {
        return collectionService.getPlanItemsByStatus(status);
    }

    @GetMapping("/plan/item/due-date")
    public List<PaymentCollectionPlanItem> getPlanItemsByDueDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return collectionService.getPlanItemsByDueDate(startDate, endDate);
    }

    @GetMapping("/collection/{collectionId}/items")
    public List<PaymentCollectionPlanItem> getPlanItemsByCollection(@PathVariable Long collectionId) {
        return collectionService.getPlanItemsByCollection(collectionId);
    }

    @GetMapping("/project/{projectId}/items/status/{status}")
    public List<PaymentCollectionPlanItem> getPlanItemsByProjectAndStatus(
            @PathVariable Long projectId,
            @PathVariable String status) {
        return collectionService.getPlanItemsByProjectAndStatus(projectId, status);
    }

    @GetMapping("/project/{projectId}/items/overdue")
    public List<PaymentCollectionPlanItem> getOverdueItemsByProject(@PathVariable Long projectId) {
        return collectionService.getOverdueItemsByProject(projectId);
    }

    // 回款记录与计划关联
    @PostMapping("/plan/item/{planItemId}/link/{collectionId}")
    public PaymentCollectionPlanItem linkCollectionToPlanItem(
            @PathVariable Long planItemId,
            @PathVariable Long collectionId) {
        return collectionService.linkCollectionToPlanItem(planItemId, collectionId);
    }

    // 回款统计分析
    @GetMapping("/stats/project/{projectId}")
    public Map<String, Object> getCollectionStatsByProject(@PathVariable Long projectId) {
        return collectionService.getCollectionStatsByProject(projectId);
    }

    @GetMapping("/stats/date-range")
    public Map<String, Object> getCollectionStatsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return collectionService.getCollectionStatsByDateRange(startDate, endDate);
    }

    @GetMapping("/stats/performance")
    public Map<String, Object> getCollectionPerformanceStats() {
        return collectionService.getCollectionPerformanceStats();
    }

    @GetMapping("/stats/trend")
    public Map<String, Object> getCollectionTrend(@RequestParam(defaultValue = "12") int months) {
        return collectionService.getCollectionTrend(months);
    }
}
