package com.example.simpleoa.controller;

import com.example.simpleoa.common.ApiResponse;
import com.example.simpleoa.constants.ExpenseCategory;
import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.ReimbursementRequestDTO;
import com.example.simpleoa.model.ReimbursementStatus;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.ReimbursementService;
import com.example.simpleoa.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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

    @PostMapping
    public ResponseEntity<ApiResponse<ReimbursementRequest>> createReimbursement(@RequestBody ReimbursementRequestDTO dto, @AuthenticationPrincipal User user) {
        if (!reimbursementService.validateReimbursementBudget(dto)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("预算不足，无法创建报销申请"));
        }
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

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<ReimbursementRequest>> submitForApproval(
            @PathVariable Long id,
            @AuthenticationPrincipal User submitter) {
        ReimbursementRequest result = reimbursementService.submitForApproval(id, submitter.getId());
        return ResponseEntity.ok(ApiResponse.success("报销申请已提交审批", result));
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

    @GetMapping("/{id}/check-budget")
    public ResponseEntity<ApiResponse<Boolean>> checkBudgetAvailability(@PathVariable Long id) {
        boolean available = reimbursementService.checkBudgetAvailability(id);
        return ResponseEntity.ok(ApiResponse.success("预算检查完成", available));
    }

    @PostMapping("/validate-budget")
    public ResponseEntity<ApiResponse<Boolean>> validateReimbursementBudget(@RequestBody ReimbursementRequestDTO dto) {
        boolean valid = reimbursementService.validateReimbursementBudget(dto);
        return ResponseEntity.ok(ApiResponse.success("预算验证完成", valid));
    }

    @GetMapping("/expense-categories")
    public ResponseEntity<ApiResponse<List<String>>> getExpenseCategories() {
        List<String> categories = ExpenseCategory.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("获取费用类别成功", categories));
    }

    /**
     * 财务审查报销申请
     */
    @PostMapping("/{id}/finance-review")
    public ResponseEntity<ApiResponse<ReimbursementRequest>> financeReview(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal User financeUser) {

        String decision = payload.get("decision");
        String comment = payload.get("comment");

        try {
            ReimbursementRequest result = reimbursementService.financeReview(id, decision, comment, financeUser.getId());
            return ResponseEntity.ok(ApiResponse.success("财务审查完成", result));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 查询待财务审查的报销申请列表（支持状态过滤）
     * @param financeUser 财务用户
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选（可选，支持数组）：all=全部, PENDING_FINANCE_REVIEW=待审查, PENDING_LEADER_APPROVAL=审批中, APPROVED=已通过, REJECTED=已拒绝
     */
    @GetMapping("/finance-review-pending")
    public ResponseEntity<ApiResponse<Page<ReimbursementRequest>>> getPendingFinanceReview(
            @AuthenticationPrincipal User financeUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String[] status) {

        // 详细日志：诊断角色加载问题
        System.out.println("=== 财务审查接口调用 ===");
        System.out.println("用户: " + financeUser.getUsername());
        System.out.println("用户ID: " + financeUser.getId());
        System.out.println("Roles对象: " + financeUser.getRoles());
        System.out.println("Roles是否为空: " + (financeUser.getRoles() == null));
        
        // 关键修复：如果角色为空，从数据库重新加载完整用户信息
        User fullUser = financeUser;
        if (financeUser.getRoles() == null || financeUser.getRoles().isEmpty()) {
            System.out.println("⚠️  角色信息为空，从数据库重新加载用户");
            try {
                fullUser = userService.getUserById(financeUser.getId());
                System.out.println("重新加载后的Roles: " + fullUser.getRoles());
            } catch (Exception e) {
                System.out.println("❌ 重新加载用户失败: " + e.getMessage());
                return ResponseEntity.status(403).body(ApiResponse.error("用户信息加载失败"));
            }
        }
        
        if (fullUser.getRoles() != null) {
            System.out.println("Roles数量: " + fullUser.getRoles().size());
            fullUser.getRoles().forEach(role -> 
                System.out.println("  - 角色: " + role.getName() + " (ID: " + role.getId() + ")")
            );
        }

        // 验证财务人员权限
        boolean isFinance = fullUser.getRoles() != null && fullUser.getRoles().stream()
                .anyMatch(role -> "ROLE_FINANCE".equals(role.getName()) || "ROLE_ADMIN".equals(role.getName()));

        System.out.println("是否有财务权限: " + isFinance);

        if (!isFinance) {
            System.out.println("❌ 权限验证失败，返回403");
            return ResponseEntity.status(403).body(ApiResponse.error("无权访问"));
        }

        // 解析状态参数（支持数组）
        List<ReimbursementStatus> statuses = null;
        if (status != null && status.length > 0) {
            statuses = new ArrayList<>();
            for (String s : status) {
                if (s != null && !s.isEmpty() && !"all".equalsIgnoreCase(s)) {
                    try {
                        statuses.add(ReimbursementStatus.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(ApiResponse.error("无效的状态参数: " + s));
                    }
                }
            }
        }

        // 如果未指定状态或指定为all，查询所有需要财务关注的状态
        Page<ReimbursementRequest> requests;
        if (statuses == null || statuses.isEmpty()) {
            // 默认查询待财务审查 + 审批中 + 已完成的状态
            requests = reimbursementService.getForFinanceReview(page, size);
        } else if (statuses.size() == 1) {
            // 查询单个状态
            requests = reimbursementService.getByStatus(statuses.get(0), page, size);
        } else {
            // 查询多个状态
            requests = reimbursementService.getByStatuses(statuses, page, size);
        }

        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * 获取财务审查统计数据
     */
    @GetMapping("/finance-statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFinanceStatistics(
            @AuthenticationPrincipal User financeUser) {

        // 如果角色为空，从数据库重新加载
        User fullUser = financeUser;
        if (financeUser.getRoles() == null || financeUser.getRoles().isEmpty()) {
            fullUser = userService.getUserById(financeUser.getId());
        }

        // 验证财务人员权限
        boolean isFinance = fullUser.getRoles() != null && fullUser.getRoles().stream()
                .anyMatch(role -> "ROLE_FINANCE".equals(role.getName()) || "ROLE_ADMIN".equals(role.getName()));

        if (!isFinance) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权访问"));
        }

        Map<String, Object> statistics = reimbursementService.getFinanceStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
