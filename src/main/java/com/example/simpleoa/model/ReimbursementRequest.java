package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReimbursementRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String expenseType;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;
    private String status;
    private String comment;

    @ElementCollection
    private List<String> attachments;

    // 这些字段是为了与ApprovalFlowController中的验证保持一致
    private String type; // 与expenseType相同，保留两个字段以兼容现有代码

    @ManyToOne
    private User applicant;

    // 为了与ApprovalFlowController中的验证保持一致
    public LocalDate getDate() {
        return expenseDate;
    }

    public void setDate(LocalDate date) {
        this.expenseDate = date;
    }

    public Long getUserId() {
        return applicant != null ? applicant.getId() : null;
    }

    public void setType(String type) {
        this.type = type;
        // 同步更新expenseType字段
        this.expenseType = type;
    }
}