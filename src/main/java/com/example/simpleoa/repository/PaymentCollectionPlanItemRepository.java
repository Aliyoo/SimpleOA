package com.example.simpleoa.repository;

import com.example.simpleoa.model.PaymentCollectionPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PaymentCollectionPlanItemRepository extends JpaRepository<PaymentCollectionPlanItem, Long> {
    List<PaymentCollectionPlanItem> findByPlanId(Long planId);
    List<PaymentCollectionPlanItem> findByStatus(String status);
    List<PaymentCollectionPlanItem> findByDueDateBetween(Date startDate, Date endDate);
    List<PaymentCollectionPlanItem> findByCollectionId(Long collectionId);
    
    @Query("SELECT pcpi FROM PaymentCollectionPlanItem pcpi WHERE pcpi.plan.project.id = ?1 AND pcpi.status = ?2")
    List<PaymentCollectionPlanItem> findByProjectIdAndStatus(Long projectId, String status);
    
    @Query("SELECT pcpi FROM PaymentCollectionPlanItem pcpi WHERE pcpi.plan.project.id = ?1 AND pcpi.dueDate <= ?2 AND pcpi.status = '未收款'")
    List<PaymentCollectionPlanItem> findOverdueItemsByProject(Long projectId, Date currentDate);
}
