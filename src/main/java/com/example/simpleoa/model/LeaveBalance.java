package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "leave_balance")
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "total_days", nullable = false)
    private Integer totalDays;  // 总天数
    
    @Column(name = "used_days", nullable = false)
    private Integer usedDays = 0;   // 已使用天数
    
    @Column(name = "remaining_days", nullable = false)
    private Integer remainingDays;  // 剩余天数
    
    // 计算剩余天数
    public void calculateRemainingDays() {
        this.remainingDays = this.totalDays - this.usedDays;
    }
}
