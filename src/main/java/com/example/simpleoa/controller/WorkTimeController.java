package com.example.simpleoa.controller;

import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.ProjectStatus;
import com.example.simpleoa.model.User;
import com.example.simpleoa.model.WorkTimeRecord;
import com.example.simpleoa.service.WorkTimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
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
    public WorkTimeRecord submitWorkTime(@RequestBody WorkTimeRecord workTimeRecord) {
        return workTimeService.submitWorkTime(workTimeRecord);
    }

    @PostMapping("/batch")
    public List<WorkTimeRecord> submitBatchWorkTime(@RequestBody List<WorkTimeRecord> workTimeRecords) {
        return workTimeService.submitBatchWorkTime(workTimeRecords);
    }

    @GetMapping("/user/{userId}")
    public List<WorkTimeRecord> getWorkTimeRecordsByUser(@PathVariable Long userId) {
        User user = new User();
        user.setId(userId);
        return workTimeService.getWorkTimeRecordsByUser(user);
    }

    @GetMapping("/project/{projectId}")
    public List<WorkTimeRecord> getWorkTimeRecordsByProject(@PathVariable Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        return workTimeService.getWorkTimeRecordsByProject(project);
    }

    @GetMapping("/user/{userId}/range")
    public Object getWorkTimeRecordsByUserAndDateRange(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        User user = new User();
        user.setId(userId);

        // 如果指定了项目 ID
        Project project = null;
        if (projectId != null) {
            project = new Project();
            project.setId(projectId);
        }

        // 如果请求包含分页参数，返回分页结果
        if (page != null && size != null) {
            return workTimeService.getWorkTimeRecordsByUserAndDateRangePaged(
                    user,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    project,
                    approved,
                    page,
                    size);
        } else {
            // 否则返回全部结果
            return workTimeService.getWorkTimeRecordsByUserAndDateRangeFiltered(
                    user,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    project,
                    approved);
        }
    }

    @PutMapping("/approve/{recordId}")
    public WorkTimeRecord approveWorkTime(@PathVariable Long recordId) {
        return workTimeService.approveWorkTime(recordId);
    }

    @PutMapping("/reject/{recordId}")
    public WorkTimeRecord rejectWorkTime(@PathVariable Long recordId, @RequestBody Map<String, String> requestBody) {
        String reason = requestBody.getOrDefault("reason", "未提供原因");
        return workTimeService.rejectWorkTime(recordId, reason);
    }

    @DeleteMapping("/{recordId}")
    public void deleteWorkTime(@PathVariable Long recordId) {
        workTimeService.deleteWorkTime(recordId);
    }

    @GetMapping("/project/{projectId}/total")
    public Double getTotalHoursByProject(@PathVariable Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        return workTimeService.getTotalHoursByProject(project);
    }

    @GetMapping("/user/{userId}/total")
    public Double getTotalHoursByUser(@PathVariable Long userId) {
        User user = new User();
        user.setId(userId);
        return workTimeService.getTotalHoursByUser(user);
    }

    @GetMapping("/user/{userId}/range/total")
    public Double getTotalHoursByUserAndDateRange(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        User user = new User();
        user.setId(userId);
        return workTimeService.getTotalHoursByUserAndDateRange(
                user,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate));
    }

    @GetMapping("/project/{projectId}/stats")
    public Map<String, Object> getDetailedStatsByProject(
            @PathVariable Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            // 打印接收到的日期参数
            System.out.println("接收到的日期参数 - startDate: " + startDate + ", endDate: " + endDate);

            // 尝试解析日期
            LocalDate parsedStartDate = LocalDate.parse(startDate);
            LocalDate parsedEndDate = LocalDate.parse(endDate);

            Project project = new Project();
            project.setId(projectId);
            Map<String, Object> stats = workTimeService.getDetailedStatsByProject(
                    project,
                    parsedStartDate,
                    parsedEndDate);

            // 打印调试信息
            System.out.println("项目统计数据: " + stats);

            // 封装到统一响应结构
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "成功获取项目统计数据");
            response.put("data", stats);
            return response;
        } catch (Exception e) {
            // 记录错误信息
            System.err.println("获取项目统计数据失败: " + e.getMessage());
            e.printStackTrace();

            // 返回错误响应
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取项目统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }

    @GetMapping("/user/{userId}/stats")
    public Map<String, Object> getDetailedStatsByUser(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            // 打印接收到的日期参数
            System.out.println("接收到的日期参数 - startDate: " + startDate + ", endDate: " + endDate);

            // 尝试解析日期
            LocalDate parsedStartDate = LocalDate.parse(startDate);
            LocalDate parsedEndDate = LocalDate.parse(endDate);

            User user = new User();
            user.setId(userId);
            Map<String, Object> stats = workTimeService.getDetailedStatsByUser(
                    user,
                    parsedStartDate,
                    parsedEndDate);

            // Include overtime hours calculation
            List<WorkTimeRecord> records = workTimeService.getWorkTimeRecordsByUserAndDateRange(
                    user, parsedStartDate, parsedEndDate);
            double overtimeHours = 0.0;
            for (WorkTimeRecord record : records) {
                if (record.getHours() > 8.0) {
                    overtimeHours += (record.getHours() - 8.0);
                }
            }
            stats.put("overtimeHours", overtimeHours);

            // Calculate workload (assuming 160 hours per month as standard)
            int months = ((parsedEndDate.getYear() - parsedStartDate.getYear()) * 12) +
                    (parsedEndDate.getMonthValue() - parsedStartDate.getMonthValue()) + 1;
            double standardHours = months * 160.0;
            double totalHours = stats.get("totalHours") != null ? ((Double) stats.get("totalHours")).doubleValue() : 0.0;
            double workload = standardHours > 0 ? (totalHours / standardHours) : 0.0; // 返回小数，前端转换为百分比
            stats.put("workload", workload);

            // 打印调试信息
            System.out.println("用户统计数据: " + stats);

            // 封装到统一响应结构
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "成功获取用户统计数据");
            response.put("data", stats);
            return response;
        } catch (Exception e) {
            // 记录错误信息
            System.err.println("获取用户统计数据失败: " + e.getMessage());
            e.printStackTrace();

            // 返回错误响应
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取用户统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }

    @GetMapping("/user/{userId}/weekly-report")
    public Map<String, Object> getWeeklyReport(
            @PathVariable Long userId,
            @RequestParam String weekStartDate) {
        User user = new User();
        user.setId(userId);
        return workTimeService.generateWeeklyReport(
                user,
                LocalDate.parse(weekStartDate));
    }

    @GetMapping("/user/{userId}/monthly-report")
    public Map<String, Object> getMonthlyReport(
            @PathVariable Long userId,
            @RequestParam String monthStartDate) {
        User user = new User();
        user.setId(userId);
        return workTimeService.generateMonthlyReport(
                user,
                LocalDate.parse(monthStartDate));
    }

    @GetMapping("/project/{projectId}/type-stats")
    public Map<String, Double> getHoursByProjectType(
            @PathVariable Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Project project = new Project();
        project.setId(projectId);
        return workTimeService.getHoursByProjectType(
                project,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate));
    }

    @GetMapping("/user/{userId}/type-stats")
    public Map<String, Double> getHoursByUserAndProjectType(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        User user = new User();
        user.setId(userId);
        return workTimeService.getHoursByUserAndProjectType(
                user,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate));
    }

    @GetMapping("/project/{projectId}/status-stats")
    public Map<ProjectStatus, Double> getHoursByProjectStatus(
            @PathVariable Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Project project = new Project();
        project.setId(projectId);
        return workTimeService.getHoursByProjectStatus(
                project,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate));
    }

    @GetMapping("/user/{userId}/status-stats")
    public Map<ProjectStatus, Double> getHoursByUserAndProjectStatus(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        User user = new User();
        user.setId(userId);
        return workTimeService.getHoursByUserAndProjectStatus(
                user,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate));
    }

    @GetMapping("/project/{projectId}/priority-stats")
    public Map<String, Double> getHoursByProjectPriority(
            @PathVariable Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Project project = new Project();
        project.setId(projectId);
        return workTimeService.getHoursByProjectPriority(
                project,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate));
    }

    @GetMapping("/user/{userId}/priority-stats")
    public Map<String, Double> getHoursByUserAndProjectPriority(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        User user = new User();
        user.setId(userId);
        return workTimeService.getHoursByUserAndProjectPriority(
                user,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate));
    }

    @GetMapping("/projects/stats")
    public Map<String, Object> getAllProjectsStats(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            // 打印接收到的日期参数
            System.out.println("接收到的日期参数 - startDate: " + startDate + ", endDate: " + endDate);

            // 尝试解析日期
            LocalDate parsedStartDate = LocalDate.parse(startDate);
            LocalDate parsedEndDate = LocalDate.parse(endDate);

            Map<String, Object> stats = workTimeService.getAllProjectsStats(
                    parsedStartDate,
                    parsedEndDate);

            // 打印调试信息
            System.out.println("所有项目统计数据: " + stats);

            // 封装到统一响应结构
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "成功获取所有项目统计数据");
            response.put("data", stats);
            return response;
        } catch (Exception e) {
            // 记录错误信息
            System.err.println("获取项目统计数据失败: " + e.getMessage());
            e.printStackTrace();

            // 返回错误响应
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取项目统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }

    @GetMapping("/project/range")
    public Object getWorkTimeRecordsByProjectAndDateRange(
            @RequestParam Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Project project = new Project();
        project.setId(projectId);

        User user = null;
        if (userId != null) {
            user = new User();
            user.setId(userId);
        }

        // 如果请求包含分页参数，返回分页结果
        if (page != null && size != null) {
            return workTimeService.getWorkTimeRecordsByProjectAndDateRangePagedWithFilters(
                    project,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    approved,
                    user,
                    workType,
                    page,
                    size);
        } else {
            // 否则返回全部结果
            return workTimeService.getWorkTimeRecordsByProjectAndDateRangeFilteredWithFilters(
                    project,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    approved,
                    user,
                    workType);
        }
    }

    @PutMapping("/batch/approve")
    public Map<String, Object> batchApproveWorkTime(@RequestBody List<Long> recordIds) {
        try {
            int successCount = workTimeService.batchApproveWorkTime(recordIds);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "批量审核成功");
            response.put("data", Map.of("successCount", successCount));
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "批量审核失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }

    @PutMapping("/batch/reject")
    public Map<String, Object> batchRejectWorkTime(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> recordIds = (List<Long>) requestBody.get("recordIds");
            String reason = (String) requestBody.get("reason");
            
            int successCount = workTimeService.batchRejectWorkTime(recordIds, reason);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "批量驳回成功");
            response.put("data", Map.of("successCount", successCount));
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "批量驳回失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
}