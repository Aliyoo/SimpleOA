package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.LeaveSystemConfig;
import com.example.simpleoa.repository.LeaveSystemConfigRepository;
import com.example.simpleoa.service.LeaveSystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 假期系统配置服务实现类
 */
@Service
@Transactional
public class LeaveSystemConfigServiceImpl implements LeaveSystemConfigService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveSystemConfigServiceImpl.class);

    @Autowired
    private LeaveSystemConfigRepository configRepository;

    // 默认配置
    private static final Map<String, ConfigInfo> DEFAULT_CONFIGS = createDefaultConfigs();
    
    private static Map<String, ConfigInfo> createDefaultConfigs() {
        Map<String, ConfigInfo> configs = new HashMap<>();
        configs.put("leave.carryover.enabled", new ConfigInfo("true", "BOOLEAN", "是否启用假期结转功能", true));
        configs.put("leave.carryover.expire_months", new ConfigInfo("3", "INTEGER", "结转假期过期月数（年假结转后3个月过期）", true));
        configs.put("leave.carryover.max_days", new ConfigInfo("5", "INTEGER", "最大结转天数限制", true));
        configs.put("leave.annual.auto_init", new ConfigInfo("true", "BOOLEAN", "是否自动初始化年假余额", true));
        configs.put("leave.annual.init_month", new ConfigInfo("1", "INTEGER", "年假初始化月份（1表示1月）", true));
        configs.put("leave.annual.init_day", new ConfigInfo("1", "INTEGER", "年假初始化日期", true));
        configs.put("leave.balance.warning_threshold", new ConfigInfo("1", "INTEGER", "假期余额警告阈值", true));
        configs.put("leave.adjustment.max_days", new ConfigInfo("30", "INTEGER", "单次调整最大天数限制", true));
        configs.put("leave.adjustment.frequency_days", new ConfigInfo("30", "INTEGER", "同类型假期调整频率限制（天）", true));
        configs.put("leave.request.max_days", new ConfigInfo("15", "INTEGER", "单次请假最大天数限制", true));
        configs.put("leave.approval.auto_approve_days", new ConfigInfo("1", "INTEGER", "自动审批的天数阈值", false));
        configs.put("leave.notification.enabled", new ConfigInfo("true", "BOOLEAN", "是否启用请假通知", false));
        configs.put("leave.notification.email_enabled", new ConfigInfo("false", "BOOLEAN", "是否启用邮件通知", false));
        configs.put("leave.statistics.cache_hours", new ConfigInfo("24", "INTEGER", "统计数据缓存小时数", false));
        return Collections.unmodifiableMap(configs);
    }

    @Override
    @Cacheable(value = "system_config", key = "#configKey")
    public Optional<LeaveSystemConfig> getConfigByKey(String configKey) {
        logger.debug("获取配置: {}", configKey);
        return configRepository.findByConfigKey(configKey);
    }

    @Override
    public String getConfigStringValue(String configKey) {
        Optional<LeaveSystemConfig> configOpt = getConfigByKey(configKey);
        return configOpt.map(LeaveSystemConfig::getStringValue).orElse(null);
    }

    @Override
    public Integer getConfigIntegerValue(String configKey) {
        Optional<LeaveSystemConfig> configOpt = getConfigByKey(configKey);
        return configOpt.map(LeaveSystemConfig::getIntValue).orElse(null);
    }

    @Override
    public Boolean getConfigBooleanValue(String configKey) {
        Optional<LeaveSystemConfig> configOpt = getConfigByKey(configKey);
        return configOpt.map(LeaveSystemConfig::getBooleanValue).orElse(null);
    }

    @Override
    public Double getConfigDoubleValue(String configKey) {
        Optional<LeaveSystemConfig> configOpt = getConfigByKey(configKey);
        return configOpt.map(LeaveSystemConfig::getDoubleValue).orElse(null);
    }

    @Override
    public String getConfigStringValue(String configKey, String defaultValue) {
        String value = getConfigStringValue(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public Integer getConfigIntegerValue(String configKey, Integer defaultValue) {
        Integer value = getConfigIntegerValue(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public Boolean getConfigBooleanValue(String configKey, Boolean defaultValue) {
        Boolean value = getConfigBooleanValue(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    @CacheEvict(value = "system_config", key = "#configKey")
    public LeaveSystemConfig saveConfig(String configKey, String configValue, String configType,
                                        String description, Long createdBy) {
        logger.info("保存配置: {} = {}", configKey, configValue);

        // 验证配置值
        ValidationResult validation = validateConfigValue(configKey, configValue, configType);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("配置值验证失败: " + validation.getMessage());
        }

        Optional<LeaveSystemConfig> existingOpt = configRepository.findByConfigKey(configKey);
        LeaveSystemConfig config;

        if (existingOpt.isPresent()) {
            // 更新现有配置
            config = existingOpt.get();
            config.setConfigValue(configValue);
            config.setConfigType(configType);
            if (description != null) {
                config.setDescription(description);
            }
            config.setUpdatedBy(createdBy);
            config.setUpdatedAt(LocalDateTime.now());
        } else {
            // 创建新配置
            ConfigInfo defaultInfo = DEFAULT_CONFIGS.get(configKey);
            boolean isSystem = defaultInfo != null && defaultInfo.isSystem;

            config = LeaveSystemConfig.builder()
                .configKey(configKey)
                .configValue(configValue)
                .configType(configType != null ? configType : "STRING")
                .description(description != null ? description :
                    (defaultInfo != null ? defaultInfo.description : ""))
                .isSystem(isSystem)
                .createdBy(createdBy)
                .build();
        }

        LeaveSystemConfig savedConfig = configRepository.save(config);
        logger.info("配置保存成功: {} = {}", configKey, savedConfig.getConfigValue());

        return savedConfig;
    }

    @Override
    @CacheEvict(value = "system_config", key = "#configKey")
    public LeaveSystemConfig updateConfig(String configKey, String configValue, Long updatedBy) {
        logger.info("更新配置: {} = {}", configKey, configValue);

        Optional<LeaveSystemConfig> configOpt = configRepository.findByConfigKey(configKey);
        if (!configOpt.isPresent()) {
            throw new IllegalArgumentException("配置不存在: " + configKey);
        }

        LeaveSystemConfig config = configOpt.get();

        // 验证配置值
        ValidationResult validation = validateConfigValue(configKey, configValue, config.getConfigType());
        if (!validation.isValid()) {
            throw new IllegalArgumentException("配置值验证失败: " + validation.getMessage());
        }

        config.setConfigValue(configValue);
        config.setUpdatedBy(updatedBy);
        config.setUpdatedAt(LocalDateTime.now());

        LeaveSystemConfig savedConfig = configRepository.save(config);
        logger.info("配置更新成功: {} = {}", configKey, savedConfig.getConfigValue());

        return savedConfig;
    }

    @Override
    @CacheEvict(value = "system_config", key = "#configKey")
    public boolean deleteConfig(String configKey) {
        logger.info("删除配置: {}", configKey);

        Optional<LeaveSystemConfig> configOpt = configRepository.findByConfigKey(configKey);
        if (!configOpt.isPresent()) {
            logger.warn("配置不存在: {}", configKey);
            return false;
        }

        LeaveSystemConfig config = configOpt.get();
        if (config.getIsSystem()) {
            throw new IllegalArgumentException("系统配置不能删除: " + configKey);
        }

        try {
            configRepository.delete(config);
            logger.info("配置删除成功: {}", configKey);
            return true;
        } catch (Exception e) {
            logger.error("删除配置失败: {}", configKey, e);
            return false;
        }
    }

    @Override
    public List<LeaveSystemConfig> getAllConfigs() {
        logger.debug("获取所有配置");
        return configRepository.findAll();
    }

    @Override
    public List<LeaveSystemConfig> getSystemConfigs() {
        logger.debug("获取系统配置");
        return configRepository.findByIsSystemTrue();
    }

    @Override
    public List<LeaveSystemConfig> getCustomConfigs() {
        logger.debug("获取自定义配置");
        return configRepository.findByIsSystemFalse();
    }

    @Override
    @CacheEvict(value = "system_config", allEntries = true)
    public List<LeaveSystemConfig> batchSaveConfigs(List<LeaveSystemConfig> configs, Long createdBy) {
        logger.info("批量保存{}个配置", configs.size());

        List<LeaveSystemConfig> savedConfigs = new ArrayList<>();

        for (LeaveSystemConfig config : configs) {
            try {
                LeaveSystemConfig saved = saveConfig(
                    config.getConfigKey(),
                    config.getConfigValue(),
                    config.getConfigType(),
                    config.getDescription(),
                    createdBy
                );
                savedConfigs.add(saved);
            } catch (Exception e) {
                logger.error("保存配置失败: {} - {}", config.getConfigKey(), e.getMessage());
            }
        }

        logger.info("批量配置保存完成，成功：{}，失败：{}", savedConfigs.size(), configs.size() - savedConfigs.size());
        return savedConfigs;
    }

    @Override
    @CacheEvict(value = "system_config", allEntries = true)
    public int resetToDefaults(Long resetBy) {
        logger.info("重置为默认配置");

        int resetCount = 0;

        for (Map.Entry<String, ConfigInfo> entry : DEFAULT_CONFIGS.entrySet()) {
            String configKey = entry.getKey();
            ConfigInfo configInfo = entry.getValue();

            try {
                saveConfig(configKey, configInfo.defaultValue, configInfo.type,
                    configInfo.description, resetBy);
                resetCount++;
            } catch (Exception e) {
                logger.error("重置默认配置失败: {} - {}", configKey, e.getMessage());
            }
        }

        logger.info("默认配置重置完成，重置数量：{}", resetCount);
        return resetCount;
    }

    @Override
    public Map<String, String> exportConfigs() {
        logger.debug("导出配置");

        Map<String, String> configMap = new HashMap<>();
        List<LeaveSystemConfig> configs = getAllConfigs();

        for (LeaveSystemConfig config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }

        return configMap;
    }

    @Override
    @CacheEvict(value = "system_config", allEntries = true)
    public int importConfigs(Map<String, String> configMap, Long createdBy) {
        logger.info("导入{}个配置", configMap.size());

        int importCount = 0;

        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String configKey = entry.getKey();
            String configValue = entry.getValue();

            try {
                ConfigInfo defaultInfo = DEFAULT_CONFIGS.get(configKey);
                String configType = defaultInfo != null ? defaultInfo.type : "STRING";

                saveConfig(configKey, configValue, configType, null, createdBy);
                importCount++;
            } catch (Exception e) {
                logger.error("导入配置失败: {} = {} - {}", configKey, configValue, e.getMessage());
            }
        }

        logger.info("配置导入完成，成功：{}，失败：{}", importCount, configMap.size() - importCount);
        return importCount;
    }

    @Override
    public ValidationResult validateConfigValue(String configKey, String configValue, String configType) {
        if (configKey == null || configKey.trim().isEmpty()) {
            return new ValidationResult(false, "配置键不能为空");
        }

        if (configValue == null) {
            return new ValidationResult(false, "配置值不能为空");
        }

        try {
            switch (configType.toUpperCase()) {
                case "STRING":
                    // 字符串类型基本不需要验证
                    break;

                case "INTEGER":
                    try {
                        int value = Integer.parseInt(configValue);
                        // 检查特定配置的合理范围
                        if (configKey.startsWith("leave.") && (value < 0 || value > 365)) {
                            return new ValidationResult(false, "请假相关整数值应在0-365之间");
                        }
                    } catch (NumberFormatException e) {
                        return new ValidationResult(false, "无效的整数值: " + configValue);
                    }
                    break;

                case "BOOLEAN":
                    if (!"true".equalsIgnoreCase(configValue) && !"false".equalsIgnoreCase(configValue) &&
                        !"1".equals(configValue) && !"0".equals(configValue)) {
                        return new ValidationResult(false, "布尔值必须是true/false或1/0");
                    }
                    break;

                case "DECIMAL":
                    try {
                        double value = Double.parseDouble(configValue);
                        if (value < 0) {
                            return new ValidationResult(false, "数值不能为负数");
                        }
                    } catch (NumberFormatException e) {
                        return new ValidationResult(false, "无效的数值: " + configValue);
                    }
                    break;

                default:
                    return new ValidationResult(false, "不支持的配置类型: " + configType);
            }

            return new ValidationResult(true, "验证通过");

        } catch (Exception e) {
            return new ValidationResult(false, "验证过程中发生错误: " + e.getMessage());
        }
    }
    
    @Override
    public ValidationResult validateBalanceConfiguration() {
        try {
            // 检查关键配置是否存在且有效
            String[] requiredConfigs = {
                "leave.carryover.enabled",
                "leave.carryover.expire_months", 
                "leave.carryover.max_days",
                "leave.annual.auto_init"
            };
            
            for (String configKey : requiredConfigs) {
                Optional<LeaveSystemConfig> config = getConfigByKey(configKey);
                if (!config.isPresent()) {
                    return new ValidationResult(false, "缺少必要配置: " + configKey);
                }
            }
            
            return new ValidationResult(true, "假期余额配置验证通过");
        } catch (Exception e) {
            logger.error("验证假期余额配置时发生错误", e);
            return new ValidationResult(false, "配置验证失败: " + e.getMessage());
        }
    }

    /**
     * 配置信息内部类
     */
    private static class ConfigInfo {
        private final String defaultValue;
        private final String type;
        private final String description;
        private final boolean isSystem;

        public ConfigInfo(String defaultValue, String type, String description, boolean isSystem) {
            this.defaultValue = defaultValue;
            this.type = type;
            this.description = description;
            this.isSystem = isSystem;
        }

        public String getDefaultValue() { return defaultValue; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public boolean isSystem() { return isSystem; }
    }
}