package com.example.simpleoa.repository;

import com.example.simpleoa.model.OutsourcingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface OutsourcingProgressRepository extends JpaRepository<OutsourcingProgress, Long> {
    List<OutsourcingProgress> findByOutsourcingId(Long outsourcingId);
    List<OutsourcingProgress> findByOutsourcingIdAndReportDateBetween(Long outsourcingId, Date startDate, Date endDate);
    List<OutsourcingProgress> findByReportedById(Long userId);
}
