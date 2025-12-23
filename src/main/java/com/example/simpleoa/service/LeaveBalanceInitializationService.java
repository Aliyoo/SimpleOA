package com.example.simpleoa.service;

import com.example.simpleoa.dto.leave.InitializationResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 假期余额初始化服务接口
 * 提供自动化的假期余额初始化和管理功能
 */
public interface LeaveBalanceInitializationService {

    /**
     * 年初批量初始化所有员工的假期余额
     * @param year 目标年份
     * @return 初始化结果统计
     */
    CompletableFuture<InitializationResult> batchInitializeYearlyBalances(Integer year);

    /**
     * 初始化单个用户的年度假期余额
     * @param userId 用户ID
     * @param year 目标年份
     * @return 是否初始化成功
     */
    boolean initializeUserYearlyBalance(Long userId, Integer year);

    /**
     * 新员工入职时初始化假期余额
     * @param userId 用户ID
     * @param hireDate 入职日期
     * @return 是否初始化成功
     */
    boolean initializeNewEmployeeBalance(Long userId, LocalDate hireDate);

    /**
     * 执行假期结转
     * @param fromYear 来源年份
     * @param toYear 目标年份
     * @return 结转结果统计
     */
    CompletableFuture<CarryoverResult> executeCarryover(Integer fromYear, Integer toYear);

    /**
     * 计算用户指定年份的各种假期额度
     * @param userId 用户ID
     * @param year 年份
     * @return 假期额度映射（假期类型 -> 天数）
     */
    java.util.Map<String, BigDecimal> calculateYearlyEntitlements(Long userId, Integer year);

    /**
     * 重新计算并更新指定用户年份的假期余额
     * @param userId 用户ID
     * @param year 年份
     * @return 是否更新成功
     */
    boolean recalculateUserBalance(Long userId, Integer year);

    /**
     * 批量重新计算指定年份的假期余额
     * @param userIds 用户ID列表
     * @param year 年份
     * @return 更新结果统计
     */
    InitializationResult batchRecalculateBalances(List<Long> userIds, Integer year);

    /**
     * 检查用户指定年份的假期余额是否已初始化
     * @param userId 用户ID
     * @param year 年份
     * @return 是否已初始化
     */
    boolean isBalanceInitialized(Long userId, Integer year);

    /**
     * 获取未初始化假期余额的用户列表
     * @param year 年份
     * @return 未初始化的用户ID列表
     */
    List<Long> getUninitializedUsers(Integer year);

    /**
     * 执行年度假期余额重置（谨慎使用）
     * @param year 年份
     * @param confirm 确认标识
     * @return 重置结果统计
     */
    InitializationResult resetYearlyBalances(Integer year, boolean confirm);

    /**
     * 修复不一致的假期余额数据
     * @param year 年份
     * @return 修复结果统计
     */
    CompletableFuture<RepairResult> repairInconsistentBalances(Integer year);

    /**
     * 获取假期余额初始化状态报告
     * @param year 年份
     * @return 状态报告
     */
    InitializationStatusReport getInitializationStatus(Integer year);

    /**
     * 验证假期余额配置的有效性
     * @return 验证结果
     */
    ValidationResult validateBalanceConfiguration();

    /**
     * 假期结转结果类
     */
    class CarryoverResult {
        private final int totalUsers;
        private final int processedUsers;
        private final BigDecimal totalCarryoverDays;
        private final List<String> errors;

        public CarryoverResult(int totalUsers, int processedUsers,
                              BigDecimal totalCarryoverDays, List<String> errors) {
            this.totalUsers = totalUsers;
            this.processedUsers = processedUsers;
            this.totalCarryoverDays = totalCarryoverDays;
            this.errors = errors;
        }

        public int getTotalUsers() { return totalUsers; }
        public int getProcessedUsers() { return processedUsers; }
        public BigDecimal getTotalCarryoverDays() { return totalCarryoverDays; }
        public List<String> getErrors() { return errors; }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }

    /**
     * 数据修复结果类
     */
    class RepairResult {
        private final int totalChecked;
        private final int repairedRecords;
        private final List<String> repairDetails;
        private final List<String> errors;

        public RepairResult(int totalChecked, int repairedRecords,
                          List<String> repairDetails, List<String> errors) {
            this.totalChecked = totalChecked;
            this.repairedRecords = repairedRecords;
            this.repairDetails = repairDetails;
            this.errors = errors;
        }

        public int getTotalChecked() { return totalChecked; }
        public int getRepairedRecords() { return repairedRecords; }
        public List<String> getRepairDetails() { return repairDetails; }
        public List<String> getErrors() { return errors; }
    }

    /**
     * 初始化状态报告类
     */
    class InitializationStatusReport {
        private final int totalUsers;
        private final int initializedUsers;
        private final int uninitializedUsers;
        private final java.util.Map<String, Integer> leaveTypeStats;

        public InitializationStatusReport(int totalUsers, int initializedUsers,
                                        int uninitializedUsers,
                                        java.util.Map<String, Integer> leaveTypeStats) {
            this.totalUsers = totalUsers;
            this.initializedUsers = initializedUsers;
            this.uninitializedUsers = uninitializedUsers;
            this.leaveTypeStats = leaveTypeStats;
        }

        public int getTotalUsers() { return totalUsers; }
        public int getInitializedUsers() { return initializedUsers; }
        public int getUninitializedUsers() { return uninitializedUsers; }
        public java.util.Map<String, Integer> getLeaveTypeStats() { return leaveTypeStats; }
        public double getInitializationRate() {
            return totalUsers > 0 ? (double) initializedUsers / totalUsers * 100 : 0;
        }
    }

    /**
     * 验证结果类
     */
    class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }
}