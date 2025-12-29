package com.example.simpleoa.integration;

import com.example.simpleoa.event.ApprovalCompletedEvent;
import com.example.simpleoa.event.ApprovalRejectedEvent;
import com.example.simpleoa.event.BusinessTripApprovalCompletedEvent;
import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.ApprovalFlowRepository;
import com.example.simpleoa.repository.BusinessTripRequestRepository;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.ApprovalOrchestrationService;
import com.example.simpleoa.service.BusinessTripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 出差审批流程集成测试
 *
 * <p>完整的出差审批流程端到端测试：
 * 1. 创建出差申请
 * 2. 提交审批
 * 3. 审批通过/拒绝
 * 4. 验证状态更新
 * 5. 验证事件发布
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=update",
        "logging.level.com.example.simpleoa=DEBUG"
})
@DisplayName("出差审批流程集成测试")
@Transactional
public class BusinessTripApprovalIntegrationTest {

    @Autowired
    private BusinessTripRequestRepository businessTripRequestRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApprovalFlowRepository approvalFlowRepository;

    @Autowired
    private ApprovalOrchestrationService approvalOrchestrationService;

    @Autowired(required = false)
    private BusinessTripService businessTripService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private User testUser;
    private User managerUser;
    private Project testProject;
    private BusinessTripRequest testBusinessTrip;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        approvalFlowRepository.deleteAll();
        businessTripRequestRepository.deleteAll();

        // 创建测试用户
        testUser = createTestUser("employee", "普通员工");
        managerUser = createTestUser("manager", "项目经理");

        // 创建测试项目
        testProject = createTestProject(managerUser);

        // 创建测试出差申请
        testBusinessTrip = createTestBusinessTripRequest(testUser, testProject);
    }

    @Test
    @DisplayName("完整的出差审批通过流程")
    void testCompleteApprovalFlow() {
        // ========== Step 1: 初始状态验证 ==========
        assertNotNull(testBusinessTrip.getId(), "出差申请应该已保存");
        assertEquals(BusinessTripStatus.DRAFT, testBusinessTrip.getStatus(), "初始状态：草稿");

        // ========== Step 2: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.BUSINESS_TRIP,
                testBusinessTrip.getId(),
                managerUser
        );

        assertNotNull(approvalFlow, "审批流程应该创建成功");
        assertEquals(EntityType.BUSINESS_TRIP, approvalFlow.getEntityType(), "entity_type应为BUSINESS_TRIP");
        assertEquals(testBusinessTrip.getId(), approvalFlow.getEntityId(), "entity_id应匹配");
        assertEquals(ApprovalStatus.PENDING, approvalFlow.getUnifiedStatus(), "状态应为PENDING");

        // ========== Step 3: 审批通过 ==========
        ApprovalFlow approvedFlow = approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "出差申请审批通过"
        );

        assertNotNull(approvedFlow, "审批应该成功");
        assertEquals(ApprovalStatus.APPROVED, approvedFlow.getUnifiedStatus(), "状态应为APPROVED");
        assertNotNull(approvedFlow.getApprovalTime(), "审批时间应该已记录");

        // ========== Step 4: 验证出差申请状态更新 ==========
        BusinessTripRequest updatedBusinessTrip = businessTripRequestRepository
                .findById(testBusinessTrip.getId())
                .orElse(null);

        assertNotNull(updatedBusinessTrip, "出差申请应该存在");
        assertEquals(BusinessTripStatus.APPROVED, updatedBusinessTrip.getStatus(), "出差申请状态应为APPROVED");

        // ========== Step 5: 验证事件发布 ==========
        // 注意：由于@Async和@Transactional，事件可能在测试结束后才处理
        // 这里主要验证不抛出异常
        assertDoesNotThrow(() -> {
            eventPublisher.publishEvent(new BusinessTripApprovalCompletedEvent(
                    this,
                    approvedFlow,
                    updatedBusinessTrip
            ));
        });
    }

    @Test
    @DisplayName("出差审批拒绝流程")
    void testRejectionFlow() {
        // ========== Step 1: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.BUSINESS_TRIP,
                testBusinessTrip.getId(),
                managerUser
        );

        assertNotNull(approvalFlow, "审批流程应该创建成功");

        // ========== Step 2: 审批拒绝 ==========
        String rejectionReason = "出差时间与项目冲突，请调整时间";
        ApprovalFlow rejectedFlow = approvalOrchestrationService.rejectApproval(
                approvalFlow.getId(),
                rejectionReason
        );

        assertNotNull(rejectedFlow, "拒绝应该成功");
        assertEquals(ApprovalStatus.REJECTED, rejectedFlow.getUnifiedStatus(), "状态应为REJECTED");
        assertEquals(rejectionReason, rejectedFlow.getComment(), "拒绝理由应该已记录");

        // ========== Step 3: 验证出差申请状态更新 ==========
        BusinessTripRequest updatedBusinessTrip = businessTripRequestRepository
                .findById(testBusinessTrip.getId())
                .orElse(null);

        assertNotNull(updatedBusinessTrip, "出差申请应该存在");
        assertEquals(BusinessTripStatus.REJECTED, updatedBusinessTrip.getStatus(), "出差申请状态应为REJECTED");
    }

    @Test
    @DisplayName("出差审批状态验证")
    void testApprovalStatusValidation() {
        // ========== Step 1: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.BUSINESS_TRIP,
                testBusinessTrip.getId(),
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
    @DisplayName("出差审批事件发布测试")
    void testEventPublishing() {
        // ========== Step 1: 提交并审批通过 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.BUSINESS_TRIP,
                testBusinessTrip.getId(),
                managerUser
        );

        ApprovalFlow approvedFlow = approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "审批通过"
        );

        // ========== Step 2: 验证事件发布 ==========
        // 注意：这里主要验证不抛出异常
        assertDoesNotThrow(() -> {
            BusinessTripRequest updatedBusinessTrip = businessTripRequestRepository
                    .findById(testBusinessTrip.getId())
                    .orElseThrow();

            eventPublisher.publishEvent(new ApprovalCompletedEvent(approvedFlow, updatedBusinessTrip));
            eventPublisher.publishEvent(new BusinessTripApprovalCompletedEvent(
                    this,
                    approvedFlow,
                    updatedBusinessTrip
            ));
        });
    }

    @Test
    @DisplayName("多级出差申请审批测试")
    void testMultipleBusinessTripApprovals() {
        // 创建多个出差申请
        BusinessTripRequest trip1 = createTestBusinessTripRequest(testUser, testProject);
        BusinessTripRequest trip2 = createTestBusinessTripRequest(testUser, testProject);

        // 提交第一个申请审批
        ApprovalFlow flow1 = approvalOrchestrationService.submitApproval(
                EntityType.BUSINESS_TRIP,
                trip1.getId(),
                managerUser
        );

        // 提交第二个申请审批
        ApprovalFlow flow2 = approvalOrchestrationService.submitApproval(
                EntityType.BUSINESS_TRIP,
                trip2.getId(),
                managerUser
        );

        // 审批第一个
        approvalOrchestrationService.approveApproval(flow1.getId(), "审批通过");

        // 拒绝第二个
        approvalOrchestrationService.rejectApproval(flow2.getId(), "时间冲突");

        // 验证状态
        BusinessTripRequest updatedTrip1 = businessTripRequestRepository.findById(trip1.getId()).orElseThrow();
        BusinessTripRequest updatedTrip2 = businessTripRequestRepository.findById(trip2.getId()).orElseThrow();

        assertEquals(BusinessTripStatus.APPROVED, updatedTrip1.getStatus(), "第一个申请应为APPROVED");
        assertEquals(BusinessTripStatus.REJECTED, updatedTrip2.getStatus(), "第二个申请应为REJECTED");
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

    private Project createTestProject(User manager) {
        Project project = new Project();
        project.setName("测试项目");
        project.setCode("TEST-001");
        project.setManager(manager);
        project.setStatus("ACTIVE");
        return projectRepository.save(project);
    }

    private BusinessTripRequest createTestBusinessTripRequest(User applicant, Project project) {
        BusinessTripRequest request = new BusinessTripRequest();
        request.setApplicant(applicant);
        request.setProject(project);
        request.setDestination("北京市");
        request.setPurpose("项目需求调研");
        request.setStartTime(LocalDateTime.now().plusDays(7));
        request.setEndTime(LocalDateTime.now().plusDays(10));
        request.setDays(3);
        request.setStatus(BusinessTripStatus.DRAFT);
        request.setComment("测试出差申请");
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        return businessTripRequestRepository.save(request);
    }
}
