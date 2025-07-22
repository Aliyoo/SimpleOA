package com.example.simpleoa.service;

import com.example.simpleoa.model.Holiday;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 节假日服务接口
 */
public interface HolidayService {
    
    /**
     * 获取所有节假日
     */
    List<Holiday> getAllHolidays();
    
    /**
     * 根据年份和月份获取节假日
     */
    List<Holiday> getHolidaysByMonth(int year, int month);
    
    /**
     * 根据日期范围获取节假日
     */
    List<Holiday> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据ID获取节假日
     */
    Optional<Holiday> getHolidayById(Long id);
    
    /**
     * 根据日期获取节假日
     */
    Optional<Holiday> getHolidayByDate(Date date);
    
    /**
     * 保存节假日
     */
    Holiday saveHoliday(Holiday holiday);
    
    /**
     * 删除节假日
     */
    void deleteHoliday(Long id);
    
    /**
     * 根据类型获取节假日
     */
    List<Holiday> getHolidaysByType(String type);
    
    /**
     * 根据名称搜索节假日
     */
    List<Holiday> searchHolidaysByName(String name);
    
    /**
     * 检查指定日期是否是节假日
     */
    boolean isHoliday(LocalDate date);
    
    /**
     * 根据年份获取节假日
     */
    List<Holiday> getHolidaysByYear(int year);
}
