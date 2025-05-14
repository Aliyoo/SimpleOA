package com.example.simpleoa.controller;

import com.example.simpleoa.model.SystemConfig;
import com.example.simpleoa.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/system-config")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * Get all system configurations
     */
    @GetMapping
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        return ResponseEntity.ok(systemConfigService.findAllConfigs());
    }

    /**
     * Get system configuration by id
     */
    @GetMapping("/{id}")
    public ResponseEntity<SystemConfig> getConfigById(@PathVariable Long id) {
        return systemConfigService.findConfigById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get system configuration by key
     */
    @GetMapping("/key/{configKey}")
    public ResponseEntity<SystemConfig> getConfigByKey(@PathVariable String configKey) {
        return systemConfigService.findConfigByKey(configKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get system configurations by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<SystemConfig>> getConfigsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(systemConfigService.findConfigsByCategory(category));
    }

    /**
     * Create new system configuration
     */
    @PostMapping
    public ResponseEntity<SystemConfig> createConfig(@RequestBody SystemConfig systemConfig) {
        return new ResponseEntity<>(systemConfigService.saveConfig(systemConfig), HttpStatus.CREATED);
    }

    /**
     * Update system configuration
     */
    @PutMapping("/{id}")
    public ResponseEntity<SystemConfig> updateConfig(@PathVariable Long id, @RequestBody SystemConfig systemConfig) {
        if (!systemConfigService.findConfigById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        systemConfig.setId(id);
        return ResponseEntity.ok(systemConfigService.saveConfig(systemConfig));
    }

    /**
     * Delete system configuration
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        if (!systemConfigService.findConfigById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        try {
            systemConfigService.deleteConfig(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Update configuration value by key
     */
    @PatchMapping("/key/{configKey}")
    public ResponseEntity<SystemConfig> updateConfigValue(
            @PathVariable String configKey,
            @RequestBody Map<String, String> payload) {
        String configValue = payload.get("value");
        if (configValue == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            return ResponseEntity.ok(systemConfigService.updateConfigValue(configKey, configValue));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Batch update configuration values
     */
    @PatchMapping("/batch")
    public ResponseEntity<List<SystemConfig>> batchUpdateConfigs(@RequestBody Map<String, String> configMap) {
        return ResponseEntity.ok(systemConfigService.batchUpdateConfigs(configMap));
    }

    /**
     * Initialize system with default configurations
     */
    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeDefaultConfigs() {
        systemConfigService.initializeDefaultConfigs();
        return ResponseEntity.ok().build();
    }
}
