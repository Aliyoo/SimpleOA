package com.example.simpleoa.service;

import com.example.simpleoa.model.PaymentCollection;
import com.example.simpleoa.model.PaymentCollectionPlan;
import com.example.simpleoa.model.PaymentCollectionPlanItem;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface PaymentCollectionService {
    // 回款记录管理
    PaymentCollection createCollection(PaymentCollection collection);
    PaymentCollection updateCollection(PaymentCollection collection);
    void deleteCollection(Long id);
    PaymentCollection getCollectionById(Long id);
    List<PaymentCollection> getAllCollections();
    List<PaymentCollection> getCollectionsByProject(Long projectId);
    List<PaymentCollection> getCollectionsByStatus(String status);
    List<PaymentCollection> getCollectionsByDateRange(Date startDate, Date endDate);
    Double getTotalCollectionAmountByProject(Long projectId);
    Double getTotalCollectionAmountByProjectAndDateRange(Long projectId, Date startDate, Date endDate);
    
    // 回款计划管理
    PaymentCollectionPlan createPlan(PaymentCollectionPlan plan);
    PaymentCollectionPlan updatePlan(PaymentCollectionPlan plan);
    void deletePlan(Long id);
    PaymentCollectionPlan getPlanById(Long id);
    List<PaymentCollectionPlan> getAllPlans();
    List<PaymentCollectionPlan> getPlansByProject(Long projectId);
    List<PaymentCollectionPlan> getPlansByStatus(String status);
    List<PaymentCollectionPlan> getPlansByDateRange(Date startDate, Date endDate);
    Double getTotalPlannedAmountByProject(Long projectId);
    Double getTotalCollectedAmountByProject(Long projectId);
    
    // 回款计划明细管理
    PaymentCollectionPlanItem createPlanItem(PaymentCollectionPlanItem item);
    PaymentCollectionPlanItem updatePlanItem(PaymentCollectionPlanItem item);
    void deletePlanItem(Long id);
    PaymentCollectionPlanItem getPlanItemById(Long id);
    List<PaymentCollectionPlanItem> getPlanItemsByPlan(Long planId);
    List<PaymentCollectionPlanItem> getPlanItemsByStatus(String status);
    List<PaymentCollectionPlanItem> getPlanItemsByDueDate(Date startDate, Date endDate);
    List<PaymentCollectionPlanItem> getPlanItemsByCollection(Long collectionId);
    List<PaymentCollectionPlanItem> getPlanItemsByProjectAndStatus(Long projectId, String status);
    List<PaymentCollectionPlanItem> getOverdueItemsByProject(Long projectId);
    
    // 回款记录与计划关联
    PaymentCollectionPlanItem linkCollectionToPlanItem(Long planItemId, Long collectionId);
    
    // 回款统计分析
    Map<String, Object> getCollectionStatsByProject(Long projectId);
    Map<String, Object> getCollectionStatsByDateRange(Date startDate, Date endDate);
    Map<String, Object> getCollectionPerformanceStats();
    Map<String, Object> getCollectionTrend(int months);
}
