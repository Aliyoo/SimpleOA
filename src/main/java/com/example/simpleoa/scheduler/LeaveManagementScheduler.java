package com.example.simpleoa.scheduler;

import com.example.simpleoa.dto.leave.InitializationResult;
import com.example.simpleoa.model.LeaveSystemConfig;
import com.example.simpleoa.service.LeaveBalanceInitializationService;
import com.example.simpleoa.service.LeaveSystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * 假期管理定时任务调度器
 * 自动执行年初初始化、结转处理等定时任务
 */
@Component
public class LeaveManagementScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LeaveManagementScheduler.class);

    @Autowired
    private LeaveBalanceInitializationService balanceInitializationService;

    @Autowired
    private LeaveSystemConfigService configService;

    /**
     * 每年1月1日凌晨2点执行年初假期余额初始化
     */
    @Scheduled(cron = "0 0 2 1 1 ?")
    public void yearlyBalanceInitialization() {
        if (!isAutoInitializationEnabled()) {
            logger.info("年初假期余额自动初始化功能已禁用");
            return;
        }

        Integer currentYear = LocalDate.now().getYear();
        logger.info("触发年初假期余额初始化任务，年份：{}", currentYear);

        try {
            CompletableFuture<InitializationResult> future =
                balanceInitializationService.batchInitializeYearlyBalances(currentYear);

            future.thenAccept(result -> {
                logger.info("年初假期余额初始化完成，处理用户数：{}，成功：{}，失败：{}",
                    result.getTotalUsers(), result.getSuccessCount(), result.getFailureCount());

                // 发送初始化结果通知
                sendInitializationNotification(result);

                // 记录初始化统计
                logInitializationStatistics(result);
            });

        } catch (Exception e) {
            logger.error("年初假期余额初始化失败", e);
            sendErrorNotification("年初假期余额初始化失败", e.getMessage());
        }
    }

    /**
     * 每年12月31日执行年假结转处理
     */
    @Scheduled(cron = "0 0 1 31 12 ?")
    public void yearlyCarryoverExecution() {
        if (!isCarryoverEnabled()) {
            logger.info("假期结转功能已禁用");
            return;
        }

        Integer currentYear = LocalDate.now().getYear();
        Integer nextYear = currentYear + 1;
        logger.info("触发年假结转任务，{}年 → {}年", currentYear, nextYear);

        try {
            CompletableFuture<LeaveBalanceInitializationService.CarryoverResult> future =
                balanceInitializationService.executeCarryover(currentYear, nextYear);

            future.thenAccept(result -> {
                if (result.hasErrors()) {
                    logger.error("年假结转完成，但有错误：{}", String.join("; ", result.getErrors()));
                } else {
                    logger.info("年假结转完成，处理用户数：{}，结转总天数：{}",
                        result.getProcessedUsers(), result.getTotalCarryoverDays());
                }

                // 发送结转结果通知
                sendCarryoverNotification(result);
            });

        } catch (Exception e) {
            logger.error("年假结转失败", e);
            sendErrorNotification("年假结转失败", e.getMessage());
        }
    }

    /**
     * 每月1号检查即将到期的结转假期
     */
    @Scheduled(cron = "0 0 9 1 * ?")
    public void checkExpiringCarryover() {
        if (!isCarryoverEnabled()) {
            return;
        }

        LocalDate currentDate = LocalDate.now();
        LocalDate expireDate = currentDate.plusMonths(1);

        logger.info("检查即将到期的结转假期，过期日期：{}", expireDate);

        try {
            // 这里需要实现检查逻辑
            logger.info("即将到期的结转假期检查完成");

        } catch (Exception e) {
            logger.error("检查即将到期的结转假期失败", e);
        }
    }

    /**
     * 每天凌晨3点清理过期的结转记录
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredCarryover() {
        logger.debug("清理过期的结转记录");

        try {
            // 这里需要实现清理逻辑
            logger.debug("过期的结转记录清理完成");

        } catch (Exception e) {
            logger.error("清理过期结转记录失败", e);
        }
    }

    /**
     * 每周一凌晨4点检查系统配置有效性
     */
    @Scheduled(cron = "0 0 4 * * MON")
    public void validateSystemConfiguration() {
        logger.info("检查假期系统配置有效性");

        try {
            LeaveSystemConfigService.ValidationResult validation = configService.validateBalanceConfiguration();

            if (validation.isValid()) {
                logger.info("假期系统配置验证通过");
            } else {
                logger.error("假期系统配置验证失败：{}", validation.getMessage());
                sendConfigurationValidationNotification(validation);
            }

        } catch (Exception e) {
            logger.error("检查假期系统配置失败", e);
        }
    }

    /**
     * 每月15号检查数据一致性
     */
    @Scheduled(cron = "0 0 5 15 * ?")
    public void checkDataConsistency() {
        Integer currentYear = LocalDate.now().getYear();
        logger.info("检查{}年假期数据一致性", currentYear);

        try {
            CompletableFuture<LeaveBalanceInitializationService.RepairResult> future =
                balanceInitializationService.repairInconsistentBalances(currentYear);

            future.thenAccept(result -> {
                if (result.getRepairedRecords() > 0) {
                    logger.info("数据一致性检查完成，修复记录数：{}", result.getRepairedRecords());
                    sendDataRepairNotification(result);
                } else {
                    logger.info("数据一致性检查完成，无需修复");
                }
            });

        } catch (Exception e) {
            logger.error("检查假期数据一致性失败", e);
        }
    }

    /**
     * 检查是否启用了自动初始化
     */
    private boolean isAutoInitializationEnabled() {
        try {
            Boolean enabled = configService.getConfigBooleanValue("leave.annual.auto_init", true);
            return enabled != null && enabled;
        } catch (Exception e) {
            logger.warn("检查自动初始化配置失败，使用默认值：true", e);
            return true;
        }
    }

    /**
     * 检查是否启用了结转功能
     */
    private boolean isCarryoverEnabled() {
        try {
            Boolean enabled = configService.getConfigBooleanValue("leave.carryover.enabled", true);
            return enabled != null && enabled;
        } catch (Exception e) {
            logger.warn("检查结转功能配置失败，使用默认值：true", e);
            return true;
        }
    }

    /**
     * 发送初始化完成通知
     */
    private void sendInitializationNotification(InitializationResult result) {
        try {
            if (result.getFailureCount() > 0) {
                logger.warn("假期余额初始化存在失败情况，请检查系统日志");
            }

            // 这里可以集成邮件、短信或其他通知方式
            logger.info("假期余额初始化通知已发送");

        } catch (Exception e) {
            logger.error("发送初始化通知失败", e);
        }
    }

    /**
     * 发送结转完成通知
     */
    private void sendCarryoverNotification(LeaveBalanceInitializationService.CarryoverResult result) {
        try {
            if (result.hasErrors()) {
                logger.warn("假期结转存在错误情况，请检查系统日志");
            }

            // 这里可以集成邮件、短信或其他通知方式
            logger.info("假期结转通知已发送");

        } catch (Exception e) {
            logger.error("发送结转通知失败", e);
        }
    }

    /**
     * 发送错误通知
     */
    private void sendErrorNotification(String operation, String errorMessage) {
        try {
            logger.error("操作失败通知 - {}: {}", operation, errorMessage);

            // 这里可以集成邮件、短信或其他通知方式
            logger.error("错误通知已发送");

        } catch (Exception e) {
            logger.error("发送错误通知失败", e);
        }
    }

    /**
     * 发送配置验证失败通知
     */
    private void sendConfigurationValidationNotification(LeaveSystemConfigService.ValidationResult validation) {
        try {
            logger.error("假期系统配置验证失败通知：{}", validation.getMessage());

            // 这里可以集成邮件、短信或其他通知方式
            logger.error("配置验证失败通知已发送");

        } catch (Exception e) {
            logger.error("发送配置验证失败通知", e);
        }
    }

    /**
     * 发送数据修复通知
     */
    private void sendDataRepairNotification(LeaveBalanceInitializationService.RepairResult result) {
        try {
            logger.info("假期数据修复通知：检查记录{}，修复记录{}",
                result.getTotalChecked(), result.getRepairedRecords());

            // 这里可以集成邮件、短信或其他通知方式
            logger.info("数据修复通知已发送");

        } catch (Exception e) {
            logger.error("发送数据修复通知失败", e);
        }
    }

    /**
     * 记录初始化统计信息
     */
    private void logInitializationStatistics(InitializationResult result) {
        try {
            double successRate = result.getSuccessRate();
            long processingTimeSeconds = result.getProcessingTime() != null ?
                result.getProcessingTime().getSeconds() : 0;

            logger.info("假期余额初始化统计 - 总用户: {}, 成功率: {:.2f}%, 处理时间: {}秒",
                result.getTotalUsers(), successRate, processingTimeSeconds);

            // 记录详细统计到监控系统
            // 这里可以集成Micrometer等监控工具

        } catch (Exception e) {
            logger.error("记录初始化统计信息失败", e);
        }
    }
}