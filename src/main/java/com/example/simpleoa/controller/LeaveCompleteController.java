package com.example.simpleoa.controller;

import com.example.simpleoa.model.LeaveBalance;
import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.model.LeaveStatus;
import com.example.simpleoa.model.LeaveType;
import com.example.simpleoa.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
@CrossOrigin(origins = "*")
public class LeaveCompleteController {
    
    @Autowired
    private LeaveService leaveService;
    
    // 申请请假
    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(@RequestBody LeaveRequest leaveRequest) {
        try {
            LeaveRequest created = leaveService.applyLeave(leaveRequest);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            e.printStackTrace(); // 临时添加错误日志
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // 获取我的请假申请
    @GetMapping("/my-applications")
    public ResponseEntity<List<LeaveRequest>> getMyApplications() {
        List<LeaveRequest> applications = leaveService.getMyApplications();
        return ResponseEntity.ok(applications);
    }
    
    // 分页获取我的请假申请
    @GetMapping("/my-applications/page")
    public ResponseEntity<Page<LeaveRequest>> getMyApplicationsPage(Pageable pageable) {
        Page<LeaveRequest> applications = leaveService.getMyApplicationsPage(pageable);
        return ResponseEntity.ok(applications);
    }
    
    // 获取待审批列表
    @GetMapping("/approval-list")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'ADMIN')")
    public ResponseEntity<List<LeaveRequest>> getApprovalList() {
        List<LeaveRequest> approvals = leaveService.getApprovalList();
        return ResponseEntity.ok(approvals);
    }
    
    // 审批通过
    @PostMapping("/approve/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'ADMIN')")
    public ResponseEntity<LeaveRequest> approveLeave(@PathVariable Long id, 
                                                    @RequestParam(required = false) String comment) {
        try {
            LeaveRequest approved = leaveService.approveLeave(id, comment);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    // 驳回申请
    @PostMapping("/reject/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'ADMIN')")
    public ResponseEntity<LeaveRequest> rejectLeave(@PathVariable Long id, 
                                                   @RequestParam String comment) {
        try {
            LeaveRequest rejected = leaveService.rejectLeave(id, comment);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    // 撤销申请
    @PostMapping("/cancel/{id}")
    public ResponseEntity<LeaveRequest> cancelLeave(@PathVariable Long id) {
        try {
            LeaveRequest cancelled = leaveService.cancelLeave(id);
            return ResponseEntity.ok(cancelled);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    // 获取请假统计
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLeaveStatistics(
            @RequestParam String startDate, 
            @RequestParam String endDate) {
        Map<String, Object> statistics = leaveService.getLeaveStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }
    
    // 获取部门统计
    @GetMapping("/statistics/department")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentStatistics() {
        Map<String, Object> statistics = leaveService.getDepartmentStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    // 获取个人统计
    @GetMapping("/statistics/personal")
    public ResponseEntity<Map<String, Object>> getPersonalStatistics(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }
        Map<String, Object> statistics = leaveService.getPersonalStatistics(userId, year);
        return ResponseEntity.ok(statistics);
    }
    
    // 获取请假余额
    @GetMapping("/balance")
    public ResponseEntity<List<LeaveBalance>> getLeaveBalance(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }
        List<LeaveBalance> balances = leaveService.getUserLeaveBalance(userId, year);
        return ResponseEntity.ok(balances);
    }
    
    // 初始化用户请假余额
    @PostMapping("/balance/init")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> initializeLeaveBalance(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer year) {
        if (year == null) {
            year = Year.now().getValue();
        }
        leaveService.initializeUserLeaveBalance(userId, year);
        Map<String, String> response = new HashMap<>();
        response.put("message", "请假余额初始化成功");
        return ResponseEntity.ok(response);
    }
    
    // 根据状态查询
    @GetMapping("/by-status")
    public ResponseEntity<List<LeaveRequest>> getLeavesByStatus(@RequestParam LeaveStatus status) {
        List<LeaveRequest> leaves = leaveService.getLeavesByStatus(status);
        return ResponseEntity.ok(leaves);
    }
    
    // 根据类型查询
    @GetMapping("/by-type")
    public ResponseEntity<List<LeaveRequest>> getLeavesByType(@RequestParam LeaveType type) {
        List<LeaveRequest> leaves = leaveService.getLeavesByType(type);
        return ResponseEntity.ok(leaves);
    }
    
    // 导出请假记录
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLeaveRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            byte[] excelData = leaveService.exportLeaveRecords(userId, startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "leave_records.xlsx");
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    
    // 获取请假类型列表
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getLeaveTypes() {
        Map<String, Object> types = new HashMap<>();
        for (LeaveType type : LeaveType.values()) {
            Map<String, Object> typeInfo = new HashMap<>();
            typeInfo.put("name", type.name());
            typeInfo.put("displayName", type.getDisplayName());
            typeInfo.put("defaultDays", type.getDefaultDays());
            types.put(type.name(), typeInfo);
        }
        return ResponseEntity.ok(types);
    }
    
    // 获取请假状态列表
    @GetMapping("/statuses")
    public ResponseEntity<LeaveStatus[]> getLeaveStatuses() {
        return ResponseEntity.ok(LeaveStatus.values());
    }
}
