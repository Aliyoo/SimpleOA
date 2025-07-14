package com.example.simpleoa.repository;

import com.example.simpleoa.model.ReimbursementItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementItemRepository extends JpaRepository<ReimbursementItem, Long> {
}
