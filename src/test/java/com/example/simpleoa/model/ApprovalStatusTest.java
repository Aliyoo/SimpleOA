package com.example.simpleoa.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApprovalStatus枚举测试
 *
 * @author SimpleOA Refactoring Team
 */
@DisplayName("ApprovalStatus枚举测试")
class ApprovalStatusTest {

    @Test
    @DisplayName("应该正确判断终态")
    void shouldIdentifyFinalStates() {
        assertTrue(ApprovalStatus.APPROVED.isFinalState());
        assertTrue(ApprovalStatus.REJECTED.isFinalState());
        assertTrue(ApprovalStatus.CANCELLED.isFinalState());

        assertFalse(ApprovalStatus.PENDING.isFinalState());
        assertFalse(ApprovalStatus.REVOKED.isFinalState());
    }

    @Test
    @DisplayName("应该正确判断状态转换")
    void shouldValidateStateTransitions() {
        // PENDING可以转换为任何状态
        assertTrue(ApprovalStatus.PENDING.canTransitionTo(ApprovalStatus.APPROVED));
        assertTrue(ApprovalStatus.PENDING.canTransitionTo(ApprovalStatus.REJECTED));
        assertTrue(ApprovalStatus.PENDING.canTransitionTo(ApprovalStatus.CANCELLED));

        // 终态不能转换为任何状态
        assertFalse(ApprovalStatus.APPROVED.canTransitionTo(ApprovalStatus.PENDING));
        assertFalse(ApprovalStatus.APPROVED.canTransitionTo(ApprovalStatus.REJECTED));

        // REVOKED可以转换为终态
        assertTrue(ApprovalStatus.REVOKED.canTransitionTo(ApprovalStatus.APPROVED));
        assertTrue(ApprovalStatus.REVOKED.canTransitionTo(ApprovalStatus.REJECTED));
    }

    @Test
    @DisplayName("应该正确返回描述信息")
    void shouldReturnCorrectDescriptions() {
        assertEquals("待审批", ApprovalStatus.PENDING.getDescription());
        assertEquals("已批准", ApprovalStatus.APPROVED.getDescription());
        assertEquals("已拒绝", ApprovalStatus.REJECTED.getDescription());
        assertEquals("已撤销", ApprovalStatus.CANCELLED.getDescription());
        assertEquals("已撤回", ApprovalStatus.REVOKED.getDescription());
    }
}
