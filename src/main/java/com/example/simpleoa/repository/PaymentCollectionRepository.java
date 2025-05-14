package com.example.simpleoa.repository;

import com.example.simpleoa.model.PaymentCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PaymentCollectionRepository extends JpaRepository<PaymentCollection, Long> {
    List<PaymentCollection> findByProjectId(Long projectId);
    List<PaymentCollection> findByStatus(String status);
    List<PaymentCollection> findByCollectionDateBetween(Date startDate, Date endDate);
    
    @Query("SELECT SUM(pc.amount) FROM PaymentCollection pc WHERE pc.project.id = ?1")
    Double getTotalCollectionAmountByProject(Long projectId);
    
    @Query("SELECT SUM(pc.amount) FROM PaymentCollection pc WHERE pc.project.id = ?1 AND pc.collectionDate BETWEEN ?2 AND ?3")
    Double getTotalCollectionAmountByProjectAndDateRange(Long projectId, Date startDate, Date endDate);
}
