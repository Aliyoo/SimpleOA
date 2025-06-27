package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;

    private String description;
    private String status;
    private String comment;

    @ElementCollection
    private List<String> attachments;

    // 这些字段是为了与前端保持一致
    private String type; // 与expenseType相同，保留两个字段以兼容现有代码

    @ManyToOne
    @JoinColumn(name = "applicant_id")
    private User applicant;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // 为了与前端保持一致的getter/setter
    @JsonProperty("date")
    public LocalDate getDate() {
        return expenseDate;
    }

    public void setDate(LocalDate date) {
        this.expenseDate = date;
    }

    @JsonProperty("type")
    public String getType() {
        return type != null ? type : expenseType;
    }

    public void setType(String type) {
        this.type = type;
        this.expenseType = type; // 保持两个字段同步
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
        this.type = expenseType; // 保持两个字段同步
    }

    public Long getUserId() {
        return applicant != null ? applicant.getId() : null;
    }

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}