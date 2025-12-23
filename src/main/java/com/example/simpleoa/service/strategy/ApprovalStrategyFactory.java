package com.example.simpleoa.service.strategy;

import com.example.simpleoa.model.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 审批策略工厂
 *
 * <p>负责管理所有审批策略的注册和查询
 * <p>自动发现并注册所有ApprovalStrategy实现类
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Component
public class ApprovalStrategyFactory {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalStrategyFactory.class);

    /**
     * 策略注册表：EntityType -> ApprovalStrategy
     */
    private final Map<EntityType, ApprovalStrategy<?>> strategies = new HashMap<>();

    /**
     * 构造函数，自动注入所有ApprovalStrategy实现
     *
     * @param strategyList Spring自动收集的所有ApprovalStrategy实现类
     */
    @Autowired
    public ApprovalStrategyFactory(List<ApprovalStrategy<?>> strategyList) {
        logger.info("Initializing ApprovalStrategyFactory...");

        for (ApprovalStrategy<?> strategy : strategyList) {
            registerStrategy(strategy);
        }

        logger.info("ApprovalStrategyFactory initialized with {} strategies", strategies.size());
        logRegisteredStrategies();
    }

    /**
     * 注册一个审批策略
     *
     * @param strategy 待注册的策略
     */
    private void registerStrategy(ApprovalStrategy<?> strategy) {
        EntityType entityType = strategy.getEntityType();

        if (strategies.containsKey(entityType)) {
            logger.warn("Duplicate strategy registration for entityType: {}, existing: {}, new: {}",
                entityType, strategies.get(entityType).getClass().getSimpleName(), strategy.getClass().getSimpleName());
            return;
        }

        strategies.put(entityType, strategy);
        logger.debug("Registered strategy: {} for entityType: {}",
            strategy.getClass().getSimpleName(), entityType);
    }

    /**
     * 根据实体类型获取对应的审批策略
     *
     * @param entityType 实体类型
     * @return 对应的审批策略
     * @throws IllegalArgumentException 如果找不到对应的策略
     */
    @SuppressWarnings("unchecked")
    public <T> ApprovalStrategy<T> getStrategy(EntityType entityType) {
        ApprovalStrategy<?> strategy = strategies.get(entityType);

        if (strategy == null) {
            throw new IllegalArgumentException(
                "No approval strategy found for entity type: " + entityType
            );
        }

        return (ApprovalStrategy<T>) strategy;
    }

    /**
     * 根据实体类型获取对应的审批策略（Optional版本）
     *
     * @param entityType 实体类型
     * @return Optional包装的策略
     */
    public <T> Optional<ApprovalStrategy<T>> getStrategyOptional(EntityType entityType) {
        @SuppressWarnings("unchecked")
        ApprovalStrategy<T> strategy = (ApprovalStrategy<T>) strategies.get(entityType);
        return Optional.ofNullable(strategy);
    }

    /**
     * 判断是否存在指定类型的策略
     *
     * @param entityType 实体类型
     * @return true如果存在对应策略
     */
    public boolean hasStrategy(EntityType entityType) {
        return strategies.containsKey(entityType);
    }

    /**
     * 获取所有注册的实体类型
     *
     * @return 实体类型集合
     */
    public java.util.Set<EntityType> getSupportedEntityTypes() {
        return strategies.keySet();
    }

    /**
     * 获取注册的策略数量
     *
     * @return 策略数量
     */
    public int getStrategyCount() {
        return strategies.size();
    }

    /**
     * 记录所有已注册的策略（用于调试）
     */
    private void logRegisteredStrategies() {
        if (logger.isDebugEnabled()) {
            logger.debug("Registered approval strategies:");
            strategies.forEach((type, strategy) -> {
                logger.debug("  - {}: {}", type, strategy.getClass().getSimpleName());
            });
        }
    }
}
