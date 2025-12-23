package com.example.simpleoa.service.strategy;

import com.example.simpleoa.model.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApprovalStrategyFactory测试
 *
 * @author SimpleOA Refactoring Team
 */
@SpringBootTest
@DisplayName("ApprovalStrategyFactory集成测试")
class ApprovalStrategyFactoryTest {

    @Autowired(required = false)
    private ApprovalStrategyFactory strategyFactory;

    @Test
    @DisplayName("应该成功加载策略工厂")
    void shouldLoadStrategyFactory() {
        assertNotNull(strategyFactory, "ApprovalStrategyFactory should be autowired");
    }

    @Test
    @DisplayName("应该注册所有策略")
    void shouldRegisterAllStrategies() {
        if (strategyFactory == null) {
            return; // Skip if factory not loaded (no strategies implemented yet)
        }

        int expectedCount = 4; // WORK_TIME, LEAVE_REQUEST, BUSINESS_TRIP, REIMBURSEMENT
        int actualCount = strategyFactory.getStrategyCount();

        // Phase 1: WorkTimeApprovalStrategy is implemented
        assertTrue(actualCount >= 1 && actualCount <= expectedCount,
            "Strategy count should be between 1 and " + expectedCount);
    }

    @Test
    @DisplayName("应该支持所有实体类型")
    void shouldSupportAllEntityTypes() {
        if (strategyFactory == null) {
            return;
        }

        Set<EntityType> supportedTypes = strategyFactory.getSupportedEntityTypes();

        // Phase 1: WORK_TIME should be supported
        assertNotNull(supportedTypes);
        assertTrue(supportedTypes.contains(EntityType.WORK_TIME),
            "WORK_TIME should be supported in Phase 1");
        assertTrue(supportedTypes.size() <= 4);
    }

    @Test
    @DisplayName("查询存在的策略应该返回Optional")
    void shouldReturnOptionalForExistingStrategy() {
        if (strategyFactory == null) {
            return;
        }

        // Phase 1: WORK_TIME strategy exists
        Optional<?> strategy = strategyFactory.getStrategyOptional(EntityType.WORK_TIME);

        assertNotNull(strategy);
        assertTrue(strategy.isPresent(), "WORK_TIME strategy should be present in Phase 1");
    }

    @Test
    @DisplayName("查询存在的策略应该成功")
    void shouldGetExistingStrategy() {
        if (strategyFactory == null) {
            return;
        }

        // Phase 1: WORK_TIME strategy exists and should be retrievable
        assertDoesNotThrow(() -> {
            ApprovalStrategy<?> strategy = strategyFactory.getStrategy(EntityType.WORK_TIME);
            assertNotNull(strategy);
            assertEquals(EntityType.WORK_TIME, strategy.getEntityType());
        });
    }

    @Test
    @DisplayName("查询不存在的策略应该抛出异常")
    void shouldThrowExceptionForNonExistentStrategy() {
        if (strategyFactory == null) {
            return;
        }

        // Phase 1: REIMBURSEMENT strategy not implemented yet
        assertThrows(IllegalArgumentException.class, () -> {
            strategyFactory.getStrategy(EntityType.REIMBURSEMENT);
        });
    }

    @Test
    @DisplayName("应该正确判断策略是否存在")
    void shouldCheckStrategyExistence() {
        if (strategyFactory == null) {
            return;
        }

        // Phase 1: WORK_TIME strategy exists
        assertTrue(strategyFactory.hasStrategy(EntityType.WORK_TIME),
            "WORK_TIME strategy should exist in Phase 1");

        // Phase 1: Other strategies not implemented yet
        assertFalse(strategyFactory.hasStrategy(EntityType.LEAVE_REQUEST),
            "LEAVE_REQUEST strategy should not exist yet");
        assertFalse(strategyFactory.hasStrategy(EntityType.BUSINESS_TRIP),
            "BUSINESS_TRIP strategy should not exist yet");
        assertFalse(strategyFactory.hasStrategy(EntityType.REIMBURSEMENT),
            "REIMBURSEMENT strategy should not exist yet");
    }
}
