package com.example.simpleoa.controller;

import com.example.simpleoa.model.Holiday;
import com.example.simpleoa.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 节假日控制器
 * 处理节假日相关的HTTP请求
 */
@RestController
@RequestMapping("/api/holidays")
@CrossOrigin(origins = "*")
public class HolidayController {

    @Autowired
    private HolidayService holidayService;

    /**
     * 获取所有节假日
     */
    @GetMapping
    public ResponseEntity<List<Holiday>> getAllHolidays() {
        List<Holiday> holidays = holidayService.getAllHolidays();
        return ResponseEntity.ok(holidays);
    }

    /**
     * 根据年份和月份获取节假日
     */
    @GetMapping("/by-month")
    public ResponseEntity<List<Holiday>> getHolidaysByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        List<Holiday> holidays = holidayService.getHolidaysByMonth(year, month);
        return ResponseEntity.ok(holidays);
    }

    /**
     * 根据日期范围获取节假日
     */
    @GetMapping("/by-range")
    public ResponseEntity<List<Holiday>> getHolidaysByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Holiday> holidays = holidayService.getHolidaysByDateRange(startDate, endDate);
        return ResponseEntity.ok(holidays);
    }

    /**
     * 根据ID获取节假日
     */
    @GetMapping("/{id}")
    public ResponseEntity<Holiday> getHolidayById(@PathVariable Long id) {
        Optional<Holiday> holiday = holidayService.getHolidayById(id);
        return holiday.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 根据日期获取节假日
     */
    @GetMapping("/by-date")
    public ResponseEntity<Holiday> getHolidayByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        Optional<Holiday> holiday = holidayService.getHolidayByDate(date);
        return holiday.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 创建新的节假日
     */
    @PostMapping
    public ResponseEntity<Holiday> createHoliday(@RequestBody Holiday holiday) {
        try {
            Holiday savedHoliday = holidayService.saveHoliday(holiday);
            return ResponseEntity.ok(savedHoliday);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新节假日
     */
    @PutMapping("/{id}")
    public ResponseEntity<Holiday> updateHoliday(@PathVariable Long id, @RequestBody Holiday holiday) {
        if (!holidayService.getHolidayById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        holiday.setId(id);
        Holiday updatedHoliday = holidayService.saveHoliday(holiday);
        return ResponseEntity.ok(updatedHoliday);
    }

    /**
     * 删除节假日
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        if (!holidayService.getHolidayById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        holidayService.deleteHoliday(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 根据类型获取节假日
     */
    @GetMapping("/by-type")
    public ResponseEntity<List<Holiday>> getHolidaysByType(
            @RequestParam String type) {
        List<Holiday> holidays = holidayService.getHolidaysByType(type);
        return ResponseEntity.ok(holidays);
    }

    /**
     * 根据名称搜索节假日
     */
    @GetMapping("/search")
    public ResponseEntity<List<Holiday>> searchHolidaysByName(
            @RequestParam String name) {
        List<Holiday> holidays = holidayService.searchHolidaysByName(name);
        return ResponseEntity.ok(holidays);
    }

    /**
     * 检查指定日期是否是节假日
     */
    @GetMapping("/is-holiday")
    public ResponseEntity<Boolean> isHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean isHoliday = holidayService.isHoliday(date);
        return ResponseEntity.ok(isHoliday);
    }

    /**
     * 根据年份获取节假日
     */
    @GetMapping("/by-year")
    public ResponseEntity<List<Holiday>> getHolidaysByYear(
            @RequestParam int year) {
        List<Holiday> holidays = holidayService.getHolidaysByYear(year);
        return ResponseEntity.ok(holidays);
    }
}
