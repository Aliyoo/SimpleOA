package com.example.simpleoa.repository;

import com.example.simpleoa.model.PaymentCollectionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PaymentCollectionPlanRepository extends JpaRepository<PaymentCollectionPlan, Long> {
    List<PaymentCollectionPlan> findByProjectId(Long projectId);
    List<PaymentCollectionPlan> findByStatus(String status);
    List<PaymentCollectionPlan> findByStartDateBetweenOrEndDateBetween(Date startDate1, Date endDate1, Date startDate2, Date endDate2);
    
    @Query("SELECT SUM(pcp.totalAmount) FROM PaymentCollectionPlan pcp WHERE pcp.project.id = ?1")
    Double getTotalPlannedAmountByProject(Long projectId);
    
    @Query("SELECT SUM(pcp.collectedAmount) FROM PaymentCollectionPlan pcp WHERE pcp.project.id = ?1")
    Double getTotalCollectedAmountByProject(Long projectId);
}
