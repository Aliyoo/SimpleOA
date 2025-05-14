package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.SystemConfig;
import com.example.simpleoa.repository.SystemConfigRepository;
import com.example.simpleoa.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Override
    public List<SystemConfig> findAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @Override
    public Optional<SystemConfig> findConfigById(Long id) {
        return systemConfigRepository.findById(id);
    }

    @Override
    public Optional<SystemConfig> findConfigByKey(String configKey) {
        return systemConfigRepository.findByConfigKey(configKey);
    }

    @Override
    public List<SystemConfig> findConfigsByCategory(String category) {
        return systemConfigRepository.findByCategory(category);
    }

    @Override
    @Transactional
    public SystemConfig saveConfig(SystemConfig systemConfig) {
        // Set creation time for new configs
        if (systemConfig.getId() == null) {
            systemConfig.setCreateTime(new Date());
        }
        
        systemConfig.setLastUpdateTime(new Date());
        return systemConfigRepository.save(systemConfig);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        // Check if it's a system config that shouldn't be deleted
        Optional<SystemConfig> configOpt = systemConfigRepository.findById(id);
        if (configOpt.isPresent() && Boolean.TRUE.equals(configOpt.get().getIsSystem())) {
            throw new IllegalArgumentException("Cannot delete system configuration with id: " + id);
        }
        
        systemConfigRepository.deleteById(id);
    }

    @Override
    @Transactional
    public SystemConfig updateConfigValue(String configKey, String configValue) {
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new NoSuchElementException("Configuration not found with key: " + configKey));
        
        // Check if config is editable
        if (Boolean.FALSE.equals(config.getIsEditable())) {
            throw new IllegalArgumentException("Configuration with key " + configKey + " is not editable");
        }
        
        config.setConfigValue(configValue);
        config.setLastUpdateTime(new Date());
        
        return systemConfigRepository.save(config);
    }

    @Override
    @Transactional
    public List<SystemConfig> batchUpdateConfigs(Map<String, String> configMap) {
        List<SystemConfig> updatedConfigs = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            try {
                SystemConfig config = updateConfigValue(entry.getKey(), entry.getValue());
                updatedConfigs.add(config);
            } catch (Exception e) {
                // Log the error but continue with other updates
                System.err.println("Error updating config " + entry.getKey() + ": " + e.getMessage());
            }
        }
        
        return updatedConfigs;
    }

    @Override
    @Transactional
    public void initializeDefaultConfigs() {
        // Define default system configurations
        Map<String, Map<String, Object>> defaultConfigs = new HashMap<>();
        
        // System settings
        defaultConfigs.put("system.name", createConfigMap("SimpleOA System", "System name", "SYSTEM", true, false));
        defaultConfigs.put("system.version", createConfigMap("1.0.0", "System version", "SYSTEM", true, false));
        defaultConfigs.put("system.admin.email", createConfigMap("admin@example.com", "Admin email", "SYSTEM", true, true));
        
        // Security settings
        defaultConfigs.put("security.password.min_length", createConfigMap("8", "Minimum password length", "SECURITY", true, true));
        defaultConfigs.put("security.password.require_special", createConfigMap("true", "Require special characters in password", "SECURITY", true, true));
        defaultConfigs.put("security.session.timeout", createConfigMap("30", "Session timeout in minutes", "SECURITY", true, true));
        
        // Notification settings
        defaultConfigs.put("notification.email.enabled", createConfigMap("true", "Enable email notifications", "NOTIFICATION", true, true));
        defaultConfigs.put("notification.sms.enabled", createConfigMap("false", "Enable SMS notifications", "NOTIFICATION", true, true));
        
        // Create or update each default config
        for (Map.Entry<String, Map<String, Object>> entry : defaultConfigs.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> configData = entry.getValue();
            
            SystemConfig config = systemConfigRepository.findByConfigKey(key).orElse(new SystemConfig());
            
            // Only set values if this is a new config
            if (config.getId() == null) {
                config.setConfigKey(key);
                config.setConfigValue((String) configData.get("value"));
                config.setDescription((String) configData.get("description"));
                config.setCategory((String) configData.get("category"));
                config.setIsSystem((Boolean) configData.get("isSystem"));
                config.setIsEditable((Boolean) configData.get("isEditable"));
                config.setCreateTime(new Date());
                config.setLastUpdateTime(new Date());
                
                systemConfigRepository.save(config);
            }
        }
    }

    @Override
    public String getConfigValueAsString(String configKey, String defaultValue) {
        return systemConfigRepository.findByConfigKey(configKey)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Override
    public Integer getConfigValueAsInteger(String configKey, Integer defaultValue) {
        String value = getConfigValueAsString(configKey, null);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Boolean getConfigValueAsBoolean(String configKey, Boolean defaultValue) {
        String value = getConfigValueAsString(configKey, null);
        if (value == null) {
            return defaultValue;
        }
        
        return "true".equalsIgnoreCase(value);
    }
    
    // Helper method to create config map
    private Map<String, Object> createConfigMap(String value, String description, String category, 
                                               Boolean isSystem, Boolean isEditable) {
        Map<String, Object> map = new HashMap<>();
        map.put("value", value);
        map.put("description", description);
        map.put("category", category);
        map.put("isSystem", isSystem);
        map.put("isEditable", isEditable);
        return map;
    }
}
