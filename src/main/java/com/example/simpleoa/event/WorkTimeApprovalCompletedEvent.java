package com.example.simpleoa.event;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.WorkTimeRecord;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 工时审批完成事件
 *
 * <p>当工时审批通过时发布此事件
 * <p>监听器可以：
 * - 发送通知给用户
 * - 更新项目统计
 * - 触发其他业务逻辑
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
public class WorkTimeApprovalCompletedEvent extends ApplicationEvent implements DomainEvent {

    private static final long serialVersionUID = 1L;

    private final ApprovalFlow approvalFlow;
    private final WorkTimeRecord workTimeRecord;
    private final LocalDateTime occurredOn;

    /**
     * 创建工时审批完成事件
     *
     * @param source          事件发布者
     * @param approvalFlow    审批流程
     * @param workTimeRecord  工时记录
     */
    public WorkTimeApprovalCompletedEvent(Object source, ApprovalFlow approvalFlow, WorkTimeRecord workTimeRecord) {
        super(source);
        this.approvalFlow = approvalFlow;
        this.workTimeRecord = workTimeRecord;
        this.occurredOn = LocalDateTime.now();
    }

    public ApprovalFlow getApprovalFlow() {
        return approvalFlow;
    }

    public WorkTimeRecord getWorkTimeRecord() {
        return workTimeRecord;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String toString() {
        return "WorkTimeApprovalCompletedEvent{" +
                "workTimeId=" + workTimeRecord.getId() +
                ", userId=" + (workTimeRecord.getUser() != null ? workTimeRecord.getUser().getId() : null) +
                ", projectId=" + (workTimeRecord.getProject() != null ? workTimeRecord.getProject().getId() : null) +
                ", hours=" + workTimeRecord.getHours() +
                '}';
    }
}
