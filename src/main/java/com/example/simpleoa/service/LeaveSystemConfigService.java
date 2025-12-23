package com.example.simpleoa.service;

import com.example.simpleoa.model.LeaveSystemConfig;

import java.util.List;
import java.util.Optional;

/**
 * 假期系统配置服务接口
 */
public interface LeaveSystemConfigService {

    /**
     * 根据配置键获取配置
     * @param configKey 配置键
     * @return 配置
     */
    Optional<LeaveSystemConfig> getConfigByKey(String configKey);

    /**
     * 获取配置的字符串值
     * @param configKey 配置键
     * @return 字符串值
     */
    String getConfigStringValue(String configKey);

    /**
     * 获取配置的整数值
     * @param configKey 配置键
     * @return 整数值
     */
    Integer getConfigIntegerValue(String configKey);

    /**
     * 获取配置的布尔值
     * @param configKey 配置键
     * @return 布尔值
     */
    Boolean getConfigBooleanValue(String configKey);

    /**
     * 获取配置的双精度浮点值
     * @param configKey 配置键
     * @return 双精度浮点值
     */
    Double getConfigDoubleValue(String configKey);

    /**
     * 获取配置的字符串值（带默认值）
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    String getConfigStringValue(String configKey, String defaultValue);

    /**
     * 获取配置的整数值（带默认值）
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    Integer getConfigIntegerValue(String configKey, Integer defaultValue);

    /**
     * 获取配置的布尔值（带默认值）
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    Boolean getConfigBooleanValue(String configKey, Boolean defaultValue);

    /**
     * 创建或更新配置
     * @param configKey 配置键
     * @param configValue 配置值
     * @param configType 配置类型
     * @param description 描述
     * @param createdBy 创建人ID
     * @return 保存的配置
     */
    LeaveSystemConfig saveConfig(String configKey, String configValue, String configType,
                               String description, Long createdBy);

    /**
     * 更新配置值
     * @param configKey 配置键
     * @param configValue 新的配置值
     * @param updatedBy 更新人ID
     * @return 更新后的配置
     */
    LeaveSystemConfig updateConfig(String configKey, String configValue, Long updatedBy);

    /**
     * 删除配置
     * @param configKey 配置键
     * @return 是否删除成功
     */
    boolean deleteConfig(String configKey);

    /**
     * 获取所有配置
     * @return 配置列表
     */
    List<LeaveSystemConfig> getAllConfigs();

    /**
     * 获取所有系统配置
     * @return 系统配置列表
     */
    List<LeaveSystemConfig> getSystemConfigs();

    /**
     * 获取所有自定义配置
     * @return 自定义配置列表
     */
    List<LeaveSystemConfig> getCustomConfigs();

    /**
     * 批量保存配置
     * @param configs 配置列表
     * @param createdBy 创建人ID
     * @return 保存的配置列表
     */
    List<LeaveSystemConfig> batchSaveConfigs(List<LeaveSystemConfig> configs, Long createdBy);

    /**
     * 重置为默认配置
     * @param resetBy 重置人ID
     * @return 重置的配置数量
     */
    int resetToDefaults(Long resetBy);

    /**
     * 导出配置到Map
     * @return 配置Map
     */
    java.util.Map<String, String> exportConfigs();

    /**
     * 从Map导入配置
     * @param configMap 配置Map
     * @param createdBy 创建人ID
     * @return 导入的配置数量
     */
    int importConfigs(java.util.Map<String, String> configMap, Long createdBy);

    /**
     * 验证配置值的有效性
     * @param configKey 配置键
     * @param configValue 配置值
     * @param configType 配置类型
     * @return 验证结果
     */
    ValidationResult validateConfigValue(String configKey, String configValue, String configType);
    
    /**
     * 验证假期余额配置的完整性
     * @return 验证结果
     */
    ValidationResult validateBalanceConfiguration();

    /**
     * 验证结果类
     */
    class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}