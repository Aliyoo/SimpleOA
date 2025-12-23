package com.example.simpleoa.service;

import com.example.simpleoa.model.Workday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 工作日服务接口
 */
public interface WorkdayService {

    /**
     * 获取所有工作日
     */
    List<Workday> getAllWorkdays();

    /**
     * 根据年份和月份获取工作日
     */
    List<Workday> getWorkdaysByMonth(int year, int month);

    /**
     * 根据年份获取工作日
     */
    List<Workday> getWorkdaysByYear(int year);

    /**
     * 根据日期范围获取工作日
     */
    List<Workday> getWorkdaysByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * 根据ID获取工作日
     */
    Optional<Workday> getWorkdayById(Long id);

    /**
     * 根据日期获取工作日
     */
    Optional<Workday> getWorkdayByDate(LocalDate date);

    /**
     * 保存工作日
     */
    Workday saveWorkday(Workday workday);

    /**
     * 删除工作日
     */
    void deleteWorkday(Long id);

    /**
     * 生成指定月份的所有工作日（根据预设规则）
     */
    List<Workday> generateWorkdaysForMonth(int year, int month, String description);

    /**
     * 批量生成多个月份的工作日
     */
    List<Workday> generateWorkdaysForRange(int startYear, int startMonth, int endYear, int endMonth, String description);

    /**
     * 检查指定月份是否已存在工作日
     */
    boolean workdayExistsForMonth(int year, int month);

    /**
     * 检查指定日期是否是工作日
     * 业务逻辑：
     * 1. 如果日期在holiday表中，则不是工作日
     * 2. 如果日期是周末且不在workday表中，则不是工作日
     * 3. 其他情况为工作日
     */
    boolean isWorkday(LocalDate date);

    /**
     * 检查指定日期是否是节假日
     */
    boolean isHoliday(LocalDate date);

    /**
     * 获取指定月份的所有工作日日期
     */
    List<LocalDate> getWorkdayDatesForMonth(int year, int month);

    /**
     * 根据预设规则生成指定月份的工作日期列表
     * （不包括节假日，包括调休补班日）
     */
    List<LocalDate> generateWorkdayDatesForMonth(int year, int month);

    /**
     * 将指定日期调整为工作日（如果是周末或节假日，则向前调整到最近的工作日）
     */
    LocalDate adjustToWorkday(LocalDate date);

    /**
     * 获取指定月份的最后一个工作日
     */
    LocalDate getLastWorkdayOfMonth(int year, int month);

    /**
     * 根据工作日类型获取工作日
     */
    List<Workday> getWorkdaysByType(String workType);
}
