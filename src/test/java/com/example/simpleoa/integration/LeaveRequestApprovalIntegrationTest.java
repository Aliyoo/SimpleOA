package com.example.simpleoa.integration;

import com.example.simpleoa.event.ApprovalCompletedEvent;
import com.example.simpleoa.event.ApprovalRejectedEvent;
import com.example.simpleoa.event.LeaveRequestApprovalCompletedEvent;
import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.ApprovalFlowRepository;
import com.example.simpleoa.repository.LeaveBalanceRepository;
import com.example.simpleoa.repository.LeaveRequestRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.ApprovalOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 请假审批流程集成测试
 *
 * <p>完整的请假审批流程端到端测试：
 * 1. 创建请假申请
 * 2. 提交审批
 * 3. 部门经理审批
 * 4. HR审批（最终审批，扣减余额）
 * 5. 验证状态更新和余额变化
 * 6. 验证事件发布
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=update",
        "logging.level.com.example.simpleoa=DEBUG"
})
@DisplayName("请假审批流程集成测试")
@Transactional
public class LeaveRequestApprovalIntegrationTest {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApprovalFlowRepository approvalFlowRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private ApprovalOrchestrationService approvalOrchestrationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private User testUser;
    private User managerUser;
    private LeaveRequest testLeaveRequest;
    private LeaveBalance testLeaveBalance;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        approvalFlowRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveBalanceRepository.deleteAll();

        // 创建测试用户
        testUser = createTestUser("employee", "普通员工");
        managerUser = createTestUser("manager", "项目经理");

        // 创建测试年假余额（15天）
        testLeaveBalance = createTestLeaveBalance(testUser, LeaveType.ANNUAL_LEAVE, 2025, 15);

        // 创建测试请假申请（年假，3天）
        testLeaveRequest = createTestLeaveRequest(testUser, LeaveType.ANNUAL_LEAVE, 3.0);
    }

    @Test
    @DisplayName("完整的请假审批通过流程（单级审批）")
    void testCompleteApprovalFlow() {
        // ========== Step 1: 初始状态验证 ==========
        assertNotNull(testLeaveRequest.getId(), "请假申请应该已保存");
        assertEquals(LeaveStatus.DRAFT, testLeaveRequest.getStatus(), "初始状态：草稿");

        // 验证初始余额
        LeaveBalance initialBalance = leaveBalanceRepository
                .findByUserIdAndYearAndLeaveType(testUser.getId(), 2025, LeaveType.ANNUAL_LEAVE)
                .orElseThrow();
        assertEquals(15, initialBalance.getRemainingDays(), "初始剩余天数应为15天");
        assertEquals(0, initialBalance.getUsedDays(), "初始已使用天数应为0天");

        // ========== Step 2: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.LEAVE_REQUEST,
                testLeaveRequest.getId(),
                managerUser
        );

        assertNotNull(approvalFlow, "审批流程应该创建成功");
        assertEquals(EntityType.LEAVE_REQUEST, approvalFlow.getEntityType(), "entity_type应为LEAVE_REQUEST");
        assertEquals(testLeaveRequest.getId(), approvalFlow.getEntityId(), "entity_id应匹配");
        assertEquals(ApprovalStatus.PENDING, approvalFlow.getUnifiedStatus(), "状态应为PENDING");
        assertEquals(ApprovalStage.INITIAL, approvalFlow.getApprovalStage(), "阶段应为INITIAL");

        // ========== Step 3: 最终审批通过（直接跳到FINAL，扣减余额）==========
        approvalFlow.setApprovalStage(ApprovalStage.FINAL);
        approvalFlowRepository.save(approvalFlow);

        ApprovalFlow approvedFlow = approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "请假申请审批通过"
        );

        assertNotNull(approvedFlow, "审批应该成功");
        assertEquals(ApprovalStatus.APPROVED, approvedFlow.getUnifiedStatus(), "状态应为APPROVED");
        assertNotNull(approvedFlow.getApprovalTime(), "审批时间应该已记录");

        // ========== Step 4: 验证请假申请状态更新 ==========
        LeaveRequest updatedLeaveRequest = leaveRequestRepository
                .findById(testLeaveRequest.getId())
                .orElse(null);

        assertNotNull(updatedLeaveRequest, "请假申请应该存在");
        assertEquals(LeaveStatus.APPROVED, updatedLeaveRequest.getStatus(), "请假申请状态应为APPROVED");
        assertNotNull(updatedLeaveRequest.getApproveTime(), "审批时间应该已记录");

        // ========== Step 5: 验证请假余额扣减 ==========
        LeaveBalance updatedBalance = leaveBalanceRepository
                .findByUserIdAndYearAndLeaveType(testUser.getId(), 2025, LeaveType.ANNUAL_LEAVE)
                .orElseThrow();

        assertEquals(3, updatedBalance.getUsedDays(), "已使用天数应为3天");
        assertEquals(12, updatedBalance.getRemainingDays(), "剩余天数应为12天");

        // ========== Step 6: 验证事件发布 ==========
        assertDoesNotThrow(() -> {
            eventPublisher.publishEvent(new LeaveRequestApprovalCompletedEvent(
                    this,
                    approvedFlow,
                    updatedLeaveRequest
            ));
        });
    }

    @Test
    @DisplayName("请假审批拒绝流程")
    void testRejectionFlow() {
        // ========== Step 1: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.LEAVE_REQUEST,
                testLeaveRequest.getId(),
                managerUser
        );

        assertNotNull(approvalFlow, "审批流程应该创建成功");

        // ========== Step 2: 审批拒绝 ==========
        String rejectionReason = "请假时间与项目冲突，请调整时间";
        ApprovalFlow rejectedFlow = approvalOrchestrationService.rejectApproval(
                approvalFlow.getId(),
                rejectionReason
        );

        assertNotNull(rejectedFlow, "拒绝应该成功");
        assertEquals(ApprovalStatus.REJECTED, rejectedFlow.getUnifiedStatus(), "状态应为REJECTED");
        assertEquals(rejectionReason, rejectedFlow.getComment(), "拒绝理由应该已记录");

        // ========== Step 3: 验证请假申请状态更新 ==========
        LeaveRequest updatedLeaveRequest = leaveRequestRepository
                .findById(testLeaveRequest.getId())
                .orElse(null);

        assertNotNull(updatedLeaveRequest, "请假申请应该存在");
        assertEquals(LeaveStatus.REJECTED, updatedLeaveRequest.getStatus(), "请假申请状态应为REJECTED");

        // ========== Step 4: 验证余额未扣减 ==========
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndYearAndLeaveType(testUser.getId(), 2025, LeaveType.ANNUAL_LEAVE)
                .orElseThrow();

        assertEquals(0, balance.getUsedDays(), "已使用天数应为0天（未扣减）");
        assertEquals(15, balance.getRemainingDays(), "剩余天数仍应为15天");
    }

    @Test
    @DisplayName("请假审批状态验证")
    void testApprovalStatusValidation() {
        // ========== Step 1: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.LEAVE_REQUEST,
                testLeaveRequest.getId(),
                managerUser
        );

        // ========== Step 2: 第一次审批通过 ==========
        ApprovalFlow firstApproval = approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "审批通过"
        );

        assertEquals(ApprovalStatus.APPROVED, firstApproval.getUnifiedStatus());

        // ========== Step 3: 尝试重复审批（应该抛出异常）==========
        assertThrows(IllegalStateException.class, () -> {
            approvalOrchestrationService.approveApproval(
                    approvalFlow.getId(),
                    "重复审批"
            );
        }, "已审批的流程不能重复审批");
    }

    @Test
    @DisplayName("多级请假审批流程测试")
    void testMultiLevelApprovalFlow() {
        // ========== Step 1: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.LEAVE_REQUEST,
                testLeaveRequest.getId(),
                managerUser
        );

        assertEquals(ApprovalStage.INITIAL, approvalFlow.getApprovalStage(), "初始阶段应为INITIAL");

        // ========== Step 2: 部门经理审批通过（INITIAL → HR_REVIEW）==========
        ApprovalFlow firstApproval = approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "部门经理审批通过"
        );

        // 验证请假申请状态
        LeaveRequest leaveRequest = leaveRequestRepository.findById(testLeaveRequest.getId()).orElseThrow();
        assertEquals(LeaveStatus.PENDING_HR_APPROVAL, leaveRequest.getStatus(), "应转为待人事审批");

        // ========== Step 3: 人事审批通过（HR_REVIEW → FINAL，扣减余额）==========
        firstApproval.setApprovalStage(ApprovalStage.HR_REVIEW);
        approvalFlowRepository.save(firstApproval);

        ApprovalFlow finalApproval = approvalOrchestrationService.approveApproval(
                firstApproval.getId(),
                "人事审批通过"
        );

        // 验证请假申请状态
        LeaveRequest finalLeaveRequest = leaveRequestRepository.findById(testLeaveRequest.getId()).orElseThrow();
        assertEquals(LeaveStatus.APPROVED, finalLeaveRequest.getStatus(), "应为最终审批通过");

        // 验证余额扣减
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndYearAndLeaveType(testUser.getId(), 2025, LeaveType.ANNUAL_LEAVE)
                .orElseThrow();
        assertEquals(3, balance.getUsedDays(), "应扣减3天");
        assertEquals(12, balance.getRemainingDays(), "剩余12天");
    }

    @Test
    @DisplayName("非配额限制类型请假测试（不扣减余额）")
    void testNonQuotaLimitedLeaveType() {
        // 创建婚假申请（无配额限制）
        LeaveRequest marriageLeave = createTestLeaveRequest(testUser, LeaveType.MARRIAGE_LEAVE, 3.0);

        // ========== Step 1: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.LEAVE_REQUEST,
                marriageLeave.getId(),
                managerUser
        );

        // ========== Step 2: 最终审批通过 ==========
        approvalFlow.setApprovalStage(ApprovalStage.FINAL);
        approvalFlowRepository.save(approvalFlow);

        approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "婚假审批通过"
        );

        // ========== Step 3: 验证状态更新 ==========
        LeaveRequest updatedLeave = leaveRequestRepository.findById(marriageLeave.getId()).orElseThrow();
        assertEquals(LeaveStatus.APPROVED, updatedLeave.getStatus(), "应为审批通过");

        // ========== Step 4: 验证余额未创建（婚假不扣减余额）==========
        boolean balanceExists = leaveBalanceRepository
                .findByUserIdAndYearAndLeaveType(testUser.getId(), 2025, LeaveType.MARRIAGE_LEAVE)
                .isPresent();

        assertFalse(balanceExists, "婚假不应有余额记录");
    }

    @Test
    @DisplayName("请假审批事件发布测试")
    void testEventPublishing() {
        // ========== Step 1: 提交并审批通过 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.LEAVE_REQUEST,
                testLeaveRequest.getId(),
                managerUser
        );

        ApprovalFlow approvedFlow = approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "审批通过"
        );

        // ========== Step 2: 验证事件发布 ==========
        assertDoesNotThrow(() -> {
            LeaveRequest updatedLeaveRequest = leaveRequestRepository
                    .findById(testLeaveRequest.getId())
                    .orElseThrow();

            eventPublisher.publishEvent(new ApprovalCompletedEvent(approvedFlow, updatedLeaveRequest));
            eventPublisher.publishEvent(new LeaveRequestApprovalCompletedEvent(
                    this,
                    approvedFlow,
                    updatedLeaveRequest
            ));
        });
    }

    // ========== 辅助方法 ==========

    private User createTestUser(String username, String roleName) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setRealName(username + "姓名");
        user.setEmail(username + "@example.com");
        user.setRole(roleName);
        return userRepository.save(user);
    }

    private LeaveBalance createTestLeaveBalance(User user, LeaveType leaveType, int year, int totalDays) {
        LeaveBalance balance = new LeaveBalance();
        balance.setUserId(user.getId());
        balance.setLeaveType(leaveType);
        balance.setYear(year);
        balance.setTotalDays(totalDays);
        balance.setUsedDays(0);
        balance.calculateRemainingDays();
        return leaveBalanceRepository.save(balance);
    }

    private LeaveRequest createTestLeaveRequest(User applicant, LeaveType leaveType, double days) {
        LeaveRequest request = new LeaveRequest();
        request.setApplicant(applicant);
        request.setLeaveType(leaveType);
        request.setReason("测试请假申请");
        request.setStartDate(LocalDateTime.now().plusDays(7));
        request.setEndDate(LocalDateTime.now().plusDays(7 + (int) days));
        request.setLeaveDays(days);
        request.setStatus(LeaveStatus.DRAFT);
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        return leaveRequestRepository.save(request);
    }
}
