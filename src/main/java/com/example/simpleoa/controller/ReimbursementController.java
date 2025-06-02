package com.example.simpleoa.controller;

import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.service.ReimbursementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oa/reimbursement")
public class ReimbursementController {
    private final ReimbursementService reimbursementService;

    @Autowired
    public ReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    @PostMapping("/apply")
    public ReimbursementRequest createReimbursement(@RequestBody ReimbursementRequest reimbursement) {
        return reimbursementService.createReimbursement(reimbursement);
    }

    @PutMapping("/{id}")
    public ReimbursementRequest updateReimbursement(@PathVariable Long id, @RequestBody ReimbursementRequest reimbursement) {
        reimbursement.setId(id);
        return reimbursementService.updateReimbursement(reimbursement);
    }

    @DeleteMapping("/{id}")
    public void deleteReimbursement(@PathVariable Long id) {
        reimbursementService.deleteReimbursement(id);
    }

    @GetMapping("/{id}")
    public ReimbursementRequest getReimbursementById(@PathVariable Long id) {
        return reimbursementService.getReimbursementById(id);
    }

    @GetMapping("/records")
    public List<ReimbursementRequest> getAllReimbursements(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (userId != null) {
            return (List<ReimbursementRequest>) reimbursementService.getReimbursementsByUser(userId);
        }
        // Additional filtering logic can be added here based on status, startDate, and endDate
        return (List<ReimbursementRequest>) reimbursementService.getAllReimbursements();
    }

    @GetMapping("/user/{userId}")
    public List<ReimbursementRequest> getReimbursementsByUser(@PathVariable Long userId) {
        return (List<ReimbursementRequest>) reimbursementService.getReimbursementsByUser(userId);
    }

    @PostMapping("/approve/{id}")
    public ReimbursementRequest approveReimbursement(@PathVariable Long id,
                                            @RequestBody Map<String, String> approvalData) {
        String status = approvalData.get("status");
        String comment = approvalData.get("comment");
        return reimbursementService.approveReimbursement(id, status, comment);
    }
}