package com.example.simpleoa.controller;

import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.service.ReimbursementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reimbursements")
public class ReimbursementController {
    private final ReimbursementService reimbursementService;

    @Autowired
    public ReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    @PostMapping
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

    @GetMapping
    public List<ReimbursementRequest> getAllReimbursements() {
        return (List<ReimbursementRequest>) reimbursementService.getAllReimbursements();
    }

    @GetMapping("/user/{userId}")
    public List<ReimbursementRequest> getReimbursementsByUser(@PathVariable Long userId) {
        return (List<ReimbursementRequest>) reimbursementService.getReimbursementsByUser(userId);
    }

    @PutMapping("/{id}/approve")
    public ReimbursementRequest approveReimbursement(@PathVariable Long id, 
                                            @RequestParam String status, 
                                            @RequestParam(required = false) String comment) {
        return reimbursementService.approveReimbursement(id, status, comment);
    }
}