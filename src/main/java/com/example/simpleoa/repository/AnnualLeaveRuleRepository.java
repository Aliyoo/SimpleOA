package com.example.simpleoa.repository;

import com.example.simpleoa.model.AnnualLeaveRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 年假规则配置数据访问接口
 */
@Repository
public interface AnnualLeaveRuleRepository extends JpaRepository<AnnualLeaveRule, Long> {

    /**
     * 查找所有启用的规则，按优先级降序排列
     * @return 启用的规则列表
     */
    List<AnnualLeaveRule> findByIsActiveTrueOrderByPriorityDesc();

    /**
     * 查找指定工龄范围内的启用规则
     * @param seniorityYears 工龄年数
     * @return 匹配的规则列表
     */
    @Query("SELECT r FROM AnnualLeaveRule r WHERE r.isActive = true " +
           "AND r.minYears <= :seniorityYears " +
           "AND (r.maxYears IS NULL OR r.maxYears > :seniorityYears) " +
           "ORDER BY r.priority DESC")
    List<AnnualLeaveRule> findApplicableRules(@Param("seniorityYears") BigDecimal seniorityYears);

    /**
     * 查找适用于指定工龄的最佳规则（优先级最高）
     * @param seniorityYears 工龄年数
     * @return 适用的最佳规则
     */
    @Query("SELECT r FROM AnnualLeaveRule r WHERE r.isActive = true " +
           "AND r.minYears <= :seniorityYears " +
           "AND (r.maxYears IS NULL OR r.maxYears > :seniorityYears) " +
           "ORDER BY r.priority DESC LIMIT 1")
    Optional<AnnualLeaveRule> findBestApplicableRule(@Param("seniorityYears") BigDecimal seniorityYears);

    /**
     * 查找指定最小工龄的规则
     * @param minYears 最小工龄
     * @return 规则列表
     */
    List<AnnualLeaveRule> findByMinYears(BigDecimal minYears);

    /**
     * 查找指定最大工龄的规则
     * @param maxYears 最大工龄
     * @return 规则列表
     */
    List<AnnualLeaveRule> findByMaxYears(BigDecimal maxYears);

    /**
     * 查找指定优先级的规则
     * @param priority 优先级
     * @return 规则列表
     */
    List<AnnualLeaveRule> findByPriority(Integer priority);

    /**
     * 检查是否存在工龄范围重叠的规则（排除指定ID）
     * @param minYears 最小工龄
     * @param maxYears 最大工龄
     * @param excludeId 排除的规则ID
     * @return 是否存在重叠规则
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM AnnualLeaveRule r " +
           "WHERE r.id != :excludeId AND r.isActive = true " +
           "AND ((r.minYears <= :maxYears AND (r.maxYears IS NULL OR r.maxYears > :minYears)))")
    boolean hasOverlappingRules(@Param("minYears") BigDecimal minYears,
                              @Param("maxYears") BigDecimal maxYears,
                              @Param("excludeId") Long excludeId);

    /**
     * 检查是否存在工龄范围重叠的新规则
     * @param minYears 最小工龄
     * @param maxYears 最大工龄
     * @return 是否存在重叠规则
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM AnnualLeaveRule r " +
           "WHERE r.isActive = true " +
           "AND ((r.minYears <= :maxYears AND (r.maxYears IS NULL OR r.maxYears > :minYears)))")
    boolean hasOverlappingRulesForNew(@Param("minYears") BigDecimal minYears,
                                    @Param("maxYears") BigDecimal maxYears);

    /**
     * 查找所有规则，按工龄范围排序
     * @return 规则列表
     */
    @Query("SELECT r FROM AnnualLeaveRule r ORDER BY r.minYears, r.maxYears")
    List<AnnualLeaveRule> findAllOrderedByRange();

    /**
     * 查找指定年假天数的规则
     * @param annualDays 年假天数
     * @return 规则列表
     */
    List<AnnualLeaveRule> findByAnnualDays(Integer annualDays);

    /**
     * 统计启用规则的数量
     * @return 启用规则数量
     */
    @Query("SELECT COUNT(r) FROM AnnualLeaveRule r WHERE r.isActive = true")
    long countActiveRules();

    /**
     * 查找指定描述模糊匹配的规则
     * @param description 描述关键词
     * @return 规则列表
     */
    List<AnnualLeaveRule> findByDescriptionContainingIgnoreCase(String description);

    /**
     * 查找最大年假天数
     * @return 最大年假天数
     */
    @Query("SELECT MAX(r.annualDays) FROM AnnualLeaveRule r WHERE r.isActive = true")
    Integer findMaxAnnualDays();

    /**
     * 查找最小年假天数
     * @return 最小年假天数
     */
    @Query("SELECT MIN(r.annualDays) FROM AnnualLeaveRule r WHERE r.isActive = true")
    Integer findMinAnnualDays();
}