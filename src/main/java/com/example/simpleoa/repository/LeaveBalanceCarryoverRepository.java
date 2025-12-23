package com.example.simpleoa.repository;

import com.example.simpleoa.model.LeaveBalanceCarryover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 假期结转记录数据访问接口
 */
@Repository
public interface LeaveBalanceCarryoverRepository extends JpaRepository<LeaveBalanceCarryover, Long> {

    /**
     * 查找指定用户指定年份的结转记录
     * @param userId 用户ID
     * @param fromYear 来源年份
     * @param toYear 目标年份
     * @param leaveType 假期类型
     * @return 结转记录
     */
    Optional<LeaveBalanceCarryover> findByUserIdAndFromYearAndToYearAndLeaveType(
        Long userId, Integer fromYear, Integer toYear, String leaveType);

    /**
     * 查找指定用户指定年份的所有结转记录
     * @param userId 用户ID
     * @param toYear 目标年份
     * @return 结转记录列表
     */
    List<LeaveBalanceCarryover> findByUserIdAndToYear(Long userId, Integer toYear);

    /**
     * 查找指定用户指定年份和假期类型的结转记录
     * @param userId 用户ID
     * @param toYear 目标年份
     * @param leaveType 假期类型
     * @return 结转记录列表
     */
    List<LeaveBalanceCarryover> findByUserIdAndToYearAndLeaveType(Long userId, Integer toYear, String leaveType);

    /**
     * 查找指定年份的所有结转记录
     * @param toYear 目标年份
     * @return 结转记录列表
     */
    List<LeaveBalanceCarryover> findByToYear(Integer toYear);

    /**
     * 查找即将过期的结转记录（指定日期之前过期且未使用完）
     * @param expireDate 过期日期
     * @return 即将过期的结转记录列表
     */
    List<LeaveBalanceCarryover> findByExpireDateBeforeAndIsUsedFalse(LocalDate expireDate);

    /**
     * 查找已过期的结转记录
     * @param currentDate 当前日期
     * @return 已过期的结转记录列表
     */
    @Query("SELECT c FROM LeaveBalanceCarryover c WHERE c.expireDate < :currentDate AND c.isUsed = false")
    List<LeaveBalanceCarryover> findExpiredCarryovers(@Param("currentDate") LocalDate currentDate);

    /**
     * 查找指定用户未使用完的结转记录
     * @param userId 用户ID
     * @param leaveType 假期类型
     * @return 未使用完的结转记录列表
     */
    @Query("SELECT c FROM LeaveBalanceCarryover c WHERE c.userId = :userId " +
           "AND c.leaveType = :leaveType AND c.isUsed = false " +
           "ORDER BY c.expireDate ASC")
    List<LeaveBalanceCarryover> findUnusedCarryoverByUserAndType(
        @Param("userId") Long userId, @Param("leaveType") String leaveType);

    /**
     * 查找指定用户可用（未过期且未使用完）的结转记录
     * @param userId 用户ID
     * @param leaveType 假期类型
     * @param currentDate 当前日期
     * @return 可用的结转记录列表
     */
    @Query("SELECT c FROM LeaveBalanceCarryover c WHERE c.userId = :userId " +
           "AND c.leaveType = :leaveType AND c.isUsed = false " +
           "AND (c.expireDate IS NULL OR c.expireDate >= :currentDate) " +
           "ORDER BY c.expireDate ASC")
    List<LeaveBalanceCarryover> findAvailableCarryoverByUserAndType(
        @Param("userId") Long userId, @Param("leaveType") String leaveType, @Param("currentDate") LocalDate currentDate);

    /**
     * 统计指定用户指定年份的结转总天数
     * @param userId 用户ID
     * @param toYear 目标年份
     * @param leaveType 假期类型
     * @return 结转总天数
     */
    @Query("SELECT COALESCE(SUM(c.carryoverDays), 0) FROM LeaveBalanceCarryover c " +
           "WHERE c.userId = :userId AND c.toYear = :toYear AND c.leaveType = :leaveType")
    Double sumCarryoverDaysByUserAndYearAndType(
        @Param("userId") Long userId, @Param("toYear") Integer toYear, @Param("leaveType") String leaveType);

    /**
     * 统计指定用户指定年份的已使用结转天数
     * @param userId 用户ID
     * @param toYear 目标年份
     * @param leaveType 假期类型
     * @return 已使用结转天数
     */
    @Query("SELECT COALESCE(SUM(c.usedDays), 0) FROM LeaveBalanceCarryover c " +
           "WHERE c.userId = :userId AND c.toYear = :toYear AND c.leaveType = :leaveType")
    Double sumUsedDaysByUserAndYearAndType(
        @Param("userId") Long userId, @Param("toYear") Integer toYear, @Param("leaveType") String leaveType);

    /**
     * 查找指定年份之前的结转记录（用于历史数据清理）
     * @param year 年份
     * @return 结转记录列表
     */
    List<LeaveBalanceCarryover> findByFromYearBefore(Integer year);

    /**
     * 查找指定来源年份的结转记录
     * @param fromYear 来源年份
     * @return 结转记录列表
     */
    List<LeaveBalanceCarryover> findByFromYear(Integer fromYear);

    /**
     * 检查是否存在指定用户指定年份的结转记录
     * @param userId 用户ID
     * @param fromYear 来源年份
     * @param toYear 目标年份
     * @param leaveType 假期类型
     * @return 是否存在
     */
    boolean existsByUserIdAndFromYearAndToYearAndLeaveType(
        Long userId, Integer fromYear, Integer toYear, String leaveType);

    /**
     * 删除指定年份之前的结转记录
     * @param year 年份
     * @return 删除的记录数
     */
    @Query("DELETE FROM LeaveBalanceCarryover c WHERE c.fromYear < :year")
    int deleteCarryoversBeforeYear(@Param("year") Integer year);

    /**
     * 统计总的结转记录数
     * @return 总记录数
     */
    @Query("SELECT COUNT(c) FROM LeaveBalanceCarryover c")
    long countTotalCarryovers();

    /**
     * 统计指定年份的结转记录数
     * @param toYear 目标年份
     * @return 记录数
     */
    @Query("SELECT COUNT(c) FROM LeaveBalanceCarryover c WHERE c.toYear = :toYear")
    long countCarryoversByYear(@Param("toYear") Integer toYear);
}