package com.example.simpleoa.controller;

import com.example.simpleoa.model.Workday;
import com.example.simpleoa.service.WorkdayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 工作日控制器
 * 处理工作日相关的HTTP请求
 */
@RestController
@RequestMapping("/api/workdays")
@CrossOrigin(origins = "*")
public class WorkdayController {

    @Autowired
    private WorkdayService workdayService;

    /**
     * 获取所有工作日
     */
    @GetMapping
    public ResponseEntity<List<Workday>> getAllWorkdays() {
        List<Workday> workdays = workdayService.getAllWorkdays();
        return ResponseEntity.ok(workdays);
    }

    /**
     * 根据年份和月份获取工作日
     */
    @GetMapping("/by-month")
    public ResponseEntity<List<Workday>> getWorkdaysByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        List<Workday> workdays = workdayService.getWorkdaysByMonth(year, month);
        return ResponseEntity.ok(workdays);
    }

    /**
     * 根据年份获取工作日
     */
    @GetMapping("/by-year")
    public ResponseEntity<List<Workday>> getWorkdaysByYear(
            @RequestParam int year) {
        List<Workday> workdays = workdayService.getWorkdaysByYear(year);
        return ResponseEntity.ok(workdays);
    }

    /**
     * 根据日期范围获取工作日
     */
    @GetMapping("/by-range")
    public ResponseEntity<List<Workday>> getWorkdaysByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Workday> workdays = workdayService.getWorkdaysByDateRange(startDate, endDate);
        return ResponseEntity.ok(workdays);
    }

    /**
     * 根据ID获取工作日
     */
    @GetMapping("/{id}")
    public ResponseEntity<Workday> getWorkdayById(@PathVariable Long id) {
        Optional<Workday> workday = workdayService.getWorkdayById(id);
        return workday.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 根据日期获取工作日
     */
    @GetMapping("/by-date")
    public ResponseEntity<Workday> getWorkdayByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Optional<Workday> workday = workdayService.getWorkdayByDate(date);
        return workday.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 创建新的工作日
     */
    @PostMapping
    public ResponseEntity<Workday> createWorkday(@RequestBody Workday workday) {
        try {
            Workday savedWorkday = workdayService.saveWorkday(workday);
            return ResponseEntity.ok(savedWorkday);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新工作日
     */
    @PutMapping("/{id}")
    public ResponseEntity<Workday> updateWorkday(@PathVariable Long id, @RequestBody Workday workday) {
        if (!workdayService.getWorkdayById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        workday.setId(id);
        Workday updatedWorkday = workdayService.saveWorkday(workday);
        return ResponseEntity.ok(updatedWorkday);
    }

    /**
     * 删除工作日
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkday(@PathVariable Long id) {
        if (!workdayService.getWorkdayById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        workdayService.deleteWorkday(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 生成指定月份的所有工作日（根据预设规则）
     */
    @PostMapping("/generate")
    public ResponseEntity<Object> generateWorkdays(@RequestBody WorkdayRequest request) {
        try {
            List<Workday> workdays = workdayService.generateWorkdaysForMonth(
                    request.getYear(), request.getMonth(), request.getDescription());
            return ResponseEntity.ok(workdays);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("系统内部错误: " + e.getMessage());
        }
    }

    /**
     * 批量生成多个月份的工作日
     */
    @PostMapping("/generate-batch")
    public ResponseEntity<List<Workday>> generateWorkdaysBatch(
            @RequestParam int startYear,
            @RequestParam int startMonth,
            @RequestParam int endYear,
            @RequestParam int endMonth,
            @RequestParam(required = false) String description) {
        try {
            List<Workday> workdays = workdayService.generateWorkdaysForRange(
                    startYear, startMonth, endYear, endMonth, description);
            return ResponseEntity.ok(workdays);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 检查指定月份是否已存在工作日
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkWorkdayExists(
            @RequestParam int year,
            @RequestParam int month) {
        boolean exists = workdayService.workdayExistsForMonth(year, month);
        return ResponseEntity.ok(exists);
    }

    /**
     * 检查指定日期是否是工作日
     */
    @GetMapping("/is-workday")
    public ResponseEntity<Boolean> isWorkday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean isWorkday = workdayService.isWorkday(date);
        return ResponseEntity.ok(isWorkday);
    }

    /**
     * 根据工作日类型获取工作日
     */
    @GetMapping("/by-type")
    public ResponseEntity<List<Workday>> getWorkdaysByType(
            @RequestParam String workType) {
        List<Workday> workdays = workdayService.getWorkdaysByType(workType);
        return ResponseEntity.ok(workdays);
    }

    /**
     * 获取指定月份的所有工作日日期
     */
    @GetMapping("/dates-by-month")
    public ResponseEntity<List<LocalDate>> getWorkdayDatesForMonth(
            @RequestParam int year,
            @RequestParam int month) {
        List<LocalDate> dates = workdayService.getWorkdayDatesForMonth(year, month);
        return ResponseEntity.ok(dates);
    }

    /**
     * 工作日请求DTO
     */
    public static class WorkdayRequest {
        private int year;
        private int month;
        private String description;

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
