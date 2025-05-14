package com.example.simpleoa.controller;

import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {
    private final LeaveService leaveService;

    @Autowired
    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    public LeaveRequest createLeave(@RequestBody LeaveRequest leaveRequest) {
        return leaveService.createLeave(leaveRequest);
    }

    @PutMapping("/{id}")
    public LeaveRequest updateLeave(@PathVariable Long id, @RequestBody LeaveRequest leaveRequest) {
        leaveRequest.setId(id);
        return leaveService.updateLeave(leaveRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteLeave(@PathVariable Long id) {
        leaveService.deleteLeave(id);
    }

    @GetMapping("/{id}")
    public LeaveRequest getLeaveById(@PathVariable Long id) {
        return leaveService.getLeaveById(id);
    }

    @GetMapping
    public List<LeaveRequest> getAllLeaves() {
        return (List<LeaveRequest>) leaveService.getAllLeaves();
    }

    @GetMapping("/user/{userId}")
    public List<LeaveRequest> getLeavesByUser(@PathVariable Long userId) {
        return (List<LeaveRequest>) leaveService.getLeavesByUser(userId);
    }

    @PutMapping("/{id}/approve")
    public LeaveRequest approveLeave(@PathVariable Long id, 
                                   @RequestParam String status, 
                                   @RequestParam(required = false) String comment) {
        return leaveService.approveLeave(id, status, comment);
    }
}