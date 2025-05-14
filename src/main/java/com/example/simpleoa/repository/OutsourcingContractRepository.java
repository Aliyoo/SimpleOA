package com.example.simpleoa.repository;

import com.example.simpleoa.model.OutsourcingContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutsourcingContractRepository extends JpaRepository<OutsourcingContract, Long> {
    List<OutsourcingContract> findByOutsourcingId(Long outsourcingId);
    Optional<OutsourcingContract> findByContractNumber(String contractNumber);
    List<OutsourcingContract> findByStatus(String status);
}
