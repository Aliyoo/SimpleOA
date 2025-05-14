package com.example.simpleoa.service;

import com.example.simpleoa.model.SystemConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SystemConfigService {
    
    /**
     * Find all system configurations
     */
    List<SystemConfig> findAllConfigs();
    
    /**
     * Find system config by id
     */
    Optional<SystemConfig> findConfigById(Long id);
    
    /**
     * Find system config by key
     */
    Optional<SystemConfig> findConfigByKey(String configKey);
    
    /**
     * Find system configs by category
     */
    List<SystemConfig> findConfigsByCategory(String category);
    
    /**
     * Save system config
     */
    SystemConfig saveConfig(SystemConfig systemConfig);
    
    /**
     * Delete system config
     */
    void deleteConfig(Long id);
    
    /**
     * Update config value
     */
    SystemConfig updateConfigValue(String configKey, String configValue);
    
    /**
     * Batch update config values
     */
    List<SystemConfig> batchUpdateConfigs(Map<String, String> configMap);
    
    /**
     * Initialize system with default configurations
     */
    void initializeDefaultConfigs();
    
    /**
     * Get configuration value as string
     */
    String getConfigValueAsString(String configKey, String defaultValue);
    
    /**
     * Get configuration value as integer
     */
    Integer getConfigValueAsInteger(String configKey, Integer defaultValue);
    
    /**
     * Get configuration value as boolean
     */
    Boolean getConfigValueAsBoolean(String configKey, Boolean defaultValue);
}
