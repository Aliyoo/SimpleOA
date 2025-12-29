package com.example.simpleoa.service.strategy;

import com.example.simpleoa.model.ApprovalFlow;
import com.example.simpleoa.model.ApprovalStage;
import com.example.simpleoa.model.ApprovalStatus;
import com.example.simpleoa.model.EntityType;
import com.example.simpleoa.model.LeaveBalance;
import com.example.simpleoa.model.LeaveRequest;
import com.example.simpleoa.model.LeaveStatus;
import com.example.simpleoa.model.LeaveType;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.LeaveBalanceRepository;
import com.example.simpleoa.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 请假审批策略
 *
 * <p>请假审批是多级审批（部门经理 → 人事 → 最终审批）
 * <p>审批特点：
 * - INITIAL阶段：部门经理审批
 * - HR_REVIEW阶段：人事审批
 * - FINAL阶段：最终审批（扣减请假余额）
 * <p>余额扣除逻辑：
 * - 只有在最终审批通过时才扣减余额
 * - 只对配额限制类型（年假、病假、事假）扣减
 * - 婚假、产假等特殊假期不扣减余额
 *
 * @author SimpleOA Refactoring Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveRequestApprovalStrategy implements ApprovalStrategy<LeaveRequest> {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    @Override
    public boolean supports(EntityType entityType) {
        return EntityType.LEAVE_REQUEST.equals(entityType);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.LEAVE_REQUEST;
    }

    @Override
    public void validateForSubmission(LeaveRequest entity) {
        if (entity == null) {
            throw new IllegalArgumentException("请假申请不能为空");
        }

        if (entity.getId() == null) {
            throw new IllegalArgumentException("请假申请必须先保存才能提交审批");
        }

        if (entity.getApplicant() == null || entity.getApplicant().getId() == null) {
            throw new IllegalArgumentException("请假申请必须关联申请人");
        }

        if (entity.getLeaveType() == null) {
            throw new IllegalArgumentException("请假类型不能为空");
        }

        if (entity.getStartDate() == null || entity.getEndDate() == null) {
            throw new IllegalArgumentException("请假时间不能为空");
        }

        if (entity.getStartDate().isAfter(entity.getEndDate())) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }

        if (entity.getReason() == null || entity.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("请假事由不能为空");
        }

        if (entity.getLeaveDays() == null || entity.getLeaveDays() <= 0) {
            throw new IllegalArgumentException("请假天数必须大于0");
        }

        // 检查请假申请是否存在于数据库中
        if (!leaveRequestRepository.existsById(entity.getId())) {
            throw new IllegalArgumentException("请假申请不存在于数据库中");
        }

        // 检查状态是否允许提交审批
        if (entity.getStatus() != LeaveStatus.DRAFT) {
            throw new IllegalArgumentException("只有草稿状态的请假申请才能提交审批");
        }
    }

    @Override
    public void onApproved(LeaveRequest entity, ApprovalFlow flow) {
        log.info("请假审批通过: leaveRequestId={}, applicantId={}, leaveType={}, currentStage={}",
                entity.getId(),
                entity.getApplicant() != null ? entity.getApplicant().getId() : null,
                entity.getLeaveType(),
                flow.getApprovalStage());

        ApprovalStage currentStage = flow.getApprovalStage();

        // 根据审批阶段更新状态
        if (currentStage == ApprovalStage.INITIAL) {
            // 部门经理审批通过 → 转到人事审批
            entity.setStatus(LeaveStatus.PENDING_HR_APPROVAL);
            log.info("部门经理审批通过，转人事审批: leaveRequestId={}", entity.getId());

        } else if (currentStage == ApprovalStage.HR) {
            // 人事审批通过 → 转最终审批
            entity.setStatus(LeaveStatus.APPROVED);
            entity.setApproveTime(java.time.LocalDateTime.now());

            // ⭐ 最终审批：扣减请假余额
            if (isQuotaLimitedType(entity.getLeaveType())) {
                deductLeaveBalance(entity);
            }

            log.info("人事审批通过，最终审批完成: leaveRequestId={}", entity.getId());

        } else if (currentStage == ApprovalStage.FINAL) {
            // 最终审批通过（如果是直接单级审批）
            entity.setStatus(LeaveStatus.APPROVED);
            entity.setApproveTime(java.time.LocalDateTime.now());

            // ⭐ 最终审批：扣减请假余额
            if (isQuotaLimitedType(entity.getLeaveType())) {
                deductLeaveBalance(entity);
            }

            log.info("最终审批通过: leaveRequestId={}", entity.getId());
        }

        // 保存请假申请
        leaveRequestRepository.save(entity);

        log.info("请假申请审批状态已更新: leaveRequestId={}, status={}", entity.getId(), entity.getStatus());
    }

    @Override
    public void onRejected(LeaveRequest entity, ApprovalFlow flow, String comment) {
        log.info("请假审批拒绝: leaveRequestId={}, applicantId={}, leaveType={}, comment={}",
                entity.getId(),
                entity.getApplicant() != null ? entity.getApplicant().getId() : null,
                entity.getLeaveType(),
                comment);

        // 审批拒绝 → 状态变为REJECTED
        entity.setStatus(LeaveStatus.REJECTED);

        // 保存审批意见
        if (entity.getComment() == null || entity.getComment().trim().isEmpty()) {
            entity.setComment(comment);
        }

        // 保存请假申请
        leaveRequestRepository.save(entity);

        log.info("请假申请审批状态已更新: leaveRequestId={}, status=REJECTED", entity.getId());
    }

    @Override
    public User getNextApprover(LeaveRequest entity, ApprovalStage currentStage) {
        // 请假审批有多级，需要根据当前阶段返回下一级审批人

        if (currentStage == null || currentStage == ApprovalStage.INITIAL) {
            // 第一级：部门经理审批
            User applicant = entity.getApplicant();
            // TODO: 实现查找部门经理的逻辑

            // 备选：返回HR角色用户
            log.info("查找部门经理作为第一级审批人: leaveRequestId={}", entity.getId());

        } else if (currentStage == ApprovalStage.HR) {
            // 第二级：HR审批（最终审批）
            log.info("查找HR作为第二级审批人: leaveRequestId={}", entity.getId());
        }

        log.warn("无法确定下一级审批人: leaveRequestId={}, currentStage={}", entity.getId(), currentStage);
        return null;
    }

    @Override
    public boolean isFinalApproval(ApprovalStage currentStage) {
        // 请假审批的最终阶段是HR或FINAL
        return currentStage == ApprovalStage.HR || currentStage == ApprovalStage.FINAL;
    }

    @Override
    public ApprovalFlow createNextStageApproval(LeaveRequest entity, ApprovalStage currentStage, User nextApprover) {
        // 创建下一级审批流程（如果支持多级审批）
        if (currentStage == ApprovalStage.INITIAL && nextApprover != null) {
            ApprovalFlow nextFlow = new ApprovalFlow();
            nextFlow.setEntityType(EntityType.LEAVE_REQUEST);
            nextFlow.setEntityId(entity.getId());
            nextFlow.setApprover(nextApprover);
            nextFlow.setUnifiedStatus(ApprovalStatus.PENDING);
            nextFlow.setApprovalStage(ApprovalStage.HR);
            nextFlow.setCreateTime(new java.util.Date());
            return nextFlow;
        }

        // 其他情况不创建下一级审批
        return null;
    }

    /**
     * 判断是否是配额限制类型
     *
     * @param leaveType 请假类型
     * @return true表示需要配额限制
     */
    private boolean isQuotaLimitedType(LeaveType leaveType) {
        return leaveType == LeaveType.ANNUAL_LEAVE ||
               leaveType == LeaveType.SICK_LEAVE ||
               leaveType == LeaveType.PERSONAL_LEAVE;
    }

    /**
     * 扣减请假余额
     *
     * <p>只有配额限制类型（年假、病假、事假）才扣减余额
     * <p>向上取整扣减（避免0.5天问题）
     *
     * @param entity 请假申请
     */
    private void deductLeaveBalance(LeaveRequest entity) {
        int year = entity.getStartDate().getYear();
        Long userId = entity.getApplicant().getId();
        LeaveType leaveType = entity.getLeaveType();
        double leaveDays = entity.getLeaveDays();

        log.info("开始扣减请假余额: userId={}, leaveType={}, year={}, leaveDays={}",
                userId, leaveType, year, leaveDays);

        // 查找请假余额记录
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndYearAndLeaveType(userId, year, leaveType)
                .orElseThrow(() -> new RuntimeException(
                        "未找到请假余额记录: userId=" + userId + ", year=" + year + ", leaveType=" + leaveType));

        // ⭐ 向上取整累加已使用天数
        int currentUsed = balance.getUsedDays();
        int newUsed = (int) Math.ceil(currentUsed + leaveDays);
        balance.setUsedDays(newUsed);

        // 重新计算剩余天数
        balance.calculateRemainingDays();

        // 保存余额
        LeaveBalance savedBalance = leaveBalanceRepository.save(balance);

        log.info("请假余额扣减成功: userId={}, leaveType={}, year={}, " +
                "原已使用={}天, 本次扣减={}天, 新已使用={}天, 剩余={}天",
                userId, leaveType, year,
                currentUsed, leaveDays, newUsed, savedBalance.getRemainingDays());

        // 检查余额是否充足（应该不会出现余额不足，因为申请时已检查）
        if (savedBalance.getRemainingDays() < 0) {
            log.error("警告：请假余额不足！userId={}, leaveType={}, remainingDays={}",
                    userId, leaveType, savedBalance.getRemainingDays());
        }
    }
}
