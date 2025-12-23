package com.example.simpleoa.service.impl;

import com.example.simpleoa.dto.statistics.*;
import com.example.simpleoa.model.*;
import com.example.simpleoa.repository.BudgetExpenseRepository;
import com.example.simpleoa.repository.BudgetRepository;
import com.example.simpleoa.repository.ReimbursementRequestRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 多维度统计属性测试
 * Feature: reimbursement-overview-statistics, Property 2: 多维度分组统计正确性
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4
 */
class MultiDimensionStatisticsPropertyTest {

    private BudgetRepository budgetRepository;
    private BudgetExpenseRepository budgetExpenseRepository;
    private ReimbursementRequestRepository reimbursementRequestRepository;
    private BudgetStatisticsServiceImpl service;
    
    /**
     * 初始化mock对象
     * jqwik不支持JUnit扩展，需要手动初始化
     */
    private void initMocks() {
        budgetRepository = Mockito.mock(BudgetRepository.class);
        budgetExpenseRepository = Mockito.mock(BudgetExpenseRepository.class);
        reimbursementRequestRepository = Mockito.mock(ReimbursementRequestRepository.class);
        service = new BudgetStatisticsServiceImpl(budgetRepository, budgetExpenseRepository, reimbursementRequestRepository);
    }

    /**
     * Property 2.1: 按项目分组统计正确性
     * For any 预算数据集合，按项目分组统计时，各项目的预算金额之和应等于总预算金额，
     * 且每个预算只能属于一个项目。
     */
    @Property(tries = 100)
    void projectGroupingStatisticsCorrectness(
            @ForAll("budgetsByProject") List<Budget> budgets
    ) {
        // 初始化mock对象
        initMocks();
        
        // 设置mock返回数据
        when(budgetRepository.findAll()).thenReturn(budgets);
        when(budgetRepository.findByStartDateBetweenOrEndDateBetween(any(), any(), any(), any()))
                .thenReturn(budgets);
        
        // 调用服务方法
        List<ProjectBudgetStatDTO> stats = service.getStatisticsByProject(null, null);
        
        // 计算预期的总预算金额
        BigDecimal expectedTotalBudget = budgets.stream()
                .map(b -> BigDecimal.valueOf(b.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算统计结果的总预算金额
        BigDecimal actualTotalBudget = stats.stream()
                .map(ProjectBudgetStatDTO::getTotalBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 验证：各项目的预算金额之和应等于总预算金额
        assertEquals(0, expectedTotalBudget.compareTo(actualTotalBudget),
                "按项目分组统计时，各项目的预算金额之和应等于总预算金额");
        
        // 验证：每个预算只能属于一个项目（通过检查统计结果的预算数量之和）
        int expectedBudgetCount = budgets.size();
        int actualBudgetCount = stats.stream()
                .mapToInt(ProjectBudgetStatDTO::getBudgetCount)
                .sum();
        
        assertEquals(expectedBudgetCount, actualBudgetCount,
                "每个预算只能属于一个项目，统计的预算数量之和应等于总预算数量");
        
        // 验证：各项目的已使用金额之和应等于总已使用金额
        BigDecimal expectedTotalUsed = budgets.stream()
                .map(b -> BigDecimal.valueOf(b.getUsedAmount() != null ? b.getUsedAmount() : 0.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal actualTotalUsed = stats.stream()
                .map(ProjectBudgetStatDTO::getUsedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertEquals(0, expectedTotalUsed.compareTo(actualTotalUsed),
                "按项目分组统计时，各项目的已使用金额之和应等于总已使用金额");
    }

    /**
     * Property 2.2: 按费用类别分组统计正确性
     * For any 预算支出数据集合，按费用类别分组统计时，各类别的支出金额之和应等于总支出金额，
     * 且每条支出记录只能属于一个类别。
     */
    @Property(tries = 100)
    void categoryGroupingStatisticsCorrectness(
            @ForAll("budgetExpensesByCategory") List<BudgetExpense> expenses
    ) {
        // 初始化mock对象
        initMocks();
        
        // 设置mock返回数据
        when(budgetExpenseRepository.findAll()).thenReturn(expenses);
        when(budgetExpenseRepository.findByProjectId(any())).thenReturn(expenses);
        
        // 调用服务方法
        List<CategoryStatDTO> stats = service.getStatisticsByCategory(null, null, null);
        
        // 计算预期的总支出金额
        BigDecimal expectedTotalAmount = expenses.stream()
                .map(e -> BigDecimal.valueOf(e.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算统计结果的总支出金额
        BigDecimal actualTotalAmount = stats.stream()
                .map(CategoryStatDTO::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 验证：各类别的支出金额之和应等于总支出金额
        assertEquals(0, expectedTotalAmount.compareTo(actualTotalAmount),
                "按费用类别分组统计时，各类别的支出金额之和应等于总支出金额");
        
        // 验证：每条支出记录只能属于一个类别（通过检查统计结果的数量之和）
        int expectedExpenseCount = expenses.size();
        int actualExpenseCount = stats.stream()
                .mapToInt(CategoryStatDTO::getCount)
                .sum();
        
        assertEquals(expectedExpenseCount, actualExpenseCount,
                "每条支出记录只能属于一个类别，统计的支出数量之和应等于总支出数量");
        
        // 验证：各类别的百分比之和应约等于100%（允许舍入误差）
        if (!stats.isEmpty() && expectedTotalAmount.compareTo(BigDecimal.ZERO) > 0) {
            double totalPercentage = stats.stream()
                    .mapToDouble(CategoryStatDTO::getPercentage)
                    .sum();
            
            assertTrue(Math.abs(totalPercentage - 100.0) < 0.1,
                    "各类别的百分比之和应约等于100%，实际值: " + totalPercentage);
        }
    }

    /**
     * Property 2.3: 按时间维度分组统计正确性
     * For any 预算和支出数据集合，按时间维度分组统计时，各时间段的预算金额之和应等于总预算金额。
     */
    @Property(tries = 100)
    void timeSeriesGroupingStatisticsCorrectness(
            @ForAll("budgetsWithDates") List<Budget> budgets,
            @ForAll("expensesWithDates") List<BudgetExpense> expenses
    ) {
        // 初始化mock对象
        initMocks();
        
        // 设置mock返回数据
        when(budgetRepository.findAll()).thenReturn(budgets);
        when(budgetRepository.findByStartDateBetweenOrEndDateBetween(any(), any(), any(), any()))
                .thenReturn(budgets);
        when(budgetExpenseRepository.findAll()).thenReturn(expenses);
        
        // 调用服务方法（按月统计）
        List<TimeSeriesStatDTO> stats = service.getStatisticsByTime("MONTH", null, null);
        
        // 计算预期的总预算金额
        BigDecimal expectedTotalBudget = budgets.stream()
                .map(b -> BigDecimal.valueOf(b.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算统计结果的总预算金额
        BigDecimal actualTotalBudget = stats.stream()
                .map(TimeSeriesStatDTO::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 验证：各时间段的预算金额之和应等于总预算金额
        assertEquals(0, expectedTotalBudget.compareTo(actualTotalBudget),
                "按时间维度分组统计时，各时间段的预算金额之和应等于总预算金额");
        
        // 计算预期的总支出金额
        BigDecimal expectedTotalExpense = expenses.stream()
                .map(e -> BigDecimal.valueOf(e.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算统计结果的总报销金额
        BigDecimal actualTotalExpense = stats.stream()
                .map(TimeSeriesStatDTO::getReimbursementAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 验证：各时间段的报销金额之和应等于总报销金额
        assertEquals(0, expectedTotalExpense.compareTo(actualTotalExpense),
                "按时间维度分组统计时，各时间段的报销金额之和应等于总报销金额");
    }

    /**
     * Property 2.4: 按项目经理分组统计正确性
     * For any 预算数据集合，按项目经理分组统计时，各经理的预算金额之和应等于总预算金额，
     * 且每个预算只能属于一个项目经理。
     */
    @Property(tries = 100)
    void managerGroupingStatisticsCorrectness(
            @ForAll("budgetsByManager") List<Budget> budgets
    ) {
        // 初始化mock对象
        initMocks();
        
        // 设置mock返回数据
        when(budgetRepository.findAll()).thenReturn(budgets);
        when(budgetRepository.findByStartDateBetweenOrEndDateBetween(any(), any(), any(), any()))
                .thenReturn(budgets);
        
        // 调用服务方法
        List<ManagerStatDTO> stats = service.getStatisticsByManager(null, null);
        
        // 计算预期的总预算金额
        BigDecimal expectedTotalBudget = budgets.stream()
                .map(b -> BigDecimal.valueOf(b.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算统计结果的总预算金额
        BigDecimal actualTotalBudget = stats.stream()
                .map(ManagerStatDTO::getTotalBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 验证：各经理的预算金额之和应等于总预算金额
        assertEquals(0, expectedTotalBudget.compareTo(actualTotalBudget),
                "按项目经理分组统计时，各经理的预算金额之和应等于总预算金额");
        
        // 验证：各经理的已使用金额之和应等于总已使用金额
        BigDecimal expectedTotalUsed = budgets.stream()
                .map(b -> BigDecimal.valueOf(b.getUsedAmount() != null ? b.getUsedAmount() : 0.0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal actualTotalUsed = stats.stream()
                .map(ManagerStatDTO::getUsedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertEquals(0, expectedTotalUsed.compareTo(actualTotalUsed),
                "按项目经理分组统计时，各经理的已使用金额之和应等于总已使用金额");
        
        // 验证：统计的项目数量之和应大于等于预算数量（一个项目可能有多个预算）
        int budgetCount = budgets.size();
        int totalProjectCount = stats.stream()
                .mapToInt(ManagerStatDTO::getProjectCount)
                .sum();
        
        assertTrue(totalProjectCount <= budgetCount,
                "统计的项目数量之和应小于等于预算数量（因为一个项目可能有多个预算）");
    }

    // ========== Arbitraries (数据生成器) ==========

    /**
     * 生成按项目分组的预算数据列表
     */
    @Provide
    Arbitrary<List<Budget>> budgetsByProject() {
        return Arbitraries.integers().between(1, 10).flatMap(projectCount -> {
            // 生成多个项目
            List<Arbitrary<Project>> projectArbitraries = new ArrayList<>();
            for (int i = 0; i < projectCount; i++) {
                final int projectId = i + 1;
                projectArbitraries.add(Arbitraries.just(createProject(projectId)));
            }
            
            // 为每个项目生成预算
            return Combinators.combine(projectArbitraries).as(projects -> {
                List<Budget> budgets = new ArrayList<>();
                for (Project project : projects) {
                    // 每个项目生成1-5个预算
                    int budgetCountForProject = new Random().nextInt(5) + 1;
                    for (int i = 0; i < budgetCountForProject; i++) {
                        budgets.add(createBudget(project));
                    }
                }
                return budgets;
            });
        });
    }

    /**
     * 生成按费用类别分组的预算支出数据列表
     */
    @Provide
    Arbitrary<List<BudgetExpense>> budgetExpensesByCategory() {
        String[] categories = {"劳务费", "差旅费", "交通费", "住宿费", "餐饮费"};
        
        return Arbitraries.integers().between(0, 30).flatMap(count -> {
            List<Arbitrary<BudgetExpense>> expenseArbitraries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                expenseArbitraries.add(
                        Combinators.combine(
                                Arbitraries.of(categories),
                                Arbitraries.doubles().between(100.0, 10000.0)
                        ).as((category, amount) -> createBudgetExpense(category, amount))
                );
            }
            
            if (expenseArbitraries.isEmpty()) {
                return Arbitraries.just(new ArrayList<>());
            }
            
            return Combinators.combine(expenseArbitraries).as(expenses -> expenses);
        });
    }

    /**
     * 生成带日期的预算数据列表
     */
    @Provide
    Arbitrary<List<Budget>> budgetsWithDates() {
        return Arbitraries.integers().between(0, 20).flatMap(count -> {
            List<Arbitrary<Budget>> budgetArbitraries = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            cal.set(2025, Calendar.JANUARY, 1);
            
            for (int i = 0; i < count; i++) {
                // 随机选择一个月份
                int month = new Random().nextInt(12);
                cal.set(Calendar.MONTH, month);
                Date startDate = cal.getTime();
                
                Project project = createProject(i + 1);
                budgetArbitraries.add(
                        Arbitraries.doubles().between(10000.0, 100000.0)
                                .map(amount -> createBudgetWithDate(project, amount, startDate))
                );
            }
            
            if (budgetArbitraries.isEmpty()) {
                return Arbitraries.just(new ArrayList<>());
            }
            
            return Combinators.combine(budgetArbitraries).as(budgets -> budgets);
        });
    }

    /**
     * 生成带日期的预算支出数据列表
     */
    @Provide
    Arbitrary<List<BudgetExpense>> expensesWithDates() {
        String[] categories = {"劳务费", "差旅费", "交通费"};
        
        return Arbitraries.integers().between(0, 20).flatMap(count -> {
            List<Arbitrary<BudgetExpense>> expenseArbitraries = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            cal.set(2025, Calendar.JANUARY, 1);
            
            for (int i = 0; i < count; i++) {
                // 随机选择一个月份
                int month = new Random().nextInt(12);
                cal.set(Calendar.MONTH, month);
                Date expenseDate = cal.getTime();
                
                String category = categories[new Random().nextInt(categories.length)];
                expenseArbitraries.add(
                        Arbitraries.doubles().between(100.0, 5000.0)
                                .map(amount -> createBudgetExpenseWithDate(category, amount, expenseDate))
                );
            }
            
            if (expenseArbitraries.isEmpty()) {
                return Arbitraries.just(new ArrayList<>());
            }
            
            return Combinators.combine(expenseArbitraries).as(expenses -> expenses);
        });
    }

    /**
     * 生成按项目经理分组的预算数据列表
     */
    @Provide
    Arbitrary<List<Budget>> budgetsByManager() {
        return Arbitraries.integers().between(1, 8).flatMap(managerCount -> {
            // 生成多个项目经理
            List<User> managers = new ArrayList<>();
            for (int i = 0; i < managerCount; i++) {
                managers.add(createUser(i + 1));
            }
            
            // 为每个经理生成项目和预算
            List<Budget> budgets = new ArrayList<>();
            for (User manager : managers) {
                // 每个经理管理1-3个项目
                int projectCountForManager = new Random().nextInt(3) + 1;
                for (int i = 0; i < projectCountForManager; i++) {
                    Project project = createProjectWithManager(manager, budgets.size() + i + 1);
                    // 每个项目生成1-3个预算
                    int budgetCountForProject = new Random().nextInt(3) + 1;
                    for (int j = 0; j < budgetCountForProject; j++) {
                        budgets.add(createBudget(project));
                    }
                }
            }
            
            return Arbitraries.just(budgets);
        });
    }

    // ========== 辅助方法 ==========

    private Project createProject(int id) {
        Project project = new Project();
        project.setId((long) id);
        project.setName("项目" + id);
        project.setManager(createUser(id));
        return project;
    }

    private Project createProjectWithManager(User manager, int id) {
        Project project = new Project();
        project.setId((long) id);
        project.setName("项目" + id);
        project.setManager(manager);
        return project;
    }

    private User createUser(int id) {
        User user = new User();
        user.setId((long) id);
        user.setRealName("用户" + id);
        return user;
    }

    private Budget createBudget(Project project) {
        Budget budget = new Budget();
        budget.setId(new Random().nextLong());
        budget.setProject(project);
        budget.setName("预算-" + project.getName());
        
        double totalAmount = 10000.0 + new Random().nextDouble() * 90000.0;
        double usedAmount = totalAmount * new Random().nextDouble();
        
        budget.setTotalAmount(totalAmount);
        budget.setUsedAmount(usedAmount);
        budget.setRemainingAmount(totalAmount - usedAmount);
        budget.setStatus("活跃");
        
        return budget;
    }

    private Budget createBudgetWithDate(Project project, double amount, Date startDate) {
        Budget budget = createBudget(project);
        budget.setTotalAmount(amount);
        budget.setUsedAmount(amount * new Random().nextDouble());
        budget.setStartDate(startDate);
        return budget;
    }

    private BudgetExpense createBudgetExpense(String category, double amount) {
        BudgetExpense expense = new BudgetExpense();
        expense.setId(new Random().nextLong());
        expense.setExpenseType(category);
        expense.setAmount(amount);
        return expense;
    }

    private BudgetExpense createBudgetExpenseWithDate(String category, double amount, Date expenseDate) {
        BudgetExpense expense = createBudgetExpense(category, amount);
        expense.setExpenseDate(expenseDate);
        return expense;
    }
}
