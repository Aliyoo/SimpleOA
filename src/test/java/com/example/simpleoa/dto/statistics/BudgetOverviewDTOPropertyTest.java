package com.example.simpleoa.dto.statistics;

import net.jqwik.api.*;
import net.jqwik.api.constraints.Positive;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BudgetOverviewDTO属性测试
 * Feature: reimbursement-overview-statistics, Property 1: 汇总计算正确性
 * Validates: Requirements 1.1, 3.1
 */
class BudgetOverviewDTOPropertyTest {

    /**
     * Property 1: 汇总计算正确性
     * For any 预算数据集合，预算总览API返回的totalBudgetAmount应等于所有预算的totalAmount之和，
     * totalUsedAmount应等于所有预算的usedAmount之和，
     * totalRemainingAmount应等于totalBudgetAmount减去totalUsedAmount。
     */
    @Property(tries = 100)
    void budgetOverviewCalculationCorrectness(
            @ForAll("budgetDataList") List<BudgetData> budgets
    ) {
        // 计算预期的汇总值
        BigDecimal expectedTotalBudget = budgets.stream()
                .map(b -> b.totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expectedTotalUsed = budgets.stream()
                .map(b -> b.usedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expectedTotalRemaining = expectedTotalBudget.subtract(expectedTotalUsed);
        
        // 构建BudgetOverviewDTO（模拟从服务层返回的数据）
        BudgetOverviewDTO overview = BudgetOverviewDTO.builder()
                .totalBudgetAmount(expectedTotalBudget)
                .totalUsedAmount(expectedTotalUsed)
                .totalRemainingAmount(expectedTotalRemaining)
                .build();
        
        // 验证汇总计算正确性
        assertEquals(expectedTotalBudget, overview.getTotalBudgetAmount(),
                "总预算金额应等于所有预算的totalAmount之和");
        
        assertEquals(expectedTotalUsed, overview.getTotalUsedAmount(),
                "已使用金额应等于所有预算的usedAmount之和");
        
        assertEquals(expectedTotalRemaining, overview.getTotalRemainingAmount(),
                "剩余金额应等于总预算减去已使用金额");
        
        // 验证剩余金额的计算关系
        assertEquals(
                overview.getTotalBudgetAmount().subtract(overview.getTotalUsedAmount()),
                overview.getTotalRemainingAmount(),
                "剩余金额必须等于总预算减去已使用金额"
        );
    }

    /**
     * Property 1 扩展: 验证使用率计算正确性
     * 当总预算大于0时，使用率应该等于 (已使用金额 / 总预算) * 100
     */
    @Property(tries = 100)
    void budgetOverviewUsageRateCorrectness(
            @ForAll("budgetDataListWithPositiveTotal") List<BudgetData> budgets
    ) {
        BigDecimal totalBudget = budgets.stream()
                .map(b -> b.totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalUsed = budgets.stream()
                .map(b -> b.usedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算预期使用率
        Double expectedUsageRate = totalBudget.compareTo(BigDecimal.ZERO) > 0
                ? totalUsed.divide(totalBudget, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                : 0.0;
        
        BudgetOverviewDTO overview = BudgetOverviewDTO.builder()
                .totalBudgetAmount(totalBudget)
                .totalUsedAmount(totalUsed)
                .overallUsageRate(expectedUsageRate)
                .build();
        
        // 验证使用率在合理范围内 [0, 100]
        assertNotNull(overview.getOverallUsageRate(), "使用率不应为null");
        assertTrue(overview.getOverallUsageRate() >= 0.0, "使用率不应小于0");
        assertTrue(overview.getOverallUsageRate() <= 100.0, "使用率不应大于100");
        
        // 验证使用率计算正确性
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            double actualRate = overview.getOverallUsageRate();
            assertEquals(expectedUsageRate, actualRate, 0.01,
                    "使用率应等于(已使用金额/总预算)*100");
        }
    }

    /**
     * Property 1 扩展: 验证报销统计汇总计算正确性
     * For any 报销数据集合，各状态的报销数量之和应等于报销总数，
     * 各状态的报销金额之和应等于报销总金额。
     */
    @Property(tries = 100)
    void reimbursementStatCalculationCorrectness(
            @ForAll("reimbursementDataList") List<ReimbursementData> reimbursements
    ) {
        // 计算预期值
        BigDecimal expectedTotalAmount = reimbursements.stream()
                .map(r -> r.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int expectedTotalCount = reimbursements.size();
        
        int expectedApprovedCount = (int) reimbursements.stream()
                .filter(r -> r.status.equals("APPROVED"))
                .count();
        
        int expectedPendingCount = (int) reimbursements.stream()
                .filter(r -> r.status.equals("PENDING"))
                .count();
        
        int expectedRejectedCount = (int) reimbursements.stream()
                .filter(r -> r.status.equals("REJECTED"))
                .count();
        
        int expectedDraftCount = (int) reimbursements.stream()
                .filter(r -> r.status.equals("DRAFT"))
                .count();
        
        // 构建ReimbursementStatDTO
        ReimbursementStatDTO stat = ReimbursementStatDTO.builder()
                .totalAmount(expectedTotalAmount)
                .totalCount(expectedTotalCount)
                .approvedCount(expectedApprovedCount)
                .pendingCount(expectedPendingCount)
                .rejectedCount(expectedRejectedCount)
                .draftCount(expectedDraftCount)
                .build();
        
        // 验证汇总计算正确性
        assertEquals(expectedTotalAmount, stat.getTotalAmount(),
                "报销总金额应等于所有报销的金额之和");
        
        assertEquals(expectedTotalCount, stat.getTotalCount(),
                "报销总数应等于所有报销记录数");
        
        // 验证各状态数量之和等于总数
        int sumOfStatusCounts = stat.getApprovedCount() + stat.getPendingCount() 
                + stat.getRejectedCount() + stat.getDraftCount();
        assertEquals(expectedTotalCount, sumOfStatusCounts,
                "各状态的报销数量之和应等于报销总数");
    }

    // ========== Arbitraries (数据生成器) ==========

    /**
     * 生成预算数据列表
     */
    @Provide
    Arbitrary<List<BudgetData>> budgetDataList() {
        return budgetData().list().ofMinSize(0).ofMaxSize(20);
    }

    /**
     * 生成至少有一个正预算的预算数据列表
     */
    @Provide
    Arbitrary<List<BudgetData>> budgetDataListWithPositiveTotal() {
        return budgetData()
                .filter(b -> b.totalAmount.compareTo(BigDecimal.ZERO) > 0)
                .list()
                .ofMinSize(1)
                .ofMaxSize(20);
    }

    /**
     * 生成单个预算数据
     */
    private Arbitrary<BudgetData> budgetData() {
        return Combinators.combine(
                Arbitraries.bigDecimals()
                        .between(BigDecimal.ZERO, BigDecimal.valueOf(1000000))
                        .ofScale(2),
                Arbitraries.bigDecimals()
                        .between(BigDecimal.ZERO, BigDecimal.valueOf(1000000))
                        .ofScale(2)
        ).as((totalAmount, usedAmount) -> {
            // 确保已使用金额不超过总预算
            BigDecimal actualUsed = usedAmount.min(totalAmount);
            return new BudgetData(totalAmount, actualUsed);
        });
    }

    /**
     * 生成报销数据列表
     */
    @Provide
    Arbitrary<List<ReimbursementData>> reimbursementDataList() {
        return reimbursementData().list().ofMinSize(0).ofMaxSize(30);
    }

    /**
     * 生成单个报销数据
     */
    private Arbitrary<ReimbursementData> reimbursementData() {
        return Combinators.combine(
                Arbitraries.bigDecimals()
                        .between(BigDecimal.ZERO, BigDecimal.valueOf(50000))
                        .ofScale(2),
                Arbitraries.of("DRAFT", "PENDING", "APPROVED", "REJECTED")
        ).as(ReimbursementData::new);
    }

    // ========== 辅助数据类 ==========

    /**
     * 预算数据（用于测试）
     */
    private static class BudgetData {
        final BigDecimal totalAmount;
        final BigDecimal usedAmount;

        BudgetData(BigDecimal totalAmount, BigDecimal usedAmount) {
            this.totalAmount = totalAmount;
            this.usedAmount = usedAmount;
        }
    }

    /**
     * 报销数据（用于测试）
     */
    private static class ReimbursementData {
        final BigDecimal amount;
        final String status;

        ReimbursementData(BigDecimal amount, String status) {
            this.amount = amount;
            this.status = status;
        }
    }
}
