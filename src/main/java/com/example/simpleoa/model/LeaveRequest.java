package com.example.simpleoa.model;

import com.example.simpleoa.converter.LeaveTypeConverter;
import com.example.simpleoa.converter.LeaveStatusConverter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "leave_request")
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = LeaveTypeConverter.class)
    @Column(name = "leave_type")
    private LeaveType leaveType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endDate;
    
    @Column(length = 500)
    private String reason;
    
    @Convert(converter = LeaveStatusConverter.class)
    @Column(name = "status")
    private LeaveStatus status;
    
    @Column(length = 500)
    private String comment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "applicant_id")
    @JsonIgnoreProperties({"roles", "position", "leaveRequests", "password"})
    private User applicant;
    
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @Column(name = "approve_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approver_id")
    @JsonIgnoreProperties({"roles", "position", "leaveRequests", "password"})
    private User approver;
    
    // 请假天数（包含半天的情况）
    @Column(name = "leave_days", columnDefinition = "DECIMAL(3,1)")
    private Double leaveDays;

    public Long getUserId() {
        return applicant != null ? applicant.getId() : null;
    }
    
    public int getDurationInDays() {
        if (startDate != null && endDate != null) {
            return (int) ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
        }
        return 0;
    }
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (leaveDays == null) {
            leaveDays = (double) getDurationInDays();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
