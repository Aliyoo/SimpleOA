package com.example.simpleoa.controller;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.ReimbursementRequest;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.ApprovalFlowService;
import com.example.simpleoa.service.ReimbursementService;
import com.example.simpleoa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test/reimbursement")
public class TestReimbursementController {

    @Autowired
    private ReimbursementService reimbursementService;

    @Autowired
    private ApprovalFlowService approvalFlowService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 测试财务用户能否看到待审批的报销
     */
    @GetMapping("/finance-approvals/{userId}")
    public ResponseEntity<?> getFinanceApprovals(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }

            // 检查用户角色
            boolean isFinance = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_FINANCE".equals(role.getName()));
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("username", user.getUsername());
            result.put("isFinance", isFinance);
            result.put("isAdmin", isAdmin);

            // 获取该用户的所有审批流程
            List<ApprovalFlow> allApprovals = approvalFlowService.getApprovalFlowsByApprover(userId);
            List<ApprovalFlow> reimbursementApprovals = allApprovals.stream()
                    .filter(af -> "REIMBURSEMENT".equals(af.getRequestType()))
                    .collect(Collectors.toList());

            List<ApprovalFlow> pendingReimbursementApprovals = reimbursementApprovals.stream()
                    .filter(af -> "PENDING".equals(af.getStatus()))
                    .collect(Collectors.toList());

            result.put("totalApprovals", allApprovals.size());
            result.put("reimbursementApprovals", reimbursementApprovals.size());
            result.put("pendingReimbursementApprovals", pendingReimbursementApprovals.size());

            // 详细信息
            List<Map<String, Object>> approvalDetails = pendingReimbursementApprovals.stream()
                    .map(af -> {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("flowId", af.getId());
                        detail.put("status", af.getStatus());
                        detail.put("createTime", af.getCreateTime());
                        if (af.getReimbursementRequest() != null) {
                            detail.put("reimbursementId", af.getReimbursementRequest().getId());
                            detail.put("reimbursementTitle", af.getReimbursementRequest().getTitle());
                            detail.put("reimbursementStatus", af.getReimbursementRequest().getStatus());
                            detail.put("totalAmount", af.getReimbursementRequest().getTotalAmount());
                            if (af.getReimbursementRequest().getApplicant() != null) {
                                detail.put("applicantName", af.getReimbursementRequest().getApplicant().getRealName());
                            }
                        }
                        return detail;
                    })
                    .collect(Collectors.toList());

            result.put("approvalDetails", approvalDetails);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 检查特定报销的审批流程状态
     */
    @GetMapping("/check-flows/{reimbursementId}")
    public ResponseEntity<?> checkReimbursementFlows(@PathVariable Long reimbursementId) {
        try {
            ReimbursementRequest request = reimbursementService.getReimbursementById(reimbursementId);
            List<ApprovalFlow> flows = approvalFlowService.getApprovalFlowsByReimbursementRequest(reimbursementId);

            Map<String, Object> result = new HashMap<>();
            result.put("reimbursementId", reimbursementId);
            result.put("reimbursementStatus", request.getStatus());
            result.put("totalFlows", flows.size());

            List<Map<String, Object>> flowDetails = flows.stream()
                    .map(flow -> {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("flowId", flow.getId());
                        detail.put("status", flow.getStatus());
                        detail.put("createTime", flow.getCreateTime());
                        detail.put("updateTime", flow.getUpdateTime());
                        detail.put("approvalTime", flow.getApprovalTime());
                        detail.put("comment", flow.getComment());
                        
                        if (flow.getApprover() != null) {
                            detail.put("approverId", flow.getApprover().getId());
                            detail.put("approverName", flow.getApprover().getRealName());
                            List<String> roles = flow.getApprover().getRoles().stream()
                                    .map(role -> role.getName())
                                    .collect(Collectors.toList());
                            detail.put("approverRoles", roles);
                        }
                        
                        return detail;
                    })
                    .collect(Collectors.toList());

            result.put("flowDetails", flowDetails);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取所有财务用户信息
     */
    @GetMapping("/finance-users")
    public ResponseEntity<?> getFinanceUsers() {
        try {
            List<User> financeUsers = userRepository.findUsersByRole("ROLE_FINANCE");
            List<User> adminUsers = userRepository.findUsersByRole("ROLE_ADMIN");

            Map<String, Object> result = new HashMap<>();
            
            List<Map<String, Object>> financeDetails = financeUsers.stream()
                    .map(user -> {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("userId", user.getId());
                        detail.put("username", user.getUsername());
                        detail.put("realName", user.getRealName());
                        detail.put("email", user.getEmail());
                        detail.put("enabled", user.getEnabled());
                        List<String> roles = user.getRoles().stream()
                                .map(role -> role.getName())
                                .collect(Collectors.toList());
                        detail.put("roles", roles);
                        return detail;
                    })
                    .collect(Collectors.toList());

            List<Map<String, Object>> adminDetails = adminUsers.stream()
                    .map(user -> {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("userId", user.getId());
                        detail.put("username", user.getUsername());
                        detail.put("realName", user.getRealName());
                        detail.put("email", user.getEmail());
                        detail.put("enabled", user.getEnabled());
                        List<String> roles = user.getRoles().stream()
                                .map(role -> role.getName())
                                .collect(Collectors.toList());
                        detail.put("roles", roles);
                        return detail;
                    })
                    .collect(Collectors.toList());

            result.put("financeUsers", financeDetails);
            result.put("adminUsers", adminDetails);
            result.put("financeUserCount", financeUsers.size());
            result.put("adminUserCount", adminUsers.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
