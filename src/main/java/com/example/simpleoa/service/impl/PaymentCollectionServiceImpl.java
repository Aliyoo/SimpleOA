package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.PaymentCollection;
import com.example.simpleoa.model.PaymentCollectionPlan;
import com.example.simpleoa.model.PaymentCollectionPlanItem;
import com.example.simpleoa.model.Project;
import com.example.simpleoa.repository.PaymentCollectionPlanItemRepository;
import com.example.simpleoa.repository.PaymentCollectionPlanRepository;
import com.example.simpleoa.repository.PaymentCollectionRepository;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.service.PaymentCollectionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PaymentCollectionServiceImpl implements PaymentCollectionService {

    private final PaymentCollectionRepository collectionRepository;
    private final PaymentCollectionPlanRepository planRepository;
    private final PaymentCollectionPlanItemRepository planItemRepository;
    private final ProjectRepository projectRepository;

    @Autowired
    public PaymentCollectionServiceImpl(
            PaymentCollectionRepository collectionRepository,
            PaymentCollectionPlanRepository planRepository,
            PaymentCollectionPlanItemRepository planItemRepository,
            ProjectRepository projectRepository) {
        this.collectionRepository = collectionRepository;
        this.planRepository = planRepository;
        this.planItemRepository = planItemRepository;
        this.projectRepository = projectRepository;
    }

    // 回款记录管理
    @Override
    @Transactional
    public PaymentCollection createCollection(PaymentCollection collection) {
        if (collection.getProject() != null && collection.getProject().getId() != null) {
            Project project = projectRepository.findById(collection.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            collection.setProject(project);
        }
        
        if (collection.getStatus() == null) {
            collection.setStatus("已收款");
        }
        
        if (collection.getRecordTime() == null) {
            collection.setRecordTime(new Date());
        }
        
        return collectionRepository.save(collection);
    }

    @Override
    @Transactional
    public PaymentCollection updateCollection(PaymentCollection collection) {
        PaymentCollection existingCollection = collectionRepository.findById(collection.getId())
                .orElseThrow(() -> new EntityNotFoundException("Payment collection not found"));
        
        if (collection.getProject() != null && collection.getProject().getId() != null) {
            Project project = projectRepository.findById(collection.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            existingCollection.setProject(project);
        }
        
        existingCollection.setAmount(collection.getAmount());
        existingCollection.setCollectionDate(collection.getCollectionDate());
        existingCollection.setCollectionMethod(collection.getCollectionMethod());
        existingCollection.setPaymentReference(collection.getPaymentReference());
        existingCollection.setStatus(collection.getStatus());
        existingCollection.setRemarks(collection.getRemarks());
        existingCollection.setRecordedBy(collection.getRecordedBy());
        
        return collectionRepository.save(existingCollection);
    }

    @Override
    @Transactional
    public void deleteCollection(Long id) {
        PaymentCollection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment collection not found"));
        
        // 解除与计划项的关联
        List<PaymentCollectionPlanItem> planItems = planItemRepository.findByCollectionId(id);
        for (PaymentCollectionPlanItem item : planItems) {
            item.setCollection(null);
            item.setStatus("未收款");
            planItemRepository.save(item);
        }
        
        collectionRepository.delete(collection);
    }

    @Override
    public PaymentCollection getCollectionById(Long id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment collection not found"));
    }

    @Override
    public List<PaymentCollection> getAllCollections() {
        return collectionRepository.findAll();
    }

    @Override
    public List<PaymentCollection> getCollectionsByProject(Long projectId) {
        return collectionRepository.findByProjectId(projectId);
    }

    @Override
    public List<PaymentCollection> getCollectionsByStatus(String status) {
        return collectionRepository.findByStatus(status);
    }

    @Override
    public List<PaymentCollection> getCollectionsByDateRange(Date startDate, Date endDate) {
        return collectionRepository.findByCollectionDateBetween(startDate, endDate);
    }

    @Override
    public Double getTotalCollectionAmountByProject(Long projectId) {
        Double amount = collectionRepository.getTotalCollectionAmountByProject(projectId);
        return amount != null ? amount : 0.0;
    }

    @Override
    public Double getTotalCollectionAmountByProjectAndDateRange(Long projectId, Date startDate, Date endDate) {
        Double amount = collectionRepository.getTotalCollectionAmountByProjectAndDateRange(projectId, startDate, endDate);
        return amount != null ? amount : 0.0;
    }

    // 回款计划管理
    @Override
    @Transactional
    public PaymentCollectionPlan createPlan(PaymentCollectionPlan plan) {
        if (plan.getProject() != null && plan.getProject().getId() != null) {
            Project project = projectRepository.findById(plan.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            plan.setProject(project);
        }
        
        if (plan.getCollectedAmount() == null) {
            plan.setCollectedAmount(0.0);
        }
        
        if (plan.getStatus() == null) {
            plan.setStatus("进行中");
        }
        
        if (plan.getCreateTime() == null) {
            plan.setCreateTime(new Date());
        }
        
        return planRepository.save(plan);
    }

    @Override
    @Transactional
    public PaymentCollectionPlan updatePlan(PaymentCollectionPlan plan) {
        PaymentCollectionPlan existingPlan = planRepository.findById(plan.getId())
                .orElseThrow(() -> new EntityNotFoundException("Payment collection plan not found"));
        
        if (plan.getProject() != null && plan.getProject().getId() != null) {
            Project project = projectRepository.findById(plan.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            existingPlan.setProject(project);
        }
        
        existingPlan.setPlanName(plan.getPlanName());
        existingPlan.setTotalAmount(plan.getTotalAmount());
        existingPlan.setInstallments(plan.getInstallments());
        existingPlan.setStartDate(plan.getStartDate());
        existingPlan.setEndDate(plan.getEndDate());
        existingPlan.setCollectedAmount(plan.getCollectedAmount());
        existingPlan.setStatus(plan.getStatus());
        existingPlan.setDescription(plan.getDescription());
        
        return planRepository.save(existingPlan);
    }

    @Override
    @Transactional
    public void deletePlan(Long id) {
        PaymentCollectionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment collection plan not found"));
        
        // 删除计划项
        List<PaymentCollectionPlanItem> planItems = planItemRepository.findByPlanId(id);
        planItemRepository.deleteAll(planItems);
        
        planRepository.delete(plan);
    }

    @Override
    public PaymentCollectionPlan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment collection plan not found"));
    }

    @Override
    public List<PaymentCollectionPlan> getAllPlans() {
        return planRepository.findAll();
    }

    @Override
    public List<PaymentCollectionPlan> getPlansByProject(Long projectId) {
        return planRepository.findByProjectId(projectId);
    }

    @Override
    public List<PaymentCollectionPlan> getPlansByStatus(String status) {
        return planRepository.findByStatus(status);
    }

    @Override
    public List<PaymentCollectionPlan> getPlansByDateRange(Date startDate, Date endDate) {
        return planRepository.findByStartDateBetweenOrEndDateBetween(startDate, endDate, startDate, endDate);
    }

    @Override
    public Double getTotalPlannedAmountByProject(Long projectId) {
        Double amount = planRepository.getTotalPlannedAmountByProject(projectId);
        return amount != null ? amount : 0.0;
    }

    @Override
    public Double getTotalCollectedAmountByProject(Long projectId) {
        Double amount = planRepository.getTotalCollectedAmountByProject(projectId);
        return amount != null ? amount : 0.0;
    }

    // 回款计划明细管理
    @Override
    @Transactional
    public PaymentCollectionPlanItem createPlanItem(PaymentCollectionPlanItem item) {
        if (item.getPlan() != null && item.getPlan().getId() != null) {
            PaymentCollectionPlan plan = planRepository.findById(item.getPlan().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Payment collection plan not found"));
            item.setPlan(plan);
        }
        
        if (item.getStatus() == null) {
            item.setStatus("未收款");
        }
        
        return planItemRepository.save(item);
    }

    @Override
    @Transactional
    public PaymentCollectionPlanItem updatePlanItem(PaymentCollectionPlanItem item) {
        PaymentCollectionPlanItem existingItem = planItemRepository.findById(item.getId())
                .orElseThrow(() -> new EntityNotFoundException("Payment collection plan item not found"));
        
        existingItem.setInstallmentNumber(item.getInstallmentNumber());
        existingItem.setAmount(item.getAmount());
        existingItem.setDueDate(item.getDueDate());
        existingItem.setMilestone(item.getMilestone());
        existingItem.setStatus(item.getStatus());
        
        if (item.getCollection() != null && item.getCollection().getId() != null) {
            PaymentCollection collection = collectionRepository.findById(item.getCollection().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Payment collection not found"));
            existingItem.setCollection(collection);
        } else {
            existingItem.setCollection(null);
        }
        
        return planItemRepository.save(existingItem);
    }

    @Override
    @Transactional
    public void deletePlanItem(Long id) {
        PaymentCollectionPlanItem item = planItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment collection plan item not found"));
        planItemRepository.delete(item);
    }

    @Override
    public PaymentCollectionPlanItem getPlanItemById(Long id) {
        return planItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment collection plan item not found"));
    }

    @Override
    public List<PaymentCollectionPlanItem> getPlanItemsByPlan(Long planId) {
        return planItemRepository.findByPlanId(planId);
    }

    @Override
    public List<PaymentCollectionPlanItem> getPlanItemsByStatus(String status) {
        return planItemRepository.findByStatus(status);
    }

    @Override
    public List<PaymentCollectionPlanItem> getPlanItemsByDueDate(Date startDate, Date endDate) {
        return planItemRepository.findByDueDateBetween(startDate, endDate);
    }

    @Override
    public List<PaymentCollectionPlanItem> getPlanItemsByCollection(Long collectionId) {
        return planItemRepository.findByCollectionId(collectionId);
    }

    @Override
    public List<PaymentCollectionPlanItem> getPlanItemsByProjectAndStatus(Long projectId, String status) {
        return planItemRepository.findByProjectIdAndStatus(projectId, status);
    }

    @Override
    public List<PaymentCollectionPlanItem> getOverdueItemsByProject(Long projectId) {
        return planItemRepository.findOverdueItemsByProject(projectId, new Date());
    }

    // 回款记录与计划关联
    @Override
    @Transactional
    public PaymentCollectionPlanItem linkCollectionToPlanItem(Long planItemId, Long collectionId) {
        PaymentCollectionPlanItem item = planItemRepository.findById(planItemId)
                .orElseThrow(() -> new EntityNotFoundException("Payment collection plan item not found"));
        
        PaymentCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new EntityNotFoundException("Payment collection not found"));
        
        item.setCollection(collection);
        item.setStatus("已收款");
        
        // 更新计划的已收款金额
        PaymentCollectionPlan plan = item.getPlan();
        if (plan != null) {
            Double collectedAmount = 0.0;
            List<PaymentCollectionPlanItem> items = planItemRepository.findByPlanId(plan.getId());
            for (PaymentCollectionPlanItem planItem : items) {
                if ("已收款".equals(planItem.getStatus()) && planItem.getAmount() != null) {
                    collectedAmount += planItem.getAmount();
                }
            }
            
            plan.setCollectedAmount(collectedAmount);
            
            // 如果所有计划项都已收款，则更新计划状态为完成
            boolean allCollected = items.stream().allMatch(i -> "已收款".equals(i.getStatus()));
            if (allCollected) {
                plan.setStatus("已完成");
            }
            
            planRepository.save(plan);
        }
        
        return planItemRepository.save(item);
    }

    // 回款统计分析
    @Override
    public Map<String, Object> getCollectionStatsByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        
        Double totalPlannedAmount = getTotalPlannedAmountByProject(projectId);
        Double totalCollectedAmount = getTotalCollectionAmountByProject(projectId);
        
        List<PaymentCollectionPlan> plans = planRepository.findByProjectId(projectId);
        List<PaymentCollection> collections = collectionRepository.findByProjectId(projectId);
        
        // 计算逾期未收款金额
        List<PaymentCollectionPlanItem> overdueItems = getOverdueItemsByProject(projectId);
        Double overdueAmount = overdueItems.stream()
                .mapToDouble(PaymentCollectionPlanItem::getAmount)
                .sum();
        
        // 计算收款完成率
        Double completionRate = totalPlannedAmount > 0 ? (totalCollectedAmount / totalPlannedAmount) * 100 : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("project", project);
        result.put("totalPlannedAmount", totalPlannedAmount);
        result.put("totalCollectedAmount", totalCollectedAmount);
        result.put("completionRate", completionRate);
        result.put("overdueAmount", overdueAmount);
        result.put("overdueCount", overdueItems.size());
        result.put("planCount", plans.size());
        result.put("collectionCount", collections.size());
        
        return result;
    }

    @Override
    public Map<String, Object> getCollectionStatsByDateRange(Date startDate, Date endDate) {
        List<PaymentCollection> collections = collectionRepository.findByCollectionDateBetween(startDate, endDate);
        
        // 按项目分组统计
        Map<Long, Double> amountByProject = new HashMap<>();
        for (PaymentCollection collection : collections) {
            Long projectId = collection.getProject().getId();
            Double amount = amountByProject.getOrDefault(projectId, 0.0);
            amount += collection.getAmount();
            amountByProject.put(projectId, amount);
        }
        
        // 计算总金额
        Double totalAmount = collections.stream()
                .mapToDouble(PaymentCollection::getAmount)
                .sum();
        
        // 按收款方式分组统计
        Map<String, Double> amountByMethod = new HashMap<>();
        for (PaymentCollection collection : collections) {
            String method = collection.getCollectionMethod();
            if (method != null) {
                Double amount = amountByMethod.getOrDefault(method, 0.0);
                amount += collection.getAmount();
                amountByMethod.put(method, amount);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("totalAmount", totalAmount);
        result.put("collectionCount", collections.size());
        result.put("amountByProject", amountByProject);
        result.put("amountByMethod", amountByMethod);
        
        return result;
    }

    @Override
    public Map<String, Object> getCollectionPerformanceStats() {
        List<Project> projects = projectRepository.findAll();
        Map<String, Object> projectStats = new HashMap<>();
        
        for (Project project : projects) {
            Long projectId = project.getId();
            Double plannedAmount = getTotalPlannedAmountByProject(projectId);
            Double collectedAmount = getTotalCollectionAmountByProject(projectId);
            Double completionRate = plannedAmount > 0 ? (collectedAmount / plannedAmount) * 100 : 0;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("project", project);
            stats.put("plannedAmount", plannedAmount);
            stats.put("collectedAmount", collectedAmount);
            stats.put("completionRate", completionRate);
            
            projectStats.put(project.getName(), stats);
        }
        
        // 计算总体统计
        Double totalPlannedAmount = projects.stream()
                .mapToDouble(p -> getTotalPlannedAmountByProject(p.getId()))
                .sum();
        
        Double totalCollectedAmount = projects.stream()
                .mapToDouble(p -> getTotalCollectionAmountByProject(p.getId()))
                .sum();
        
        Double overallCompletionRate = totalPlannedAmount > 0 ? (totalCollectedAmount / totalPlannedAmount) * 100 : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("projectStats", projectStats);
        result.put("totalPlannedAmount", totalPlannedAmount);
        result.put("totalCollectedAmount", totalCollectedAmount);
        result.put("overallCompletionRate", overallCompletionRate);
        
        return result;
    }

    @Override
    public Map<String, Object> getCollectionTrend(int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        Map<String, Double> monthlyCollections = new LinkedHashMap<>();
        Map<String, Double> monthlyPlanned = new LinkedHashMap<>();
        
        // 从当前月份往前推指定的月数
        for (int i = 0; i < months; i++) {
            Date startDate = calendar.getTime();
            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.MILLISECOND, -1);
            Date endDate = calendar.getTime();
            
            String monthKey = String.format("%d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
            
            // 统计该月的实际收款
            List<PaymentCollection> collections = collectionRepository.findByCollectionDateBetween(startDate, endDate);
            Double monthlyAmount = collections.stream()
                    .mapToDouble(PaymentCollection::getAmount)
                    .sum();
            monthlyCollections.put(monthKey, monthlyAmount);
            
            // 统计该月的计划收款
            List<PaymentCollectionPlanItem> planItems = planItemRepository.findByDueDateBetween(startDate, endDate);
            Double plannedAmount = planItems.stream()
                    .mapToDouble(PaymentCollectionPlanItem::getAmount)
                    .sum();
            monthlyPlanned.put(monthKey, plannedAmount);
            
            // 重置为下个月的开始
            calendar.add(Calendar.MILLISECOND, 1);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("monthlyCollections", monthlyCollections);
        result.put("monthlyPlanned", monthlyPlanned);
        
        return result;
    }
}
