package com.example.simpleoa.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApprovalStage枚举测试
 *
 * @author SimpleOA Refactoring Team
 */
@DisplayName("ApprovalStage枚举测试")
class ApprovalStageTest {

    @Test
    @DisplayName("应该正确返回下一阶段")
    void shouldReturnNextStage() {
        assertEquals(ApprovalStage.MANAGER, ApprovalStage.INITIAL.nextStage());
        assertEquals(ApprovalStage.HR, ApprovalStage.MANAGER.nextStage());
        assertEquals(ApprovalStage.FINANCE, ApprovalStage.HR.nextStage());
        assertEquals(ApprovalStage.FINAL, ApprovalStage.FINANCE.nextStage());

        // 最后阶段返回null
        assertNull(ApprovalStage.FINAL.nextStage());
    }

    @Test
    @DisplayName("应该正确判断最终阶段")
    void shouldIdentifyFinalStage() {
        assertTrue(ApprovalStage.FINAL.isFinalStage());

        assertFalse(ApprovalStage.INITIAL.isFinalStage());
        assertFalse(ApprovalStage.MANAGER.isFinalStage());
        assertFalse(ApprovalStage.HR.isFinalStage());
        assertFalse(ApprovalStage.FINANCE.isFinalStage());
    }

    @Test
    @DisplayName("应该正确返回描述和级别")
    void shouldReturnCorrectDescriptionAndLevel() {
        assertEquals("初始审批", ApprovalStage.INITIAL.getDescription());
        assertEquals(Integer.valueOf(1), ApprovalStage.INITIAL.getLevel());

        assertEquals("部门经理审批", ApprovalStage.MANAGER.getDescription());
        assertEquals(Integer.valueOf(2), ApprovalStage.MANAGER.getLevel());

        assertEquals("HR审批", ApprovalStage.HR.getDescription());
        assertEquals(Integer.valueOf(3), ApprovalStage.HR.getLevel());

        assertEquals("财务审批", ApprovalStage.FINANCE.getDescription());
        assertEquals(Integer.valueOf(4), ApprovalStage.FINANCE.getLevel());

        assertEquals("最终审批", ApprovalStage.FINAL.getDescription());
        assertEquals(Integer.valueOf(99), ApprovalStage.FINAL.getLevel());
    }

    @Test
    @DisplayName("级别应该按顺序递增")
    void shouldHaveOrderedLevels() {
        assertTrue(ApprovalStage.INITIAL.getLevel() < ApprovalStage.MANAGER.getLevel());
        assertTrue(ApprovalStage.MANAGER.getLevel() < ApprovalStage.HR.getLevel());
        assertTrue(ApprovalStage.HR.getLevel() < ApprovalStage.FINANCE.getLevel());
        assertTrue(ApprovalStage.FINANCE.getLevel() < ApprovalStage.FINAL.getLevel());
    }
}
