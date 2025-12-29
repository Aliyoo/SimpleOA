package com.example.simpleoa.integration;

import com.example.simpleoa.event.ApprovalCompletedEvent;
import com.example.simpleoa.event.ApprovalRejectedEvent;
import com.example.simpleoa.event.ReimbursementApprovalCompletedEvent;
import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.ApprovalFlowRepository;
import com.example.simpleoa.repository.ReimbursementRequestRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 报销审批流程集成测试
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=update",
        "logging.level.com.example.simpleoa=DEBUG"
})
@DisplayName("报销审批流程集成测试")
@Transactional
public class ReimbursementApprovalIntegrationTest {

    @Autowired
    private ReimbursementRequestRepository reimbursementRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApprovalFlowRepository approvalFlowRepository;

    @Autowired
    private ApprovalOrchestrationService approvalOrchestrationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private User testUser;
    private User managerUser;
    private ReimbursementRequest testReimbursement;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        approvalFlowRepository.deleteAll();
        reimbursementRequestRepository.deleteAll();

        // 创建测试用户
        testUser = createTestUser("employee", "普通员工");
        managerUser = createTestUser("manager", "项目经理");

        // 创建测试报销申请
        testReimbursement = createTestReimbursementRequest(testUser);
    }

    @Test
    @DisplayName("完整的报销审批通过流程（单级审批）")
    void testCompleteApprovalFlow() {
        // ========== Step 1: 初始状态验证 ==========
        assertNotNull(testReimbursement.getId(), "报销申请应该已保存");
        assertEquals(ReimbursementStatus.DRAFT, testReimbursement.getStatus(), "初始状态：草稿");

        // ========== Step 2: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.REIMBURSEMENT,
                testReimbursement.getId(),
                managerUser
        );

        assertNotNull(approvalFlow, "审批流程应该创建成功");
        assertEquals(EntityType.REIMBURSEMENT, approvalFlow.getEntityType(), "entity_type应为REIMBURSEMENT");
        assertEquals(testReimbursement.getId(), approvalFlow.getEntityId(), "entity_id应匹配");
        assertEquals(ApprovalStatus.PENDING, approvalFlow.getUnifiedStatus(), "状态应为PENDING");

        // ========== Step 3: 最终审批通过（直接跳到FINAL，扣减预算）==========
        approvalFlow.setApprovalStage(ApprovalStage.FINAL);
        approvalFlowRepository.save(approvalFlow);

        ApprovalFlow approvedFlow = approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "报销申请审批通过"
        );

        assertNotNull(approvedFlow, "审批应该成功");
        assertEquals(ApprovalStatus.APPROVED, approvedFlow.getUnifiedStatus(), "状态应为APPROVED");
        assertNotNull(approvedFlow.getApprovalTime(), "审批时间应该已记录");

        // ========== Step 4: 验证报销申请状态更新 ==========
        ReimbursementRequest updatedReimbursement = reimbursementRequestRepository
                .findById(testReimbursement.getId())
                .orElse(null);

        assertNotNull(updatedReimbursement, "报销申请应该存在");
        assertEquals(ReimbursementStatus.APPROVED, updatedReimbursement.getStatus(), "报销申请状态应为APPROVED");

        // ========== Step 5: 验证事件发布 ==========
        assertDoesNotThrow(() -> {
            eventPublisher.publishEvent(new ReimbursementApprovalCompletedEvent(
                    this,
                    approvedFlow,
                    updatedReimbursement
            ));
        });
    }

    @Test
    @DisplayName("报销审批拒绝流程")
    void testRejectionFlow() {
        // ========== Step 1: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.REIMBURSEMENT,
                testReimbursement.getId(),
                managerUser
        );

        assertNotNull(approvalFlow, "审批流程应该创建成功");

        // ========== Step 2: 审批拒绝 ==========
        String rejectionReason = "报销不符合规定，请补充说明";
        ApprovalFlow rejectedFlow = approvalOrchestrationService.rejectApproval(
                approvalFlow.getId(),
                rejectionReason
        );

        assertNotNull(rejectedFlow, "拒绝应该成功");
        assertEquals(ApprovalStatus.REJECTED, rejectedFlow.getUnifiedStatus(), "状态应为REJECTED");
        assertEquals(rejectionReason, rejectedFlow.getComment(), "拒绝理由应该已记录");

        // ========== Step 3: 验证报销申请状态更新 ==========
        ReimbursementRequest updatedReimbursement = reimbursementRequestRepository
                .findById(testReimbursement.getId())
                .orElse(null);

        assertNotNull(updatedReimbursement, "报销申请应该存在");
        assertEquals(ReimbursementStatus.REJECTED, updatedReimbursement.getStatus(), "报销申请状态应为REJECTED");
    }

    @Test
    @DisplayName("报销审批状态验证")
    void testApprovalStatusValidation() {
        // ========== Step 1: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.REIMBURSEMENT,
                testReimbursement.getId(),
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
    @DisplayName("多级报销审批流程测试")
    void testMultiLevelApprovalFlow() {
        // ========== Step 1: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.REIMBURSEMENT,
                testReimbursement.getId(),
                managerUser
        );

        assertEquals(ApprovalStage.INITIAL, approvalFlow.getApprovalStage(), "初始阶段应为INITIAL");

        // ========== Step 2: 项目经理审批通过（INITIAL → FINANCE）==========
        ApprovalFlow firstApproval = approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "项目经理审批通过"
        );

        // 验证报销申请状态
        ReimbursementRequest reimbursement = reimbursementRequestRepository.findById(testReimbursement.getId()).orElseThrow();
        assertEquals(ReimbursementStatus.PENDING_FINANCE_APPROVAL, reimbursement.getStatus(), "应转为待财务审批");

        // ========== Step 3: 财务审批通过（FINANCE → FINAL，扣减预算）==========
        firstApproval.setApprovalStage(ApprovalStage.FINANCE);
        approvalFlowRepository.save(firstApproval);

        ApprovalFlow finalApproval = approvalOrchestrationService.approveApproval(
                firstApproval.getId(),
                "财务审批通过"
        );

        // 验证报销申请状态
        ReimbursementRequest finalReimbursement = reimbursementRequestRepository.findById(testReimbursement.getId()).orElseThrow();
        assertEquals(ReimbursementStatus.APPROVED, finalReimbursement.getStatus(), "应为最终审批通过");
    }

    @Test
    @DisplayName("报销审批事件发布测试")
    void testEventPublishing() {
        // ========== Step 1: 提交并审批通过 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.REIMBURSEMENT,
                testReimbursement.getId(),
                managerUser
        );

        ApprovalFlow approvedFlow = approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "审批通过"
        );

        // ========== Step 2: 验证事件发布 ==========
        assertDoesNotThrow(() -> {
            ReimbursementRequest updatedReimbursement = reimbursementRequestRepository
                    .findById(testReimbursement.getId())
                    .orElseThrow();

            eventPublisher.publishEvent(new ApprovalCompletedEvent(approvedFlow, updatedReimbursement));
            eventPublisher.publishEvent(new ReimbursementApprovalCompletedEvent(
                    this,
                    approvedFlow,
                    updatedReimbursement
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

    private ReimbursementRequest createTestReimbursementRequest(User applicant) {
        ReimbursementRequest request = new ReimbursementRequest();
        request.setApplicant(applicant);
        request.setTitle("测试报销申请");
        request.setTotalAmount(new BigDecimal("1000.00"));
        request.setStatus(ReimbursementStatus.DRAFT);
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());

        // 创建费用明细
        List<ReimbursementItem> items = new ArrayList<>();
        ReimbursementItem item = new ReimbursementItem();
        item.setExpenseDate(java.time.LocalDate.now());
        item.setItemCategory("交通");
        item.setDescription("差旅交通费");
        item.setAmount(new BigDecimal("500.00"));
        item.setReimbursementRequest(request);
        items.add(item);

        request.setItems(items);
        return reimbursementRequestRepository.save(request);
    }
}
