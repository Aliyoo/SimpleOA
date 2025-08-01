package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTripRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String destination;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endTime;
    
    // 为了兼容原有代码，保留原字段
    private LocalDate startDate;
    private LocalDate endDate;
    
    @JsonProperty("reason")
    private String purpose; // 前端使用reason，后端使用purpose
    
    private Integer days; // 出差天数
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessTripStatus status; // 使用枚举表示状态
    
    private String comment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true) // 允许为空，如果出差不特定于项目
    private Project project;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public Long getUserId() {
        return applicant != null ? applicant.getId() : null;
    }
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (status == null) {
            status = BusinessTripStatus.DRAFT;
        }
        syncDateFields();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
        syncDateFields();
    }
    
    // 数据转换方法，确保时间字段同步
    private void syncDateFields() {
        if (startTime != null) {
            startDate = startTime.toLocalDate();
        }
        if (endTime != null) {
            endDate = endTime.toLocalDate();
        }
    }
    
    // 计算出差天数
    public int getDurationInDays() {
        if (startDate != null && endDate != null) {
            return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
        return days != null ? days : 0;
    }
}