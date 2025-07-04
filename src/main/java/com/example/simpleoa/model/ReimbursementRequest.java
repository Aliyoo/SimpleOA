package com.example.simpleoa.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Column(nullable = false)
    private String title; // 报销标题，例如 “2025年6月差旅报销”

    @Column(nullable = false)
    private BigDecimal totalAmount; // 总金额，由所有明细计算得出

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReimbursementStatus status; // 使用枚举表示状态

    private String comment; // 审批意见

    @ElementCollection
    @CollectionTable(name = "reimbursement_attachments", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "attachment_path")
    private List<String> attachments; // 凭证文件路径列表

    @OneToMany(mappedBy = "reimbursementRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReimbursementItem> items; // 费用明细

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (status == null) {
            status = ReimbursementStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    // Helper method to calculate total amount from items
    public void calculateTotalAmount() {
        if (this.items == null) {
            this.totalAmount = BigDecimal.ZERO;
            return;
        }
        this.totalAmount = items.stream()
                                .map(ReimbursementItem::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}