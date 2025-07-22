package com.example.simpleoa.model;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 节假日模型 - 用于定义哪些日子是节假日
 * 节假日优先级高于工作日，即如果某个日期既在节假日表中又在工作日表中，
 * 那么该日期被认为是节假日而不是工作日
 */
@Entity
@Table(name = "holiday")
public class Holiday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Date date;
    
    @Column(nullable = false)
    private String name;
    
    // 节假日类型：PUBLIC(法定节假日), COMPANY(公司节假日), OTHER(其他)
    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'PUBLIC'")
    private String type = "PUBLIC";
    
    // 节假日描述
    private String description;
    
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    // 构造函数
    public Holiday() {}
    
    public Holiday(Date date, String name, String type) {
        this.date = date;
        this.name = name;
        this.type = type;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
