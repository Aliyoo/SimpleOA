package com.example.simpleoa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User approver;

    @ManyToOne
    private WorkTimeRecord workTimeRecord;

    @ManyToOne
    private LeaveRequest leaveRequest;

    @ManyToOne
    private BusinessTripRequest businessTripRequest;

    @ManyToOne
    private ReimbursementRequest reimbursementRequest;

    private String requestType; // WORKTIME, LEAVE, BUSINESS_TRIP, REIMBURSEMENT

    private String status;
    private String comment;
    private Date createTime;
    private Date updateTime;
    private Date approvalTime;
}