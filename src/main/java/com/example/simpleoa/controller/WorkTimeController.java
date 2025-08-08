package com.example.simpleoa.controller;

import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.ProjectStatus;
import com.example.simpleoa.model.User;
import com.example.simpleoa.model.WorkTimeRecord;
import com.example.simpleoa.service.WorkTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/worktime")
public class WorkTimeController {
    private static final Logger log = LoggerFactory.getLogger(WorkTimeController.class);
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
            log.info("接收到的日期参数 - startDate: {}, endDate: {}", startDate, endDate);
            LocalDate parsedStartDate = parseDateOrBadRequest(startDate);
            LocalDate parsedEndDate = parseDateOrBadRequest(endDate);
            if (parsedStartDate == null || parsedEndDate == null) {
                return badRequest("日期格式错误，应为yyyy-MM-dd");
            }

            Project project = new Project();
            project.setId(projectId);
            Map<String, Object> stats = workTimeService.getDetailedStatsByProject(
                    project,
                    parsedStartDate,
                    parsedEndDate);

            log.debug("项目统计数据: {}", stats);

            // 封装到统一响应结构
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "成功获取项目统计数据");
            response.put("data", stats);
            return response;
        } catch (Exception e) {
            log.error("获取项目统计数据失败: {}", e.getMessage(), e);

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
            log.info("接收到的日期参数 - startDate: {}, endDate: {}", startDate, endDate);
            LocalDate parsedStartDate = parseDateOrBadRequest(startDate);
            LocalDate parsedEndDate = parseDateOrBadRequest(endDate);
            if (parsedStartDate == null || parsedEndDate == null) {
                return badRequest("日期格式错误，应为yyyy-MM-dd");
            }

            User user = new User();
            user.setId(userId);
            Map<String, Object> stats = workTimeService.getDetailedStatsByUser(
                    user,
                    parsedStartDate,
                    parsedEndDate);

            log.debug("用户统计数据: {}", stats);

            // 封装到统一响应结构
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "成功获取用户统计数据");
            response.put("data", stats);
            return response;
        } catch (Exception e) {
            log.error("获取用户统计数据失败: {}", e.getMessage(), e);

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
            log.info("接收到的日期参数 - startDate: {}, endDate: {}", startDate, endDate);
            LocalDate parsedStartDate = parseDateOrBadRequest(startDate);
            LocalDate parsedEndDate = parseDateOrBadRequest(endDate);
            if (parsedStartDate == null || parsedEndDate == null) {
                return badRequest("日期格式错误，应为yyyy-MM-dd");
            }

            Map<String, Object> stats = workTimeService.getAllProjectsStats(
                    parsedStartDate,
                    parsedEndDate);

            log.debug("所有项目统计数据: {}", stats);

            // 封装到统一响应结构
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "成功获取所有项目统计数据");
            response.put("data", stats);
            return response;
        } catch (Exception e) {
            log.error("获取项目统计数据失败: {}", e.getMessage(), e);

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

    // ========== 新增高性能API端点 ==========
    
    /**
     * 批量获取用户统计数据
     */
    @PostMapping("/users/stats")
    public Map<String, Object> getBatchUserStats(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) requestBody.get("userIds");
            String startDate = (String) requestBody.get("startDate");
            String endDate = (String) requestBody.get("endDate");

            List<Map<String, Object>> stats = workTimeService.getBatchUserStats(
                    userIds,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取用户统计数据成功");
            response.put("data", stats);
            response.put("deprecated", false);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取用户统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 批量获取项目统计数据
     */
    @PostMapping({"/projects/batch-stats", "/projects/stats"})
    public Map<String, Object> getBatchProjectStats(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> projectIds = (List<Long>) requestBody.get("projectIds");
            String startDate = (String) requestBody.get("startDate");
            String endDate = (String) requestBody.get("endDate");

            List<Map<String, Object>> stats = workTimeService.getBatchProjectStats(
                    projectIds,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取项目统计数据成功");
            response.put("data", stats);
            response.put("deprecated", true); // 建议使用 /projects/batch-stats
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取项目统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 批量获取项目-成员工时统计（一次性返回多个项目的成员工时汇总，避免前端逐个项目查询）
     */
    @PostMapping("/projects/users/stats")
    public Map<String, Object> getBatchProjectUserStats(@RequestBody Map<String, Object> requestBody) {
        try {
            Object idsObj = requestBody.get("projectIds");
            List<Long> projectIds = new ArrayList<>();
            if (idsObj instanceof List<?>) {
                for (Object o : (List<?>) idsObj) {
                    if (o == null) continue;
                    if (o instanceof Number) {
                        projectIds.add(((Number) o).longValue());
                    } else if (o instanceof String) {
                        try {
                            projectIds.add(Long.parseLong((String) o));
                        } catch (NumberFormatException nfe) {
                            // skip invalid id
                        }
                    } else {
                        // unsupported type, try toString parse
                        try {
                            projectIds.add(Long.parseLong(o.toString()));
                        } catch (NumberFormatException nfe) {
                            // skip
                        }
                    }
                }
            }

            String startDate = (String) requestBody.get("startDate");
            String endDate = (String) requestBody.get("endDate");

            List<Map<String, Object>> stats = workTimeService.getBatchProjectUserStats(
                    projectIds,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取项目成员工时统计成功");
            response.put("data", stats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取项目成员工时统计失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 获取工作类型统计
     */
    @GetMapping("/worktype/stats")
    public Map<String, Object> getWorkTypeStats(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long userId) {
        try {
            Map<String, Map<String, Object>> stats = workTimeService.getWorkTypeStats(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    projectId,
                    userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取工作类型统计成功");
            response.put("data", stats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取工作类型统计失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 获取时间序列统计数据
     */
    @GetMapping("/timeseries/stats")
    public Map<String, Object> getTimeSeriesStats(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "day") String period) {
        try {
            List<Map<String, Object>> stats = workTimeService.getTimeSeriesStats(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    period);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取时间序列统计成功");
            response.put("data", stats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取时间序列统计失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 获取工时排行榜
     */
    @GetMapping("/ranking")
    public Map<String, Object> getWorkTimeRanking(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> ranking = workTimeService.getWorkTimeRanking(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    limit);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取工时排行榜成功");
            response.put("data", ranking);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取工时排行榜失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 获取工时分布统计
     */
    @GetMapping("/distribution")
    public Map<String, Object> getHoursDistribution(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            Map<String, Long> distribution = workTimeService.getHoursDistribution(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取工时分布统计成功");
            response.put("data", distribution);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取工时分布统计失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 高性能游标分页查询
     */
    @GetMapping("/cursor")
    public Map<String, Object> getCursorPagedRecords(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> result = workTimeService.getCursorPagedRecords(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    lastId,
                    size);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取分页数据成功");
            response.put("data", result);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取分页数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 批量导出工时数据
     */
    @PostMapping("/export")
    public Map<String, Object> exportWorkTimeData(@RequestBody Map<String, Object> requestBody) {
        try {
            String startDate = (String) requestBody.get("startDate");
            String endDate = (String) requestBody.get("endDate");
            @SuppressWarnings("unchecked")
            List<Long> projectIds = (List<Long>) requestBody.get("projectIds");
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) requestBody.get("userIds");

            List<WorkTimeRecord> records = workTimeService.exportWorkTimeData(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    projectIds,
                    userIds);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "导出工时数据成功");
            response.put("data", records);
            response.put("totalCount", records.size());
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "导出工时数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 检查数据完整性
     */
    @PostMapping("/integrity/check")
    public Map<String, Object> checkDataIntegrity(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) requestBody.get("userIds");
            String startDate = (String) requestBody.get("startDate");
            String endDate = (String) requestBody.get("endDate");

            List<Map<String, Object>> issues = workTimeService.checkDataIntegrity(
                    userIds,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "数据完整性检查完成");
            response.put("data", issues);
            response.put("issueCount", issues.size());
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "数据完整性检查失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 获取项目批量工时数据（专为批量填写页面优化）
     */
    @PostMapping("/projects/batch")
    public Map<String, Object> getBatchWorkTimeByProjects(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> projectIds = (List<Long>) requestBody.get("projectIds");
            String startDate = (String) requestBody.get("startDate");
            String endDate = (String) requestBody.get("endDate");

            Map<Long, List<WorkTimeRecord>> batchData = workTimeService.getBatchWorkTimeByProjects(
                    projectIds,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取批量工时数据成功");
            response.put("data", batchData);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取批量工时数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    // ========== 统计报表性能优化API端点（解决N+1查询问题）==========
    
    /**
     * 综合统计数据 API - 一次获取所有基础统计数据，解决N+1查询问题
     * 替代原有的多次API调用
     */
    @GetMapping("/comprehensive/stats")
    public Map<String, Object> getComprehensiveStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            Map<String, Object> stats = workTimeService.getComprehensiveStatistics(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取综合统计数据成功");
            response.put("data", stats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取综合统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 所有项目聚合统计 API - 替代原有getAllProjectsStats()，解决N+1查询问题
     */
    @GetMapping("/projects/aggregated/stats")
    public Map<String, Object> getAllProjectsAggregatedStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            List<Map<String, Object>> projectStats = workTimeService.getAllProjectsAggregatedStatistics(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取所有项目聚合统计数据成功");
            response.put("data", projectStats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取项目聚合统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 所有用户聚合统计 API - 解决逐个用户查询的N+1问题
     */
    @GetMapping("/users/aggregated/stats")
    public Map<String, Object> getAllUsersAggregatedStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            List<Map<String, Object>> userStats = workTimeService.getAllUsersAggregatedStatistics(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取所有用户聚合统计数据成功");
            response.put("data", userStats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取用户聚合统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 项目用户组合统计 API - 一次性获取所有项目的用户工时分布
     */
    @GetMapping("/project-user-combo/stats")
    public Map<String, Object> getProjectUserComboStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            List<Map<String, Object>> comboStats = workTimeService.getProjectUserComboStatistics(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取项目用户组合统计数据成功");
            response.put("data", comboStats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取项目用户组合统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 日期维度趋势统计 API - 支持时间趋势分析
     */
    @GetMapping("/daily-trend/stats")
    public Map<String, Object> getDailyTrendStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            List<Map<String, Object>> trendStats = workTimeService.getDailyTrendStatistics(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取日期维度趋势统计成功");
            response.put("data", trendStats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取日期趋势统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 部门维度聚合统计 API - 支持部门间对比分析
     */
    @GetMapping("/departments/stats")
    public Map<String, Object> getDepartmentAggregatedStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            List<Map<String, Object>> deptStats = workTimeService.getDepartmentAggregatedStatistics(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取部门统计数据成功");
            response.put("data", deptStats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取部门统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 工作类型聚合统计 API - 增强版本
     */
    @GetMapping("/work-types/aggregated/stats")
    public Map<String, Object> getWorkTypeAggregatedStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            List<Map<String, Object>> workTypeStats = workTimeService.getWorkTypeAggregatedStatistics(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取工作类型统计数据成功");
            response.put("data", workTypeStats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取工作类型统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 月度趋势聚合统计 API - 支持长期趋势分析
     */
    @GetMapping("/monthly-trend/stats")
    public Map<String, Object> getMonthlyTrendStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            List<Map<String, Object>> monthlyStats = workTimeService.getMonthlyTrendStatistics(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取月度趋势统计成功");
            response.put("data", monthlyStats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取月度趋势统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 审批状态聚合统计 API - 支持审批工作台统计分析
     */
    @GetMapping("/approval-status/stats")
    public Map<String, Object> getApprovalStatusAggregatedStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            List<Map<String, Object>> approvalStats = workTimeService.getApprovalStatusAggregatedStatistics(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取审批状态统计数据成功");
            response.put("data", approvalStats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取审批状态统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 项目状态聚合统计 API - 支持项目状态分析
     */
    @GetMapping("/project-status/stats")
    public Map<String, Object> getProjectStatusAggregatedStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            List<Map<String, Object>> statusStats = workTimeService.getProjectStatusAggregatedStatistics(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate));

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取项目状态统计数据成功");
            response.put("data", statusStats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取项目状态统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    
    /**
     * 统一的综合统计 API - 一次调用获取所有统计数据，解决N+1查询问题
     * 该API将替代原有的fetchStatisticsData()方法中的多次API调用
     */
    @GetMapping("/unified/stats")
    public Map<String, Object> getUnifiedStatisticsData(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long userId) {
        try {
            Map<String, Object> unifiedStats = workTimeService.getUnifiedStatisticsData(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    projectId,
                    userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取统一综合统计数据成功");
            response.put("data", unifiedStats);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取统一综合统计数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return errorResponse;
        }
    }
    // ==================== 辅助方法 ====================
    private LocalDate parseDateOrBadRequest(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Map<String, Object> badRequest(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", 400);
        errorResponse.put("message", message);
        errorResponse.put("data", null);
        return errorResponse;
    }
}
