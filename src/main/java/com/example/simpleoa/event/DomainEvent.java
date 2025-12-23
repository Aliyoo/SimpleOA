package com.example.simpleoa.event;

/**
 * 领域事件标记接口
 *
 * <p>所有领域事件都需要实现此接口
 * <p>用于标识领域事件，便于统一处理和监听
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
public interface DomainEvent {

    /**
     * 获取事件发生的时间
     *
     * @return 事件发生时间
     */
    java.time.LocalDateTime getOccurredOn();
}
