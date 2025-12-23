package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 假期系统配置实体
 * 存储假期管理相关的系统配置参数
 */
@Entity
@Table(name = "leave_system_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveSystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置键
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /**
     * 配置值
     */
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    /**
     * 配置类型：STRING, INTEGER, BOOLEAN, DECIMAL
     */
    @Column(name = "config_type", nullable = false, length = 20)
    private String configType;

    /**
     * 配置描述
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * 是否为系统配置（系统配置不可删除）
     */
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem;

    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新人ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.configType == null) {
            this.configType = "STRING";
        }
        if (this.isSystem == null) {
            this.isSystem = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取字符串类型的配置值
     * @return 字符串值
     */
    public String getStringValue() {
        return configValue;
    }

    /**
     * 获取整数类型的配置值
     * @return 整数值
     */
    public Integer getIntValue() {
        try {
            return Integer.parseInt(configValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取布尔类型的配置值
     * @return 布尔值
     */
    public Boolean getBooleanValue() {
        return "true".equalsIgnoreCase(configValue) || "1".equals(configValue);
    }

    /**
     * 获取双精度浮点类型的配置值
     * @return 双精度浮点值
     */
    public Double getDoubleValue() {
        try {
            return Double.parseDouble(configValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 设置字符串类型的配置值
     * @param value 字符串值
     */
    public void setStringValue(String value) {
        this.configValue = value;
        this.configType = "STRING";
    }

    /**
     * 设置整数类型的配置值
     * @param value 整数值
     */
    public void setIntValue(Integer value) {
        this.configValue = value != null ? value.toString() : null;
        this.configType = "INTEGER";
    }

    /**
     * 设置布尔类型的配置值
     * @param value 布尔值
     */
    public void setBooleanValue(Boolean value) {
        this.configValue = value != null ? value.toString() : null;
        this.configType = "BOOLEAN";
    }

    /**
     * 设置双精度浮点类型的配置值
     * @param value 双精度浮点值
     */
    public void setDoubleValue(Double value) {
        this.configValue = value != null ? value.toString() : null;
        this.configType = "DECIMAL";
    }
}