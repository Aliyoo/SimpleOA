package com.example.simpleoa.controller;

import com.example.simpleoa.common.ApiResponse;
import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.ProjectStatus;
import com.example.simpleoa.model.User;
import com.example.simpleoa.model.WorkTimeRecord;
import com.example.simpleoa.service.WorkTimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/worktime")
public class WorkTimeController {
    private final WorkTimeService workTimeService;

    @Autowired
    public WorkTimeController(WorkTimeService workTimeService) {
        this.workTimeService = workTimeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkTimeRecord>> submitWorkTime(@RequestBody WorkTimeRecord workTimeRecord) {
        WorkTimeRecord submittedRecord = workTimeService.submitWorkTime(workTimeRecord);
        return ResponseEntity.ok(ApiResponse.success("工时提交成功", submittedRecord));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<WorkTimeRecord>>> submitBatchWorkTime(@RequestBody List<WorkTimeRecord> workTimeRecords) {
        List<WorkTimeRecord> submittedRecords = workTimeService.submitBatchWorkTime(workTimeRecords);
        return ResponseEntity.ok(ApiResponse.success("批量工时提交成功", submittedRecords));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<WorkTimeRecord>>> getWorkTimeRecordsByUser(@PathVariable Long userId) {
        User user = new User();
        user.setId(userId);
        List<WorkTimeRecord> records = workTimeService.getWorkTimeRecordsByUser(user);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<WorkTimeRecord>>> getWorkTimeRecordsByProject(@PathVariable Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        List<WorkTimeRecord> records = workTimeService.getWorkTimeRecordsByProject(project);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/user/{userId}/range")
    public ResponseEntity<ApiResponse<?>> getWorkTimeRecordsByUserAndDateRange(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) Integer page, // Keep as Integer to check for null
            @RequestParam(required = false) Integer size) { // Keep as Integer to check for null
        User user = new User();
        user.setId(userId);

        Project projectFilter = null;
        if (projectId != null) {
            projectFilter = new Project();
            projectFilter.setId(projectId);
        }

        if (page != null && size != null) {
            Page<WorkTimeRecord> pagedRecords = workTimeService.getWorkTimeRecordsByUserAndDateRangePaged(
                    user, LocalDate.parse(startDate), LocalDate.parse(endDate), projectFilter, approved, page, size);
            return ResponseEntity.ok(ApiResponse.success(pagedRecords));
        } else {
            List<WorkTimeRecord> filteredRecords = workTimeService.getWorkTimeRecordsByUserAndDateRangeFiltered(
                    user, LocalDate.parse(startDate), LocalDate.parse(endDate), projectFilter, approved);
            return ResponseEntity.ok(ApiResponse.success(filteredRecords));
        }
    }

    @PutMapping("/approve/{recordId}")
    public ResponseEntity<ApiResponse<WorkTimeRecord>> approveWorkTime(@PathVariable Long recordId) {
        WorkTimeRecord approvedRecord = workTimeService.approveWorkTime(recordId);
        return ResponseEntity.ok(ApiResponse.success("工时已通过", approvedRecord));
    }

    @PutMapping("/reject/{recordId}")
    public ResponseEntity<ApiResponse<WorkTimeRecord>> rejectWorkTime(@PathVariable Long recordId, @RequestBody Map<String, String> requestBody) {
        String reason = requestBody.getOrDefault("reason", "未提供原因");
        WorkTimeRecord rejectedRecord = workTimeService.rejectWorkTime(recordId, reason);
        return ResponseEntity.ok(ApiResponse.success("工时已驳回", rejectedRecord));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkTime(@PathVariable Long recordId) {
        workTimeService.deleteWorkTime(recordId);
        return ResponseEntity.ok(ApiResponse.success("工时记录已删除", null));
    }

    @GetMapping("/project/{projectId}/total")
    public ResponseEntity<ApiResponse<Double>> getTotalHoursByProject(@PathVariable Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        Double totalHours = workTimeService.getTotalHoursByProject(project);
        return ResponseEntity.ok(ApiResponse.success(totalHours));
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<ApiResponse<Double>> getTotalHoursByUser(@PathVariable Long userId) {
        User user = new User();
        user.setId(userId);
        Double totalHours = workTimeService.getTotalHoursByUser(user);
        return ResponseEntity.ok(ApiResponse.success(totalHours));
    }

    @GetMapping("/user/{userId}/range/total")
    public ResponseEntity<ApiResponse<Double>> getTotalHoursByUserAndDateRange(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        User user = new User();
        user.setId(userId);
        Double totalHours = workTimeService.getTotalHoursByUserAndDateRange(
                user, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return ResponseEntity.ok(ApiResponse.success(totalHours));
    }

    @GetMapping("/project/{projectId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDetailedStatsByProject(
            @PathVariable Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate parsedStartDate = LocalDate.parse(startDate);
            LocalDate parsedEndDate = LocalDate.parse(endDate);
            Project project = new Project();
            project.setId(projectId);
            Map<String, Object> stats = workTimeService.getDetailedStatsByProject(project, parsedStartDate, parsedEndDate);
            // The System.out.println calls are removed as they are not suitable for production code
            // and logging should be handled by a proper logging framework.
            return ResponseEntity.ok(ApiResponse.success("成功获取项目统计数据", stats));
        } catch (Exception e) {
            // Proper logging should be added here, e.g., log.error("Error getting project stats for project {}", projectId, e);
            System.err.println("获取项目统计数据失败: " + e.getMessage()); // Kept for now if no logger
            e.printStackTrace(); // Kept for now
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取项目统计数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDetailedStatsByUser(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate parsedStartDate = LocalDate.parse(startDate);
            LocalDate parsedEndDate = LocalDate.parse(endDate);
            User user = new User();
            user.setId(userId);
            Map<String, Object> stats = workTimeService.getDetailedStatsByUser(user, parsedStartDate, parsedEndDate);

            // This logic for overtime and workload might be better placed in the service layer
            // or a dedicated stats calculation service for better separation of concerns.
            // For now, keeping it here as per original structure but wrapping in ApiResponse.
            List<WorkTimeRecord> records = workTimeService.getWorkTimeRecordsByUserAndDateRange(user, parsedStartDate, parsedEndDate);
            double overtimeHours = 0.0;
            for (WorkTimeRecord record : records) {
                if (record.getHours() > 8.0) { // Assuming 8 hours is standard
                    overtimeHours += (record.getHours() - 8.0);
                }
            }
            stats.put("overtimeHours", overtimeHours);

            int months = ((parsedEndDate.getYear() - parsedStartDate.getYear()) * 12) +
                         (parsedEndDate.getMonthValue() - parsedStartDate.getMonthValue()) + 1;
            double standardHours = months * 160.0; // Assuming 160 hours per month
            double totalHours = stats.get("totalHours") != null ? ((Number) stats.get("totalHours")).doubleValue() : 0.0;
            double workload = standardHours > 0 ? (totalHours / standardHours) : 0.0;
            stats.put("workload", workload);
            // System.out.println calls removed.
            return ResponseEntity.ok(ApiResponse.success("成功获取用户统计数据", stats));
        } catch (Exception e) {
            System.err.println("获取用户统计数据失败: " + e.getMessage()); // Kept for now
            e.printStackTrace(); // Kept for now
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取用户统计数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/weekly-report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeeklyReport(
            @PathVariable Long userId,
            @RequestParam String weekStartDate) {
        User user = new User();
        user.setId(userId);
        Map<String, Object> report = workTimeService.generateWeeklyReport(user, LocalDate.parse(weekStartDate));
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/user/{userId}/monthly-report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyReport(
            @PathVariable Long userId,
            @RequestParam String monthStartDate) {
        User user = new User();
        user.setId(userId);
        Map<String, Object> report = workTimeService.generateMonthlyReport(user, LocalDate.parse(monthStartDate));
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/project/{projectId}/type-stats")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getHoursByProjectType(
            @PathVariable Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Project project = new Project();
        project.setId(projectId);
        Map<String, Double> stats = workTimeService.getHoursByProjectType(project, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/user/{userId}/type-stats")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getHoursByUserAndProjectType(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        User user = new User();
        user.setId(userId);
        Map<String, Double> stats = workTimeService.getHoursByUserAndProjectType(user, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/project/{projectId}/status-stats")
    public ResponseEntity<ApiResponse<Map<ProjectStatus, Double>>> getHoursByProjectStatus(
            @PathVariable Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Project project = new Project();
        project.setId(projectId);
        Map<ProjectStatus, Double> stats = workTimeService.getHoursByProjectStatus(project, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/user/{userId}/status-stats")
    public ResponseEntity<ApiResponse<Map<ProjectStatus, Double>>> getHoursByUserAndProjectStatus(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        User user = new User();
        user.setId(userId);
        Map<ProjectStatus, Double> stats = workTimeService.getHoursByUserAndProjectStatus(user, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/project/{projectId}/priority-stats")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getHoursByProjectPriority(
            @PathVariable Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Project project = new Project();
        project.setId(projectId);
        Map<String, Double> stats = workTimeService.getHoursByProjectPriority(project, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/user/{userId}/priority-stats")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getHoursByUserAndProjectPriority(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        User user = new User();
        user.setId(userId);
        Map<String, Double> stats = workTimeService.getHoursByUserAndProjectPriority(user, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/projects/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllProjectsStats(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            Map<String, Object> stats = workTimeService.getAllProjectsStats(LocalDate.parse(startDate), LocalDate.parse(endDate));
            // System.out.println calls removed
            return ResponseEntity.ok(ApiResponse.success("成功获取所有项目统计数据", stats));
        } catch (Exception e) {
            System.err.println("获取项目统计数据失败: " + e.getMessage()); // Kept for now
            e.printStackTrace(); // Kept for now
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取所有项目统计数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/project/range")
    public ResponseEntity<ApiResponse<?>> getWorkTimeRecordsByProjectAndDateRange(
            @RequestParam Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Project project = new Project();
        project.setId(projectId);

        User userFilter = null;
        if (userId != null) {
            userFilter = new User();
            userFilter.setId(userId);
        }

        if (page != null && size != null) {
            Page<WorkTimeRecord> pagedRecords = workTimeService.getWorkTimeRecordsByProjectAndDateRangePagedWithFilters(
                    project, LocalDate.parse(startDate), LocalDate.parse(endDate), approved, userFilter, workType, page, size);
            return ResponseEntity.ok(ApiResponse.success(pagedRecords));
        } else {
            List<WorkTimeRecord> filteredRecords = workTimeService.getWorkTimeRecordsByProjectAndDateRangeFilteredWithFilters(
                    project, LocalDate.parse(startDate), LocalDate.parse(endDate), approved, userFilter, workType);
            return ResponseEntity.ok(ApiResponse.success(filteredRecords));
        }
    }

    @PutMapping("/batch/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchApproveWorkTime(@RequestBody List<Long> recordIds) {
        try {
            int successCount = workTimeService.batchApproveWorkTime(recordIds);
            // System.out.println calls removed
            return ResponseEntity.ok(ApiResponse.success("批量审核成功", Map.of("successCount", successCount)));
        } catch (Exception e) {
            System.err.println("批量审核失败: " + e.getMessage()); // Kept for now
            e.printStackTrace(); // Kept for now
            return ResponseEntity.internalServerError().body(ApiResponse.error("批量审核失败: " + e.getMessage()));
        }
    }

    @PutMapping("/batch/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchRejectWorkTime(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> recordIds = (List<Long>) requestBody.get("recordIds");
            String reason = (String) requestBody.get("reason");
            
            int successCount = workTimeService.batchRejectWorkTime(recordIds, reason);
            // System.out.println calls removed
            return ResponseEntity.ok(ApiResponse.success("批量驳回成功", Map.of("successCount", successCount)));
        } catch (Exception e) {
            System.err.println("批量驳回失败: " + e.getMessage()); // Kept for now
            e.printStackTrace(); // Kept for now
            return ResponseEntity.internalServerError().body(ApiResponse.error("批量驳回失败: " + e.getMessage()));
        }
    }
}