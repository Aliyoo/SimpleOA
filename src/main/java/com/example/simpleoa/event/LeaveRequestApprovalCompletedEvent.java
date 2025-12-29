package com.example.simpleoa.event;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.LeaveRequest;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 请假审批完成事件
 *
 * <p>当请假审批最终通过时发布此事件
 * <p>监听器可以：
 * - 发送通知给申请人
 * - 更新项目统计
 * - 触发其他业务逻辑（如工作交接、系统权限调整等）
 * - 记录审计日志
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
public class LeaveRequestApprovalCompletedEvent extends ApplicationEvent implements DomainEvent {

    private static final long serialVersionUID = 1L;

    private final ApprovalFlow approvalFlow;
    private final LeaveRequest leaveRequest;
    private final LocalDateTime occurredOn;

    /**
     * 创建请假审批完成事件
     *
     * @param source        事件发布者
     * @param approvalFlow  审批流程
     * @param leaveRequest  请假申请
     */
    public LeaveRequestApprovalCompletedEvent(Object source, ApprovalFlow approvalFlow, LeaveRequest leaveRequest) {
        super(source);
        this.approvalFlow = approvalFlow;
        this.leaveRequest = leaveRequest;
        this.occurredOn = LocalDateTime.now();
    }

    public ApprovalFlow getApprovalFlow() {
        return approvalFlow;
    }

    public LeaveRequest getLeaveRequest() {
        return leaveRequest;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String toString() {
        return "LeaveRequestApprovalCompletedEvent{" +
                "leaveRequestId=" + leaveRequest.getId() +
                ", applicantId=" + (leaveRequest.getApplicant() != null ? leaveRequest.getApplicant().getId() : null) +
                ", leaveType=" + leaveRequest.getLeaveType() +
                ", leaveDays=" + leaveRequest.getLeaveDays() +
                ", startDate=" + leaveRequest.getStartDate() +
                ", endDate=" + leaveRequest.getEndDate() +
                ", reason=" + leaveRequest.getReason() +
                ", status=" + leaveRequest.getStatus() +
                '}';
    }
}
