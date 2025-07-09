package com.example.simpleoa.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReimbursementRequestDTO {
    private Long id; // Good to have for updates
    private String title;
    private Long projectId; // New field for project association
    private List<ReimbursementItem> items;
    private List<String> attachments;

    // Fields that might be part of a DTO but are often set/derived server-side
    // private BigDecimal totalAmount; // Usually calculated on the server
    // private String status; // Usually managed by the server workflow
    // private String comment; // Usually for approval process
    // private Long applicantId; // Usually taken from @AuthenticationPrincipal
}
