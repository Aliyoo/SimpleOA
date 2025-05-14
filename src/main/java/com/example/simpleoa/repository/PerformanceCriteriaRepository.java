package com.example.simpleoa.repository;

import com.example.simpleoa.model.PerformanceCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceCriteriaRepository extends JpaRepository<PerformanceCriteria, Long> {
    List<PerformanceCriteria> findByCategory(String category);
    List<PerformanceCriteria> findByIsActive(Boolean isActive);
}
