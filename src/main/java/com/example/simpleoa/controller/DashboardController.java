package com.example.simpleoa.controller;

import com.example.simpleoa.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 获取仪表盘基础统计数据
     * @return 统计数据
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取仪表盘统计数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get dashboard stats");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取工时趋势数据
     * @param days 统计天数
     * @return 工时趋势数据
     */
    @GetMapping("/worktime-trends")
    public ResponseEntity<Map<String, Object>> getWorktimeTrends(
            @RequestParam(defaultValue = "30") int days) {
        try {
            Map<String, Object> trends = dashboardService.getWorktimeTrends(days);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            logger.error("获取工时趋势数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get worktime trends");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取项目统计数据
     * @return 项目统计数据
     */
    @GetMapping("/project-stats")
    public ResponseEntity<Map<String, Object>> getProjectStats() {
        try {
            Map<String, Object> stats = dashboardService.getProjectStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取项目统计数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get project stats");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取审批统计数据
     * @return 审批统计数据
     */
    @GetMapping("/approval-stats")
    public ResponseEntity<Map<String, Object>> getApprovalStats() {
        try {
            Map<String, Object> stats = dashboardService.getApprovalStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取审批统计数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get approval stats");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
