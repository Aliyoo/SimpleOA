package com.example.simpleoa.service;

import com.example.simpleoa.dto.leave.SeniorityInfo;

import java.time.LocalDate;
import java.util.List;

/**
 * 工龄计算服务接口
 * 提供基于入职日期的工龄计算功能
 */
public interface SeniorityCalculationService {

    /**
     * 计算员工指定日期的工龄
     * @param userId 用户ID
     * @param calculateDate 计算基准日期
     * @return 工龄信息（精确到月）
     */
    SeniorityInfo calculateSeniority(Long userId, LocalDate calculateDate);

    /**
     * 计算员工当前的工龄
     * @param userId 用户ID
     * @return 工龄信息
     */
    SeniorityInfo calculateCurrentSeniority(Long userId);

    /**
     * 批量计算多个员工的工龄
     * @param userIds 用户ID列表
     * @param calculateDate 计算基准日期
     * @return 工龄信息列表
     */
    List<SeniorityInfo> batchCalculateSeniority(List<Long> userIds, LocalDate calculateDate);

    /**
     * 计算指定年份开始时的工龄
     * @param userId 用户ID
     * @param year 年份
     * @return 年初工龄信息
     */
    SeniorityInfo calculateYearStartSeniority(Long userId, Integer year);

    /**
     * 检查用户是否有有效的入职日期
     * @param userId 用户ID
     * @return 是否有有效入职日期
     */
    boolean hasValidHireDate(Long userId);

    /**
     * 获取员工入职日期
     * @param userId 用户ID
     * @return 入职日期
     */
    LocalDate getHireDate(Long userId);

    /**
     * 计算两个日期之间的工龄
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 工龄信息
     */
    SeniorityInfo calculateSeniorityBetweenDates(LocalDate startDate, LocalDate endDate);

    /**
     * 获取员工工龄等级
     * @param userId 用户ID
     * @param calculateDate 计算日期
     * @return 工龄等级描述
     */
    String getSeniorityLevel(Long userId, LocalDate calculateDate);

    /**
     * 检查是否为试用期员工
     * @param userId 用户ID
     * @param calculateDate 计算日期
     * @return 是否为试用期员工
     */
    boolean isProbationEmployee(Long userId, LocalDate calculateDate);

    /**
     * 检查是否为正式员工
     * @param userId 用户ID
     * @param calculateDate 计算日期
     * @return 是否为正式员工
     */
    boolean isFormalEmployee(Long userId, LocalDate calculateDate);
}