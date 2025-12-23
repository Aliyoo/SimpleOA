package com.example.simpleoa.dto.leave;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.List;

/**
 * 假期余额初始化结果DTO
 * 包含批量初始化的统计信息和结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializationResult {

    /**
     * 目标年份
     */
    private Integer year;

    /**
     * 处理的用户总数
     */
    private Integer totalUsers;

    /**
     * 成功处理的用户数
     */
    private Integer successCount;

    /**
     * 失败处理的用户数
     */
    private Integer failureCount;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 处理耗时
     */
    private Duration processingTime;

    /**
     * 处理开始时间
     */
    private java.time.Instant startTime;

    /**
     * 处理结束时间
     */
    private java.time.Instant endTime;

    /**
     * 成功率百分比
     * @return 成功率
     */
    public Double getSuccessRate() {
        if (totalUsers == null || totalUsers == 0) {
            return 0.0;
        }
        return (successCount != null ? successCount.doubleValue() : 0.0) / totalUsers * 100.0;
    }

    /**
     * 获取处理状态描述
     * @return 状态描述
     */
    public String getStatusDescription() {
        if (failureCount == null || failureCount == 0) {
            return "全部成功";
        } else if (successCount != null && failureCount > 0 && successCount > 0) {
            return String.format("部分成功（成功：%d，失败：%d）", successCount, failureCount);
        } else {
            return String.format("全部失败（失败：%d）", failureCount);
        }
    }

    /**
     * 获取处理速度（用户/秒）
     * @return 处理速度
     */
    public Double getProcessingSpeed() {
        if (processingTime == null || processingTime.isZero() || totalUsers == null || totalUsers == 0) {
            return 0.0;
        }
        return totalUsers.doubleValue() / processingTime.getSeconds();
    }

    /**
     * 获取详细报告
     * @return 详细报告字符串
     */
    public String getDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 假期余额初始化报告 ===\n");
        sb.append("年份：").append(year).append("\n");
        sb.append("总用户数：").append(totalUsers).append("\n");
        sb.append("成功处理：").append(successCount).append("\n");
        sb.append("处理失败：").append(failureCount).append("\n");
        sb.append("成功率：").append(String.format("%.2f%%", getSuccessRate())).append("\n");

        if (processingTime != null) {
            sb.append("处理耗时：").append(processingTime.toMinutes()).append("分钟\n");
            sb.append("处理速度：").append(String.format("%.2f", getProcessingSpeed())).append("用户/秒\n");
        }

        if (errors != null && !errors.isEmpty()) {
            sb.append("\n=== 错误详情 ===\n");
            for (int i = 0; i < Math.min(errors.size(), 10); i++) {
                sb.append((i + 1)).append(". ").append(errors.get(i)).append("\n");
            }
            if (errors.size() > 10) {
                sb.append("... 还有").append(errors.size() - 10).append("个错误\n");
            }
        }

        return sb.toString();
    }
}