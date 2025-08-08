package com.example.simpleoa.repository;

import com.example.simpleoa.model.Outsourcing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutsourcingRepository extends JpaRepository<Outsourcing, Long> {
    List<Outsourcing> findByProjectId(Long projectId);
    List<Outsourcing> findByCompany(String company);
    List<Outsourcing> findByStatus(String status);
}
