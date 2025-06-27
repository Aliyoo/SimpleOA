package com.example.simpleoa.controller;

import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.ReimbursementService;
import com.example.simpleoa.service.UserService;
import com.example.simpleoa.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oa/reimbursement")
public class ReimbursementController {
    private final ReimbursementService reimbursementService;
    private final UserService userService;

    public ReimbursementController(ReimbursementService reimbursementService, UserService userService) {
        this.reimbursementService = reimbursementService;
        this.userService = userService;
    }

    @PostMapping("/apply")
    public ApiResponse<ReimbursementRequest> createReimbursement(@RequestBody ReimbursementRequest reimbursement) {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = (User) userService.loadUserByUsername(username);

            if (currentUser == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            // 设置申请人
            reimbursement.setApplicant(currentUser);

            ReimbursementRequest savedRequest = reimbursementService.createReimbursement(reimbursement);
            return ApiResponse.success("报销申请提交成功", savedRequest);
        } catch (Exception e) {
            return ApiResponse.error("提交失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<ReimbursementRequest> updateReimbursement(@PathVariable Long id,
            @RequestBody ReimbursementRequest reimbursement) {
        try {
            reimbursement.setId(id);
            ReimbursementRequest updated = reimbursementService.updateReimbursement(reimbursement);
            return ApiResponse.success("更新成功", updated);
        } catch (Exception e) {
            return ApiResponse.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReimbursement(@PathVariable Long id) {
        try {
            reimbursementService.deleteReimbursement(id);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<ReimbursementRequest> getReimbursementById(@PathVariable Long id) {
        try {
            ReimbursementRequest request = reimbursementService.getReimbursementById(id);
            if (request == null) {
                return ApiResponse.error(404, "报销申请不存在");
            }
            return ApiResponse.success(request);
        } catch (Exception e) {
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> getAllReimbursements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            // 如果没有指定userId，获取当前登录用户的ID
            if (userId == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();
                User currentUser = (User) userService.loadUserByUsername(username);
                userId = currentUser.getId();
            }

            Map<String, Object> result = reimbursementService.getReimbursementsWithFilter(
                    page - 1, size, userId, status, startDate, endDate);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<ReimbursementRequest>> getReimbursementsByUser(@PathVariable Long userId) {
        try {
            List<ReimbursementRequest> requests = (List<ReimbursementRequest>) reimbursementService
                    .getReimbursementsByUser(userId);
            return ApiResponse.success(requests);
        } catch (Exception e) {
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ApiResponse<List<ReimbursementRequest>> getAllReimbursementsSimple() {
        try {
            List<ReimbursementRequest> requests = (List<ReimbursementRequest>) reimbursementService
                    .getAllReimbursements();
            System.out.println("Controller - 查询到记录数: " + requests.size());
            for (ReimbursementRequest req : requests) {
                System.out.println("记录: ID=" + req.getId() + ", type=" + req.getType() + ", amount=" + req.getAmount()
                        + ", status=" + req.getStatus());
            }
            return ApiResponse.success(requests);
        } catch (Exception e) {
            System.err.println("查询失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/approve/{id}")
    public ApiResponse<ReimbursementRequest> approveReimbursement(@PathVariable Long id,
                                            @RequestBody Map<String, String> approvalData) {
        try {
            String status = approvalData.get("status");
            String comment = approvalData.get("comment");
            ReimbursementRequest approved = reimbursementService.approveReimbursement(id, status, comment);
            return ApiResponse.success("审批成功", approved);
        } catch (Exception e) {
            return ApiResponse.error("审批失败: " + e.getMessage());
        }
    }
}