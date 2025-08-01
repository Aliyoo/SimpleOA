package com.example.simpleoa.datafix;

import com.example.simpleoa.model.Budget;
import com.example.simpleoa.repository.BudgetExpenseRepository;
import com.example.simpleoa.repository.BudgetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 预算金额修正数据脚本
 * 
 * 用途：修正已存在预算的 usedAmount 与 remainingAmount
 * 逻辑：
 * 1. 对每个 Budget 计算 SELECT COALESCE(SUM(amount),0) FROM budget_expense WHERE budget_id = ?
 * 2. 更新对应的 usedAmount 和 remainingAmount 字段
 * 
 * 使用方法：
 * 启动时添加参数: --budget-fix.enabled=true
 * 例如: java -jar app.jar --budget-fix.enabled=true
 * 
 * 注意：
 * 1. 执行前请备份数据库
 * 2. 建议在测试环境先执行验证
 * 3. 该脚本只在指定参数时运行，避免意外执行
 */
@Component
@ConditionalOnProperty(name = "budget-fix.enabled", havingValue = "true")
@Slf4j
public class BudgetAmountFixRunner implements CommandLineRunner {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private BudgetExpenseRepository budgetExpenseRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== 开始执行预算金额修正脚本 ===");
        log.info("执行时间: {}", new Date());
        
        try {
            // 获取所有预算记录
            List<Budget> budgets = budgetRepository.findAll();
            log.info("找到 {} 个预算记录需要处理", budgets.size());
            
            int processedCount = 0;
            int updatedCount = 0;
            
            for (Budget budget : budgets) {
                processedCount++;
                
                // 计算该预算的已使用金额
                Double calculatedUsedAmount = budgetExpenseRepository.sumAmountByBudgetId(budget.getId());
                if (calculatedUsedAmount == null) {
                    calculatedUsedAmount = 0.0;
                }
                
                // 计算剩余金额
                Double calculatedRemainingAmount = budget.getTotalAmount() - calculatedUsedAmount;
                
                // 检查是否需要更新
                boolean needUpdate = false;
                
                if (budget.getUsedAmount() == null || !budget.getUsedAmount().equals(calculatedUsedAmount)) {
                    log.debug("预算 {} (ID: {}) usedAmount 需要更新: {} -> {}", 
                             budget.getName(), budget.getId(), budget.getUsedAmount(), calculatedUsedAmount);
                    needUpdate = true;
                }
                
                if (budget.getRemainingAmount() == null || !budget.getRemainingAmount().equals(calculatedRemainingAmount)) {
                    log.debug("预算 {} (ID: {}) remainingAmount 需要更新: {} -> {}", 
                             budget.getName(), budget.getId(), budget.getRemainingAmount(), calculatedRemainingAmount);
                    needUpdate = true;
                }
                
                if (needUpdate) {
                    // 更新预算金额
                    budget.setUsedAmount(calculatedUsedAmount);
                    budget.setRemainingAmount(calculatedRemainingAmount);
                    budget.setLastUpdateTime(new Date());
                    
                    budgetRepository.save(budget);
                    updatedCount++;
                    
                    log.info("已更新预算: {} (ID: {}), 总预算: {}, 已使用: {}, 剩余: {}", 
                             budget.getName(), budget.getId(), 
                             budget.getTotalAmount(), calculatedUsedAmount, calculatedRemainingAmount);
                } else {
                    log.debug("预算 {} (ID: {}) 金额正确，无需更新", budget.getName(), budget.getId());
                }
                
                // 每处理100条记录输出进度
                if (processedCount % 100 == 0) {
                    log.info("处理进度: {}/{}", processedCount, budgets.size());
                }
            }
            
            log.info("=== 预算金额修正脚本执行完成 ===");
            log.info("处理预算总数: {}", processedCount);
            log.info("更新预算数量: {}", updatedCount);
            log.info("未更新数量: {}", processedCount - updatedCount);
            
            // 输出修正前后的统计信息
            logSummaryStatistics();
            
        } catch (Exception e) {
            log.error("预算金额修正脚本执行失败", e);
            throw e;
        }
    }
    
    /**
     * 输出修正后的统计信息
     */
    private void logSummaryStatistics() {
        try {
            // 统计总预算信息
            Double totalBudgetAmount = budgetRepository.sumTotalAmount();
            Double totalUsedAmount = budgetRepository.sumUsedAmount();
            Double totalRemainingAmount = budgetRepository.sumRemainingAmount();
            
            log.info("=== 修正后统计信息 ===");
            log.info("总预算金额: {}", totalBudgetAmount != null ? totalBudgetAmount : 0.0);
            log.info("总已使用金额: {}", totalUsedAmount != null ? totalUsedAmount : 0.0);
            log.info("总剩余金额: {}", totalRemainingAmount != null ? totalRemainingAmount : 0.0);
            
            // 验证金额一致性
            if (totalBudgetAmount != null && totalUsedAmount != null && totalRemainingAmount != null) {
                Double calculatedRemaining = totalBudgetAmount - totalUsedAmount;
                if (Math.abs(calculatedRemaining - totalRemainingAmount) < 0.01) {
                    log.info("✅ 金额验证通过：总预算 - 已使用 = 剩余金额");
                } else {
                    log.warn("⚠️ 金额验证异常：计算剩余金额 {} 与数据库剩余金额 {} 不匹配", 
                             calculatedRemaining, totalRemainingAmount);
                }
            }
        } catch (Exception e) {
            log.warn("统计信息输出失败", e);
        }
    }
}
