package com.example.simpleoa.integration;

import com.example.simpleoa.event.ApprovalCompletedEvent;
import com.example.simpleoa.event.ApprovalRejectedEvent;
import com.example.simpleoa.event.WorkTimeApprovalCompletedEvent;
import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.ApprovalFlowRepository;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.repository.WorkTimeRecordRepository;
import com.example.simpleoa.service.ApprovalOrchestrationService;
import com.example.simpleoa.service.WorkTimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工时审批流程集成测试
 *
 * <p>完整的工时审批流程端到端测试：
 * 1. 创建工时记录
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
@DisplayName("工时审批流程集成测试")
@Transactional
public class WorkTimeApprovalIntegrationTest {

    @Autowired
    private WorkTimeRecordRepository workTimeRecordRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApprovalFlowRepository approvalFlowRepository;

    @Autowired
    private ApprovalOrchestrationService approvalOrchestrationService;

    @Autowired(required = false)
    private WorkTimeService workTimeService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private User testUser;
    private User managerUser;
    private Project testProject;
    private WorkTimeRecord testWorkTime;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        approvalFlowRepository.deleteAll();
        workTimeRecordRepository.deleteAll();

        // 创建测试用户
        testUser = createTestUser("employee", "普通员工");
        managerUser = createTestUser("manager", "项目经理");

        // 创建测试项目
        testProject = createTestProject(managerUser);

        // 创建测试工时记录
        testWorkTime = createTestWorkTimeRecord(testUser, testProject);
    }

    @Test
    @DisplayName("完整的工时审批通过流程")
    void testCompleteApprovalFlow() {
        // ========== Step 1: 初始状态验证 ==========
        assertNotNull(testWorkTime.getId(), "工时记录应该已保存");
        assertFalse(testWorkTime.isApproved(), "初始状态：未审批");

        // ========== Step 2: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.WORK_TIME,
                testWorkTime.getId(),
                managerUser
        );

        assertNotNull(approvalFlow, "审批流程应该创建成功");
        assertNotNull(approvalFlow.getId(), "审批流程应该有ID");
        assertEquals(EntityType.WORK_TIME, approvalFlow.getEntityType(), "实体类型应为WORK_TIME");
        assertEquals(testWorkTime.getId(), approvalFlow.getEntityId(), "实体ID应匹配");
        assertEquals(managerUser, approvalFlow.getApprover(), "审批人应为项目经理");
        assertEquals(ApprovalStatus.PENDING, approvalFlow.getUnifiedStatus(), "状态应为PENDING");
        assertEquals(ApprovalStage.INITIAL, approvalFlow.getApprovalStage(), "阶段应为INITIAL");

        // ========== Step 3: 审批通过 ==========
        ApprovalFlow approvedFlow = approvalOrchestrationService.approveApproval(
                approvalFlow.getId(),
                "审批通过"
        );

        assertNotNull(approvedFlow, "审批流程应该更新成功");
        assertEquals(ApprovalStatus.APPROVED, approvedFlow.getUnifiedStatus(), "状态应为APPROVED");
        assertNotNull(approvedFlow.getApprovalTime(), "应该有审批时间");

        // ========== Step 4: 验证工时记录状态更新 ==========
        WorkTimeRecord updatedWorkTime = workTimeRecordRepository.findById(testWorkTime.getId())
                .orElseThrow();

        assertTrue(updatedWorkTime.isApproved(), "工时记录应该已审批通过");
        assertEquals("APPROVED", updatedWorkTime.getStatus(), "状态应为APPROVED");

        // ========== Step 5: 验证历史审批流程 ==========
        List<ApprovalFlow> history = approvalFlowRepository.findByEntityTypeAndEntityId(
                EntityType.WORK_TIME,
                testWorkTime.getId()
        );

        assertFalse(history.isEmpty(), "应该有审批历史");
        assertTrue(history.stream().anyMatch(af ->
                af.getId().equals(approvalFlow.getId()) &&
                        ApprovalStatus.APPROVED.equals(af.getUnifiedStatus())
        ), "审批历史应包含本次审批");
    }

    @Test
    @DisplayName("完整的工时审批拒绝流程")
    void testRejectionFlow() {
        // ========== Step 1: 提交审批 ==========
        ApprovalFlow approvalFlow = approvalOrchestrationService.submitApproval(
                EntityType.WORK_TIME,
                testWorkTime.getId(),
                managerUser
        );

        // ========== Step 2: 审批拒绝 ==========
        String rejectionReason = "工时填报不合理";
        ApprovalFlow rejectedFlow = approvalOrchestrationService.rejectApproval(
                approvalFlow.getId(),
                rejectionReason
        );

        assertNotNull(rejectedFlow, "审批流程应该更新成功");
        assertEquals(ApprovalStatus.REJECTED, rejectedFlow.getUnifiedStatus(), "状态应为REJECTED");
        assertEquals(rejectionReason, rejectedFlow.getComment(), "应该有拒绝理由");

        // ========== Step 3: 验证工时记录状态 ==========
        WorkTimeRecord updatedWorkTime = workTimeRecordRepository.findById(testWorkTime.getId())
                .orElseThrow();

        assertFalse(updatedWorkTime.isApproved(), "工时记录应该未通过审批");
        assertEquals("REJECTED", updatedWorkTime.getStatus(), "状态应为REJECTED");
    }

    @Test
    @DisplayName("测试事件发布机制")
    void testEventPublishing() {
        // ========== 测试审批通过事件 ==========
        ApprovalFlow flow1 = approvalOrchestrationService.submitApproval(
                EntityType.WORK_TIME,
                testWorkTime.getId(),
                managerUser
        );

        approvalOrchestrationService.approveApproval(flow1.getId(), "通过");

        // 验证审批流程状态
        ApprovalFlow loaded1 = approvalFlowRepository.findById(flow1.getId()).get();
        assertEquals(ApprovalStatus.APPROVED, loaded1.getUnifiedStatus());

        // ========== 测试审批拒绝事件 ==========
        WorkTimeRecord workTime2 = createTestWorkTimeRecord(testUser, testProject);
        ApprovalFlow flow2 = approvalOrchestrationService.submitApproval(
                EntityType.WORK_TIME,
                workTime2.getId(),
                managerUser
        );

        approvalOrchestrationService.rejectApproval(flow2.getId(), "拒绝");

        // 验证审批流程状态
        ApprovalFlow loaded2 = approvalFlowRepository.findById(flow2.getId()).get();
        assertEquals(ApprovalStatus.REJECTED, loaded2.getUnifiedStatus());
        assertEquals("拒绝", loaded2.getComment());

        // 注意：实际的事件监听器验证需要异步等待或mock
        // 这里只验证事件发布机制是否就绪
    }

    @Test
    @DisplayName("测试重复审批防护")
    void testDuplicateApprovalProtection() {
        // ========== Step 1: 提交并审批通过 ==========
        ApprovalFlow flow = approvalOrchestrationService.submitApproval(
                EntityType.WORK_TIME,
                testWorkTime.getId(),
                managerUser
        );

        approvalOrchestrationService.approveApproval(flow.getId(), "首次审批");

        // ========== Step 2: 尝试重复审批应该失败 ==========
        assertThrows(IllegalStateException.class, () -> {
            approvalOrchestrationService.approveApproval(flow.getId(), "重复审批");
        }, "重复审批应该抛出异常");
    }

    @Test
    @DisplayName("测试新旧结构兼容性")
    void testLegacyStructureCompatibility() {
        // 创建使用旧结构的审批流程
        ApprovalFlow legacyFlow = new ApprovalFlow();
        legacyFlow.setWorkTimeRecord(testWorkTime);
        legacyFlow.setApprover(managerUser);
        legacyFlow.setStatus("PENDING");
        legacyFlow.setRequestType("WORKTIME");
        legacyFlow.setCreateTime(new java.util.Date());

        ApprovalFlow saved = approvalFlowRepository.save(legacyFlow);

        assertNotNull(saved, "旧结构审批流程应该保存成功");
        assertTrue(saved.isLegacy(), "应该被识别为遗留数据");
        assertNull(saved.getEntityType(), "entityType应该为null");
        assertNull(saved.getEntityId(), "entityId应该为null");

        // 验证可以读取遗留数据
        ApprovalFlow loaded = approvalFlowRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(loaded, "应该能加载遗留审批流程");
        assertTrue(loaded.isLegacy(), "加载后仍应识别为遗留数据");
    }

    @Test
    @DisplayName("测试策略工厂集成")
    void testStrategyFactoryIntegration() {
        // 创建多个工时记录
        WorkTimeRecord workTime1 = createTestWorkTimeRecord(testUser, testProject);
        WorkTimeRecord workTime2 = createTestWorkTimeRecord(testUser, testProject);

        // 提交多个审批
        ApprovalFlow flow1 = approvalOrchestrationService.submitApproval(
                EntityType.WORK_TIME,
                workTime1.getId(),
                managerUser
        );

        ApprovalFlow flow2 = approvalOrchestrationService.submitApproval(
                EntityType.WORK_TIME,
                workTime2.getId(),
                managerUser
        );

        assertNotNull(flow1, "第一个审批应该创建成功");
        assertNotNull(flow2, "第二个审批应该创建成功");

        // 验证都使用 WORK_TIME 策略
        assertEquals(EntityType.WORK_TIME, flow1.getEntityType());
        assertEquals(EntityType.WORK_TIME, flow2.getEntityType());

        // 批量审批
        approvalOrchestrationService.approveApproval(flow1.getId(), "批量审批1");
        approvalOrchestrationService.approveApproval(flow2.getId(), "批量审批2");

        // 验证都成功更新
        ApprovalFlow loaded1 = approvalFlowRepository.findById(flow1.getId()).get();
        ApprovalFlow loaded2 = approvalFlowRepository.findById(flow2.getId()).get();

        assertEquals(ApprovalStatus.APPROVED, loaded1.getUnifiedStatus());
        assertEquals(ApprovalStatus.APPROVED, loaded2.getUnifiedStatus());
    }

    // ========== Helper Methods ==========

    private User createTestUser(String username, String realName) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setRealName(realName);
        user.setEmail(username + "@test.com");
        return userRepository.save(user);
    }

    private Project createTestProject(User manager) {
        Project project = new Project();
        project.setName("测试项目");
        project.setDescription("集成测试项目");
        project.setManager(manager);
        project.setStatus(ProjectStatus.IN_PROGRESS); // ProjectStatus is required
        project.setStartDate(new java.util.Date());
        project.setEndDate(new java.util.Date(System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000));
        return projectRepository.save(project);
    }

    private WorkTimeRecord createTestWorkTimeRecord(User user, Project project) {
        WorkTimeRecord record = new WorkTimeRecord();
        record.setUser(user);
        record.setProject(project);
        record.setDate(LocalDate.now());
        record.setHours(8.0);
        record.setWorkType("NORMAL"); // workType is required
        record.setDescription("测试工时");
        record.setApproved(false);
        record.setStatus("DRAFT");
        return workTimeRecordRepository.save(record);
    }
}
