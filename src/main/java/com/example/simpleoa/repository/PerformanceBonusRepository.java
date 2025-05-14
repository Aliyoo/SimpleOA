package com.example.simpleoa.repository;

import com.example.simpleoa.model.PerformanceBonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PerformanceBonusRepository extends JpaRepository<PerformanceBonus, Long> {
    List<PerformanceBonus> findByUserId(Long userId);
    List<PerformanceBonus> findByEvaluationId(Long evaluationId);
    List<PerformanceBonus> findByBonusPeriod(String bonusPeriod);
    List<PerformanceBonus> findByStatus(String status);
    List<PerformanceBonus> findByPaymentDateBetween(Date startDate, Date endDate);
    List<PerformanceBonus> findByBonusType(String bonusType);
    
    @Query("SELECT SUM(pb.amount) FROM PerformanceBonus pb WHERE pb.user.id = ?1")
    Double getTotalBonusAmountByUser(Long userId);
    
    @Query("SELECT SUM(pb.amount) FROM PerformanceBonus pb WHERE pb.bonusPeriod = ?1")
    Double getTotalBonusAmountByPeriod(String bonusPeriod);
    
    @Query("SELECT SUM(pb.amount) FROM PerformanceBonus pb WHERE pb.paymentDate BETWEEN ?1 AND ?2")
    Double getTotalBonusAmountByDateRange(Date startDate, Date endDate);
}
