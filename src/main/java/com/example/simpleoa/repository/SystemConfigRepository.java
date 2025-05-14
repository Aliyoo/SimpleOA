package com.example.simpleoa.repository;

import com.example.simpleoa.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    
    /**
     * Find system config by key
     */
    Optional<SystemConfig> findByConfigKey(String configKey);
    
    /**
     * Find system configs by category
     */
    List<SystemConfig> findByCategory(String category);
    
    /**
     * Find system configs by editable flag
     */
    List<SystemConfig> findByIsEditable(Boolean isEditable);
    
    /**
     * Find system configs by system flag
     */
    List<SystemConfig> findByIsSystem(Boolean isSystem);
    
    /**
     * Check if config key exists
     */
    boolean existsByConfigKey(String configKey);
    
    /**
     * Find system configs by key pattern
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configKey LIKE %:keyPattern%")
    List<SystemConfig> findByConfigKeyPattern(@Param("keyPattern") String keyPattern);
    
    /**
     * Find system configs by value pattern
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configValue LIKE %:valuePattern%")
    List<SystemConfig> findByConfigValuePattern(@Param("valuePattern") String valuePattern);
}
