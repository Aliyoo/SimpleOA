package com.example.simpleoa.controller;

import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {
    private final LeaveService leaveService;

    @Autowired
    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/apply")
    public LeaveRequest applyLeave(@RequestBody LeaveRequest leaveRequest) {
        return leaveService.applyLeave(leaveRequest);
    }

    @GetMapping("/approval-list")
    public List<LeaveRequest> getApprovalList() {
        return leaveService.getApprovalList();
    }

    @GetMapping("/my-applications")
    public List<LeaveRequest> getMyApplications() {
        return leaveService.getMyApplications();
    }

    @PostMapping("/approve/{id}")
    public LeaveRequest approveLeave(@PathVariable Long id) {
        return leaveService.approveLeave(id, "已通过", "同意");
    }

    @PostMapping("/reject/{id}")
    public LeaveRequest rejectLeave(@PathVariable Long id, @RequestBody(required = false) String comment) {
        return leaveService.rejectLeave(id, comment);
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getLeaveStatistics(@RequestParam String startDate, @RequestParam String endDate) {
        return ResponseEntity.ok(leaveService.getLeaveStatistics(startDate, endDate));
    }
}