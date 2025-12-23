package com.example.simpleoa.controller;

import com.example.simpleoa.dto.leave.AnnualLeaveCalculation;
import com.example.simpleoa.dto.leave.InitializationResult;
import com.example.simpleoa.model.AnnualLeaveRule;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.AnnualLeaveRuleService;
import com.example.simpleoa.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 年假规则管理控制器
 * 提供年假规则的CRUD操作和年假计算功能
 */
@RestController
@RequestMapping("/api/leave/annual-rules")
@CrossOrigin(origins = "*")
public class AnnualLeaveRuleController {

    private static final Logger logger = LoggerFactory.getLogger(AnnualLeaveRuleController.class);

    @Autowired
    private AnnualLeaveRuleService annualLeaveRuleService;

    @Autowired
    private UserService userService;

    /**
     * 获取所有年假规则
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<List<AnnualLeaveRule>> getAllRules() {
        try {
            List<AnnualLeaveRule> rules = annualLeaveRuleService.getAllRules();
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            logger.error("获取年假规则失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取所有启用的年假规则
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<AnnualLeaveRule>> getActiveRules() {
        try {
            List<AnnualLeaveRule> rules = annualLeaveRuleService.getActiveRules();
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            logger.error("获取启用的年假规则失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据ID获取年假规则
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<AnnualLeaveRule> getRuleById(@PathVariable Long id) {
        try {
            Optional<AnnualLeaveRule> ruleOpt = annualLeaveRuleService.getRuleById(id);
            return ruleOpt.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("获取年假规则失败，ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 创建年假规则
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<AnnualLeaveRule> createRule(@Valid @RequestBody AnnualLeaveRule rule,
                                                    Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            AnnualLeaveRule createdRule = annualLeaveRuleService.createRule(rule, currentUser.getId());
            return ResponseEntity.ok(createdRule);
        } catch (IllegalArgumentException e) {
            logger.warn("创建年假规则失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("创建年假规则失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 更新年假规则
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<AnnualLeaveRule> updateRule(@PathVariable Long id,
                                                    @Valid @RequestBody AnnualLeaveRule rule,
                                                    Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            rule.setId(id);
            AnnualLeaveRule updatedRule = annualLeaveRuleService.updateRule(rule, currentUser.getId());
            return ResponseEntity.ok(updatedRule);
        } catch (IllegalArgumentException e) {
            logger.warn("更新年假规则失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("更新年假规则失败，ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除年假规则
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        try {
            boolean deleted = annualLeaveRuleService.deleteRule(id);
            return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("删除年假规则失败，ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 启用/禁用年假规则
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<AnnualLeaveRule> toggleRuleStatus(@PathVariable Long id,
                                                         @RequestParam boolean enabled,
                                                         Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            AnnualLeaveRule updatedRule = annualLeaveRuleService.toggleRuleStatus(id, enabled, currentUser.getId());
            return ResponseEntity.ok(updatedRule);
        } catch (Exception e) {
            logger.error("切换年假规则状态失败，ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 计算用户指定年份的年假
     */
    @GetMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<AnnualLeaveCalculation> calculateAnnualLeave(
            @RequestParam Long userId,
            @RequestParam Integer year,
            Authentication authentication) {
        try {
            // 检查权限：员工只能查看自己的年假
            User currentUser = getCurrentUser(authentication);
            if (currentUser != null && !hasAdminRole(currentUser) &&
                !userId.equals(currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            AnnualLeaveCalculation calculation = annualLeaveRuleService.calculateAnnualLeave(userId, year);
            return ResponseEntity.ok(calculation);
        } catch (Exception e) {
            logger.error("计算用户年假失败，用户ID: {}, 年份: {}", userId, year, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 计算用户当前年份的年假
     */
    @GetMapping("/calculate/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<AnnualLeaveCalculation> calculateCurrentAnnualLeave(
            @RequestParam(required = false) Long userId,
            Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            Long targetUserId = (userId != null && hasAdminRole(currentUser)) ? userId : currentUser.getId();

            if (targetUserId == null) {
                return ResponseEntity.badRequest().build();
            }

            AnnualLeaveCalculation calculation = annualLeaveRuleService.calculateCurrentAnnualLeave(targetUserId);
            return ResponseEntity.ok(calculation);
        } catch (Exception e) {
            logger.error("计算当前年假失败，用户ID: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量计算多个用户的年假
     */
    @PostMapping("/calculate/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<List<AnnualLeaveCalculation>> batchCalculateAnnualLeave(
            @RequestBody BatchCalculateRequest request) {
        try {
            List<AnnualLeaveCalculation> calculations = annualLeaveRuleService
                .batchCalculateAnnualLeave(request.getUserIds(), request.getYear());
            return ResponseEntity.ok(calculations);
        } catch (Exception e) {
            logger.error("批量计算年假失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据工龄获取年假天数
     */
    @GetMapping("/days")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<Integer> getAnnualLeaveDays(@RequestParam Double seniorityYears) {
        try {
            Integer days = annualLeaveRuleService.getAnnualLeaveDays(java.math.BigDecimal.valueOf(seniorityYears));
            return ResponseEntity.ok(days);
        } catch (Exception e) {
            logger.error("获取年假天数失败，工龄: {}", seniorityYears, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取年假规则统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<AnnualLeaveRuleService.RuleStatistics> getRuleStatistics() {
        try {
            AnnualLeaveRuleService.RuleStatistics statistics = annualLeaveRuleService.getRuleStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("获取年假规则统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 验证年假规则
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<AnnualLeaveRuleService.ValidationResult> validateRule(@RequestBody AnnualLeaveRule rule) {
        try {
            AnnualLeaveRuleService.ValidationResult validation = annualLeaveRuleService.validateRule(rule);
            return ResponseEntity.ok(validation);
        } catch (Exception e) {
            logger.error("验证年假规则失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 重新计算指定年份所有用户的年假余额
     */
    @PostMapping("/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<InitializationResult> recalculateAnnualLeaves(
            @RequestParam Integer year,
            Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().build();
            }

            // 这里需要调用假期余额初始化服务
            // 暂时返回成功响应
            return ResponseEntity.ok(InitializationResult.builder()
                .year(year)
                .totalUsers(0)
                .successCount(0)
                .failureCount(0)
                .errors(List.of("功能待实现"))
                .build());
        } catch (Exception e) {
            logger.error("重新计算年假失败，年份: {}", year, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量计算请求DTO
     */
    public static class BatchCalculateRequest {
        private List<Long> userIds;
        private Integer year;

        public List<Long> getUserIds() { return userIds; }
        public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
    }

    // 私有辅助方法

    /**
     * 获取当前用户
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        return userService.getUserByUsername(username);
    }

    /**
     * 检查用户是否有管理员角色
     */
    private boolean hasAdminRole(User user) {
        return user.getRoles() != null && user.getRoles().stream()
            .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || "HR_MANAGER".equals(role.getName()));
    }
}