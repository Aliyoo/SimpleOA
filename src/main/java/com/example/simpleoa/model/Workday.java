package com.example.simpleoa.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.util.Date;

/**
 * 工作日模型 - 用于定义哪些日子是工作日
 * 主要用途：
 * 1. 记录调休补班日（原本是周末，但需要上班）
 * 2. 记录特殊工作安排日
 * 
 * 业务逻辑：
 * - 默认周一到周五是工作日
 * - 如果某个周末日期在此表中，则该日期也是工作日
 * - 如果某个工作日在 Holiday 表中，则该日期不是工作日（节假日优先级更高）
 */
@Entity
@Table(name = "workday")
public class Workday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date date;
    
    // 描述：如调休、补班等
    private String description;
    
    // 工作日类型：NORMAL(正常工作日), MAKEUP(调休补班), OVERTIME(加班日)
    @Column(name = "work_type")
    private String workType = "NORMAL";
    
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    // 构造函数
    public Workday() {}
    
    public Workday(Date date, String description, String workType) {
        this.date = date;
        this.description = description;
        this.workType = workType;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
