package com.example.simpleoa.controller;

import com.example.simpleoa.model.*;
import com.example.simpleoa.service.ApprovalFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approval")
public class ApprovalFlowController {
    private final ApprovalFlowService approvalFlowService;

    @Autowired
    public ApprovalFlowController(ApprovalFlowService approvalFlowService) {
        this.approvalFlowService = approvalFlowService;
    }

    @PostMapping
    public ApprovalFlow createApprovalFlow(@RequestBody WorkTimeRecord workTimeRecord, @RequestBody User approver) {
        if (workTimeRecord == null || approver == null) {
            throw new IllegalArgumentException("WorkTimeRecord and approver cannot be null");
        }
        if (workTimeRecord.getId() == null || approver.getId() == null) {
            throw new IllegalArgumentException("WorkTimeRecord and approver must have valid IDs");
        }
        // if (approver.getId().equals(workTimeRecord.getUser().getId())) {
        //     throw new IllegalArgumentException("Approver cannot be the same person as the work time record submitter");
        // }
        if (workTimeRecord.getStatus() == null || !workTimeRecord.getStatus().equals("SUBMITTED")) {
            throw new IllegalStateException("WorkTimeRecord must be in SUBMITTED status for approval");
        }
        if (workTimeRecord.getHours() == null || workTimeRecord.getHours() <= 0) {
            throw new IllegalArgumentException("Work time hours must be positive");
        }
        if (workTimeRecord.getDate() == null) {
            throw new IllegalArgumentException("Work time date cannot be null");
        }
        return approvalFlowService.createApprovalFlow(workTimeRecord, approver);
    }

    @PutMapping("/{flowId}")
    public ApprovalFlow updateApprovalFlowStatus(
            @PathVariable Long flowId,
            @RequestParam String status,
            @RequestParam(required = false) String comment) {
        if (flowId == null || status == null) {
            throw new IllegalArgumentException("flowId and status cannot be null");
        }
        if (!status.matches("PENDING|APPROVED|REJECTED")) {
            throw new IllegalArgumentException("Invalid status value");
        }
        ApprovalFlow currentFlow = approvalFlowService.getApprovalFlowById(flowId);
        if (currentFlow == null) {
            throw new IllegalArgumentException("Approval flow not found");
        }
        if ("APPROVED".equals(currentFlow.getStatus()) || "REJECTED".equals(currentFlow.getStatus())) {
            throw new IllegalStateException("Cannot update status of a finalized approval");
        }
        if ("PENDING".equals(currentFlow.getStatus()) && "REJECTED".equals(status)) {
            throw new IllegalStateException("Cannot directly reject a pending approval");
        }
        if (comment != null && comment.length() > 500) {
            throw new IllegalArgumentException("Comment cannot exceed 500 characters");
        }
        return approvalFlowService.updateApprovalFlowStatus(flowId, status, comment);
    }

    @GetMapping("/worktime/{workTimeRecordId}")
    public List<ApprovalFlow> getApprovalFlowsByWorkTimeRecord(@PathVariable Long workTimeRecordId) {
        return approvalFlowService.getApprovalFlowsByWorkTimeRecord(workTimeRecordId);
    }

    @GetMapping("/approver/{approverId}")
    public List<ApprovalFlow> getApprovalFlowsByApprover(@PathVariable Long approverId) {
        return approvalFlowService.getApprovalFlowsByApprover(approverId);
    }

    @GetMapping("/{id}")
    public ApprovalFlow getApprovalFlowById(@PathVariable Long id) {
        return approvalFlowService.getApprovalFlowById(id);
    }

    @GetMapping("/history/{flowId}")
    public List<ApprovalFlow> getApprovalHistory(@PathVariable Long flowId) {
        return approvalFlowService.getApprovalHistory(flowId);
    }

    @GetMapping("/all")
    public List<ApprovalFlow> getAllApprovals() {
        return approvalFlowService.getAllApprovals();
    }

    @GetMapping("/my-approvals")
    public List<ApprovalFlow> getMyApprovals(
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long projectId) {

        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User currentUser = (User) authentication.getPrincipal();
            Long approverId = currentUser.getId();

            // 处理日期参数
            Date start = null;
            Date end = null;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            try {
                if (startDate != null && !startDate.isEmpty()) {
                    start = dateFormat.parse(startDate);
                }
                if (endDate != null && !endDate.isEmpty()) {
                    end = dateFormat.parse(endDate);
                    // 设置为当天结束时间
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(end);
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    end = calendar.getTime();
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd");
            }

            // 根据筛选条件查询
            List<ApprovalFlow> approvals;

            // 按项目ID筛选（仅适用于工时审批）
            if (projectId != null && projectId > 0) {
                approvals = approvalFlowService.getApprovalFlowsByProjectId(projectId);
                // 还需要筛选当前用户的审批
                approvals.removeIf(approval -> !approval.getApprover().getId().equals(approverId));
            }
            // 按日期范围、类型和状态筛选
            else if (start != null && end != null && requestType != null && !requestType.isEmpty()
                    && status != null && !status.isEmpty()) {
                approvals = approvalFlowService.getApprovalFlowsByApproverAndDateRangeAndTypeAndStatus(
                        approverId, start, end, requestType, status);
            }
            // 按日期范围和类型筛选
            else if (start != null && end != null && requestType != null && !requestType.isEmpty()) {
                approvals = approvalFlowService.getApprovalFlowsByApproverAndDateRangeAndType(
                        approverId, start, end, requestType);
            }
            // 按日期范围和状态筛选
            else if (start != null && end != null && status != null && !status.isEmpty()) {
                approvals = approvalFlowService.getApprovalFlowsByApproverAndDateRangeAndStatus(
                        approverId, start, end, status);
            }
            // 按日期范围筛选
            else if (start != null && end != null) {
                approvals = approvalFlowService.getApprovalFlowsByApproverAndDateRange(
                        approverId, start, end);
            }
            // 按类型和状态筛选
            else if (requestType != null && !requestType.isEmpty() && status != null && !status.isEmpty()) {
                approvals = approvalFlowService.getApprovalFlowsByApproverAndTypeAndStatus(
                        approverId, requestType, status);
            }
            // 按类型筛选
            else if (requestType != null && !requestType.isEmpty()) {
                approvals = approvalFlowService.getApprovalFlowsByApproverAndType(
                        approverId, requestType);
            }
            // 按状态筛选
            else if (status != null && !status.isEmpty()) {
                approvals = approvalFlowService.getApprovalFlowsByApproverAndStatus(
                        approverId, status);
            }
            // 无筛选条件，获取所有审批
            else {
                approvals = approvalFlowService.getApprovalFlowsByApprover(approverId);
            }

            // 输出调试信息
            System.out.println("Found " + approvals.size() + " approvals for user: " +
                    currentUser.getUsername() + " (ID: " + approverId + ")");

            return approvals;
        }

        return new ArrayList<>();
    }

    @GetMapping("/my-approvals/paged")
    public Page<ApprovalFlow> getMyApprovalsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User currentUser = (User) authentication.getPrincipal();
            return approvalFlowService.getMyApprovalsPaged(currentUser.getId(), page, size);
        }

        throw new IllegalStateException("User not authenticated");
    }

    @PostMapping("/notify/{flowId}")
    public void notifyStatusChange(@PathVariable Long flowId, @RequestParam String newStatus) {
        if (flowId == null || newStatus == null) {
            throw new IllegalArgumentException("flowId and newStatus cannot be null");
        }
        if (!newStatus.matches("PENDING|APPROVED|REJECTED")) {
            throw new IllegalArgumentException("Invalid status value");
        }
        ApprovalFlow currentFlow = approvalFlowService.getApprovalFlowById(flowId);
        if (currentFlow == null) {
            throw new IllegalArgumentException("Approval flow not found");
        }
        if ("APPROVED".equals(currentFlow.getStatus()) && "PENDING".equals(newStatus)) {
            throw new IllegalStateException("Cannot revert approved approval to pending");
        }
        approvalFlowService.notifyStatusChange(flowId, newStatus);
    }

    @PostMapping("/batch-approve")
    public ResponseEntity<?> batchApprove(@RequestBody List<Long> flowIds, @RequestParam(required = false) String comment) {
        if (flowIds == null || flowIds.isEmpty()) {
            return ResponseEntity.badRequest().body("No approval flows specified");
        }

        int updatedCount = approvalFlowService.batchUpdateApprovalStatus(flowIds, "APPROVED", comment);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", String.format("Successfully approved %d approval flows", updatedCount));
        response.put("updatedCount", updatedCount);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch-reject")
    public ResponseEntity<?> batchReject(@RequestBody List<Long> flowIds, @RequestParam(required = false) String comment) {
        if (flowIds == null || flowIds.isEmpty()) {
            return ResponseEntity.badRequest().body("No approval flows specified");
        }

        int updatedCount = approvalFlowService.batchUpdateApprovalStatus(flowIds, "REJECTED", comment);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", String.format("Successfully rejected %d approval flows", updatedCount));
        response.put("updatedCount", updatedCount);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/leave")
    public ApprovalFlow createLeaveApproval(@RequestBody LeaveRequest leaveRequest, @RequestBody User approver) {
        if (leaveRequest == null || approver == null) {
            throw new IllegalArgumentException("LeaveRequest and approver cannot be null");
        }
        if (leaveRequest.getId() == null || approver.getId() == null) {
            throw new IllegalArgumentException("LeaveRequest and approver must have valid IDs");
        }
        if (approver.getId().equals(leaveRequest.getUserId())) {
            throw new IllegalArgumentException("Approver cannot be the same person as the leave request submitter");
        }
        if (leaveRequest.getStatus() == null || !leaveRequest.getStatus().equals("SUBMITTED")) {
            throw new IllegalStateException("LeaveRequest must be in SUBMITTED status for approval");
        }
        if (leaveRequest.getStartDate() == null || leaveRequest.getEndDate() == null) {
            throw new IllegalArgumentException("Leave start and end dates cannot be null");
        }
        if (leaveRequest.getStartDate().isAfter(leaveRequest.getEndDate())) {
            throw new IllegalArgumentException("Leave start date must be before end date");
        }
        if (leaveRequest.getLeaveType() == null) {
            throw new IllegalArgumentException("Leave type cannot be null");
        }
        return approvalFlowService.createLeaveApproval(leaveRequest, approver);
    }

    @PostMapping("/business-trip")
    public ApprovalFlow createBusinessTripApproval(@RequestBody BusinessTripRequest tripRequest, @RequestBody User approver) {
        if (tripRequest == null || approver == null) {
            throw new IllegalArgumentException("BusinessTripRequest and approver cannot be null");
        }
        if (tripRequest.getId() == null || approver.getId() == null) {
            throw new IllegalArgumentException("BusinessTripRequest and approver must have valid IDs");
        }
        if (approver.getId().equals(tripRequest.getUserId())) {
            throw new IllegalArgumentException("Approver cannot be the same person as the trip request submitter");
        }
        if (tripRequest.getStatus() == null || !tripRequest.getStatus().equals("SUBMITTED")) {
            throw new IllegalStateException("BusinessTripRequest must be in SUBMITTED status for approval");
        }
        if (tripRequest.getStartDate() == null || tripRequest.getEndDate() == null) {
            throw new IllegalArgumentException("Trip start and end dates cannot be null");
        }
        if (tripRequest.getStartDate().isAfter(tripRequest.getEndDate())) {
            throw new IllegalArgumentException("Trip start date must be before end date");
        }
        if (tripRequest.getDestination() == null || tripRequest.getDestination().isEmpty()) {
            throw new IllegalArgumentException("Trip destination cannot be empty");
        }
        if (tripRequest.getPurpose() == null || tripRequest.getPurpose().isEmpty()) {
            throw new IllegalArgumentException("Trip purpose cannot be empty");
        }
        return approvalFlowService.createBusinessTripApproval(tripRequest, approver);
    }

    @PostMapping("/reimbursement")
    public ApprovalFlow createReimbursementApproval(@RequestBody ReimbursementRequest reimbursementRequest, @RequestBody User approver) {
        if (reimbursementRequest == null || approver == null) {
            throw new IllegalArgumentException("ReimbursementRequest and approver cannot be null");
        }
        if (reimbursementRequest.getId() == null || approver.getId() == null) {
            throw new IllegalArgumentException("ReimbursementRequest and approver must have valid IDs");
        }
        if (approver.getId().equals(reimbursementRequest.getUserId())) {
            throw new IllegalArgumentException("Approver cannot be the same person as the reimbursement submitter");
        }
        if (reimbursementRequest.getStatus() == null || !reimbursementRequest.getStatus().equals("SUBMITTED")) {
            throw new IllegalStateException("ReimbursementRequest must be in SUBMITTED status for approval");
        }
        if (reimbursementRequest.getAmount() == null || reimbursementRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reimbursement amount must be positive");
        }
        if (reimbursementRequest.getType() == null) {
            throw new IllegalArgumentException("Reimbursement type cannot be null");
        }
        if (reimbursementRequest.getDate() == null) {
            throw new IllegalArgumentException("Reimbursement date cannot be null");
        }
        if (reimbursementRequest.getAttachments() == null || reimbursementRequest.getAttachments().isEmpty()) {
            throw new IllegalArgumentException("Reimbursement must have at least one attachment");
        }
        return approvalFlowService.createReimbursementApproval(reimbursementRequest, approver);
    }
}