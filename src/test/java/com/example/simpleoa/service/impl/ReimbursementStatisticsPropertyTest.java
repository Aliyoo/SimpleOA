package com.example.simpleoa.service.impl;

import com.example.simpleoa.dto.statistics.*;
import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.BudgetExpenseRepository;
import com.example.simpleoa.repository.BudgetRepository;
import com.example.simpleoa.repository.ReimbursementRequestRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 报销统计属性测试
 * Feature: reimbursement-overview-statistics, Property 6: 报销状态统计正确性
 * Validates: Requirements 3.2
 */
class ReimbursementStatisticsPropertyTest {

    private BudgetRepository budgetRepository;
    private BudgetExpenseRepository budgetExpenseRepository;
    private ReimbursementRequestRepository reimbursementRequestRepository;
    private BudgetStatisticsServiceImpl service;

    /**
     * Property 6: 报销状态统计正确性
     * For any 报销数据集合，各状态的报销数量之和应等于报销总数，
     * 各状态的报销金额之和应等于报销总金额。
     */
    @Property(tries = 100)
    void reimbursementStatusStatisticsCorrectness(
            @ForAll("reimbursementRequests") List<ReimbursementRequest> reimbursements
    ) {
        // 初始化mocks
        budgetRepository = Mockito.mock(BudgetRepository.class);
        budgetExpenseRepository = Mockito.mock(BudgetExpenseRepository.class);
        reimbursementRequestRepository = Mockito.mock(ReimbursementRequestRepository.class);
        service = new BudgetStatisticsServiceImpl(budgetRepository, budgetExpenseRepository, reimbursementRequestRepository);
        
        // 设置mock返回数据
        when(reimbursementRequestRepository.findAll()).thenReturn(reimbursements);
        
        // 调用服务方法
        ReimbursementStatDTO stat = service.getReimbursementStatistics(null, null);
        
        // 计算预期的报销总金额
        BigDecimal expectedTotalAmount = reimbursements.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算预期的报销总数
        int expectedTotalCount = reimbursements.size();
        
        // 计算各状态的预期数量
        int expectedDraftCount = (int) reimbursements.stream()
                .filter(r -> ReimbursementStatus.DRAFT.equals(r.getStatus()))
                .count();
        
        int expectedPendingCount = (int) reimbursements.stream()
                .filter(r -> ReimbursementStatus.PENDING_MANAGER_APPROVAL.equals(r.getStatus()) 
                        || ReimbursementStatus.PENDING_FINANCE_APPROVAL.equals(r.getStatus()))
                .count();
        
        int expectedApprovedCount = (int) reimbursements.stream()
                .filter(r -> ReimbursementStatus.APPROVED.equals(r.getStatus()))
                .count();
        
        int expectedRejectedCount = (int) reimbursements.stream()
                .filter(r -> ReimbursementStatus.REJECTED.equals(r.getStatus()))
                .count();
        
        // 验证报销总金额
        assertNotNull(stat.getTotalAmount(), "报销总金额不应为null");
        assertEquals(0, expectedTotalAmount.compareTo(stat.getTotalAmount()),
                "报销总金额应等于所有报销的金额之和");
        
        // 验证报销总数
        assertNotNull(stat.getTotalCount(), "报销总数不应为null");
        assertEquals(expectedTotalCount, stat.getTotalCount(),
                "报销总数应等于所有报销记录数");
        
        // 验证各状态数量
        assertNotNull(stat.getDraftCount(), "草稿数量不应为null");
        assertEquals(expectedDraftCount, stat.getDraftCount(),
                "草稿数量应正确统计");
        
        assertNotNull(stat.getPendingCount(), "待审批数量不应为null");
        assertEquals(expectedPendingCount, stat.getPendingCount(),
                "待审批数量应正确统计");
        
        assertNotNull(stat.getApprovedCount(), "已审批数量不应为null");
        assertEquals(expectedApprovedCount, stat.getApprovedCount(),
                "已审批数量应正确统计");
        
        assertNotNull(stat.getRejectedCount(), "已驳回数量不应为null");
        assertEquals(expectedRejectedCount, stat.getRejectedCount(),
                "已驳回数量应正确统计");
        
        // 核心验证：各状态的报销数量之和应等于报销总数
        int sumOfStatusCounts = stat.getDraftCount() + stat.getPendingCount() 
                + stat.getApprovedCount() + stat.getRejectedCount();
        
        assertEquals(expectedTotalCount, sumOfStatusCounts,
                "各状态的报销数量之和应等于报销总数");
    }

    /**
     * Property 6 扩展: 验证状态分布的金额汇总正确性
     * For any 报销数据集合，状态分布中各状态的金额之和应等于报销总金额。
     */
    @Property(tries = 100)
    void reimbursementStatusDistributionAmountCorrectness(
            @ForAll("reimbursementRequests") List<ReimbursementRequest> reimbursements
    ) {
        // 初始化mocks
        budgetRepository = Mockito.mock(BudgetRepository.class);
        budgetExpenseRepository = Mockito.mock(BudgetExpenseRepository.class);
        reimbursementRequestRepository = Mockito.mock(ReimbursementRequestRepository.class);
        service = new BudgetStatisticsServiceImpl(budgetRepository, budgetExpenseRepository, reimbursementRequestRepository);
        
        // 设置mock返回数据
        when(reimbursementRequestRepository.findAll()).thenReturn(reimbursements);
        
        // 调用服务方法
        ReimbursementStatDTO stat = service.getReimbursementStatistics(null, null);
        
        // 如果没有状态分布数据，跳过验证
        if (stat.getStatusDistribution() == null || stat.getStatusDistribution().isEmpty()) {
            return;
        }
        
        // 计算预期的报销总金额
        BigDecimal expectedTotalAmount = reimbursements.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算状态分布中各状态的金额之和
        BigDecimal sumOfStatusAmounts = stat.getStatusDistribution().stream()
                .map(StatusDistributionDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 验证：状态分布中各状态的金额之和应等于报销总金额
        assertEquals(0, expectedTotalAmount.compareTo(sumOfStatusAmounts),
                "状态分布中各状态的金额之和应等于报销总金额");
        
        // 验证：状态分布中各状态的数量之和应等于报销总数
        int sumOfStatusDistributionCounts = stat.getStatusDistribution().stream()
                .mapToInt(StatusDistributionDTO::getCount)
                .sum();
        
        assertEquals(reimbursements.size(), sumOfStatusDistributionCounts,
                "状态分布中各状态的数量之和应等于报销总数");
    }

    /**
     * Property 6 扩展: 验证状态分布的百分比计算正确性
     * For any 报销数据集合，状态分布中各状态的百分比之和应约等于100%。
     */
    @Property(tries = 100)
    void reimbursementStatusDistributionPercentageCorrectness(
            @ForAll("reimbursementRequestsNonEmpty") List<ReimbursementRequest> reimbursements
    ) {
        // 初始化mocks
        budgetRepository = Mockito.mock(BudgetRepository.class);
        budgetExpenseRepository = Mockito.mock(BudgetExpenseRepository.class);
        reimbursementRequestRepository = Mockito.mock(ReimbursementRequestRepository.class);
        service = new BudgetStatisticsServiceImpl(budgetRepository, budgetExpenseRepository, reimbursementRequestRepository);
        
        // 设置mock返回数据
        when(reimbursementRequestRepository.findAll()).thenReturn(reimbursements);
        
        // 调用服务方法
        ReimbursementStatDTO stat = service.getReimbursementStatistics(null, null);
        
        // 如果没有状态分布数据，跳过验证
        if (stat.getStatusDistribution() == null || stat.getStatusDistribution().isEmpty()) {
            return;
        }
        
        // 计算状态分布中各状态的百分比之和
        double sumOfPercentages = stat.getStatusDistribution().stream()
                .mapToDouble(StatusDistributionDTO::getPercentage)
                .sum();
        
        // 验证：各状态的百分比之和应约等于100%（允许舍入误差）
        assertTrue(Math.abs(sumOfPercentages - 100.0) < 0.1,
                "状态分布中各状态的百分比之和应约等于100%，实际值: " + sumOfPercentages);
    }

    /**
     * Property 6 扩展: 验证空数据集的处理正确性
     * For any 空报销数据集合，统计结果应返回零值而不是null。
     */
    @Property(tries = 20)
    void reimbursementStatisticsHandlesEmptyDataCorrectly() {
        // 初始化mocks
        budgetRepository = Mockito.mock(BudgetRepository.class);
        budgetExpenseRepository = Mockito.mock(BudgetExpenseRepository.class);
        reimbursementRequestRepository = Mockito.mock(ReimbursementRequestRepository.class);
        service = new BudgetStatisticsServiceImpl(budgetRepository, budgetExpenseRepository, reimbursementRequestRepository);
        
        // 设置mock返回空列表
        when(reimbursementRequestRepository.findAll()).thenReturn(Collections.emptyList());
        
        // 调用服务方法
        ReimbursementStatDTO stat = service.getReimbursementStatistics(null, null);
        
        // 验证：空数据集应返回零值而不是null
        assertNotNull(stat, "统计结果不应为null");
        assertNotNull(stat.getTotalAmount(), "报销总金额不应为null");
        assertEquals(0, stat.getTotalAmount().compareTo(BigDecimal.ZERO),
                "空数据集的报销总金额应为0");
        
        assertNotNull(stat.getTotalCount(), "报销总数不应为null");
        assertEquals(0, stat.getTotalCount(),
                "空数据集的报销总数应为0");
        
        assertEquals(0, stat.getDraftCount(), "空数据集的草稿数量应为0");
        assertEquals(0, stat.getPendingCount(), "空数据集的待审批数量应为0");
        assertEquals(0, stat.getApprovedCount(), "空数据集的已审批数量应为0");
        assertEquals(0, stat.getRejectedCount(), "空数据集的已驳回数量应为0");
    }

    // ========== Arbitraries (数据生成器) ==========

    /**
     * 生成报销申请数据列表（可能为空）
     */
    @Provide
    Arbitrary<List<ReimbursementRequest>> reimbursementRequests() {
        return Arbitraries.integers().between(0, 30).flatMap(count -> {
            if (count == 0) {
                return Arbitraries.just(new ArrayList<>());
            }
            
            List<Arbitrary<ReimbursementRequest>> requestArbitraries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                requestArbitraries.add(reimbursementRequest());
            }
            
            return Combinators.combine(requestArbitraries).as(requests -> requests);
        });
    }

    /**
     * 生成非空报销申请数据列表
     */
    @Provide
    Arbitrary<List<ReimbursementRequest>> reimbursementRequestsNonEmpty() {
        return Arbitraries.integers().between(1, 30).flatMap(count -> {
            List<Arbitrary<ReimbursementRequest>> requestArbitraries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                requestArbitraries.add(reimbursementRequest());
            }
            
            return Combinators.combine(requestArbitraries).as(requests -> requests);
        });
    }

    /**
     * 生成单个报销申请
     */
    private Arbitrary<ReimbursementRequest> reimbursementRequest() {
        return Combinators.combine(
                Arbitraries.of(
                        ReimbursementStatus.DRAFT,
                        ReimbursementStatus.PENDING_MANAGER_APPROVAL,
                        ReimbursementStatus.PENDING_FINANCE_APPROVAL,
                        ReimbursementStatus.APPROVED,
                        ReimbursementStatus.REJECTED
                ),
                Arbitraries.doubles().between(100.0, 50000.0)
        ).as((status, amount) -> {
            ReimbursementRequest request = new ReimbursementRequest();
            request.setId(new Random().nextLong(1000000));
            request.setStatus(status);
            request.setTotalAmount(BigDecimal.valueOf(amount));
            request.setCreateTime(LocalDateTime.now());
            
            // 为已审批的报销添加审批时间
            if (status == ReimbursementStatus.APPROVED || status == ReimbursementStatus.REJECTED) {
                request.setUpdateTime(request.getCreateTime().plusDays(new Random().nextInt(10) + 1));
            } else {
                request.setUpdateTime(request.getCreateTime());
            }
            
            return request;
        });
    }
}
