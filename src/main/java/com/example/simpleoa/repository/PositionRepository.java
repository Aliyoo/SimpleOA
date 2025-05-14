package com.example.simpleoa.repository;

import com.example.simpleoa.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByDepartment(String department);
    List<Position> findByLevel(String level);
    List<Position> findByIsActive(Boolean isActive);
    
    @Query("SELECT p FROM Position p WHERE p.name LIKE %?1%")
    List<Position> findByNameContaining(String name);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.position.id = ?1")
    Long countUsersByPosition(Long positionId);
}
