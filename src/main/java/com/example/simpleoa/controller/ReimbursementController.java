package com.example.simpleoa.controller;

import com.example.simpleoa.common.ApiResponse;
import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.ReimbursementRequestDTO;
import com.example.simpleoa.model.ReimbursementStatus;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.ReimbursementService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/oa/reimbursement")
public class ReimbursementController {

    private final ReimbursementService reimbursementService;

    public ReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReimbursementRequest>> createReimbursement(@RequestBody ReimbursementRequestDTO dto, @AuthenticationPrincipal User user) {
        ReimbursementRequest created = reimbursementService.createReimbursement(dto, user.getId());
        return ResponseEntity.ok(ApiResponse.success("报销申请已创建", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReimbursementRequest>> updateReimbursement(@PathVariable Long id, @RequestBody ReimbursementRequestDTO dto) {
        ReimbursementRequest updated = reimbursementService.updateReimbursement(id, dto);
        return ResponseEntity.ok(ApiResponse.success("报销申请已更新", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReimbursement(@PathVariable Long id) {
        reimbursementService.deleteReimbursement(id);
        return ResponseEntity.ok(ApiResponse.success("报销申请已删除", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReimbursementRequest>> getReimbursementById(@PathVariable Long id) {
        ReimbursementRequest request = reimbursementService.getReimbursementById(id);
        return ResponseEntity.ok(ApiResponse.success(request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReimbursementRequest>>> getReimbursements(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ReimbursementStatus status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        Page<ReimbursementRequest> requests = reimbursementService.getReimbursements(user.getId(), page, size, status, startDate, endDate, keyword);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @PostMapping("/{id}/approval")
    public ResponseEntity<ApiResponse<ReimbursementRequest>> approveOrRejectReimbursement(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal User approver) {
        String decision = payload.get("decision");
        String comment = payload.get("comment");
        ReimbursementRequest result = reimbursementService.approveOrReject(id, decision, comment, approver.getId());
        return ResponseEntity.ok(ApiResponse.success("审批处理成功", result));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReimbursementStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @AuthenticationPrincipal User user) {
        Map<String, Object> statistics = reimbursementService.getReimbursementStatistics(startDate, endDate, user.getId());
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @GetMapping("/status-options")
    public ResponseEntity<ApiResponse<ReimbursementStatus[]>> getReimbursementStatusOptions() {
        return ResponseEntity.ok(ApiResponse.success("获取报销状态选项成功", ReimbursementStatus.values()));
    }
}
