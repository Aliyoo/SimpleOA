package com.example.simpleoa.controller;

import com.example.simpleoa.service.DashboardService;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.ProjectStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final ProjectRepository projectRepository;

    @Autowired
    public DashboardController(DashboardService dashboardService, ProjectRepository projectRepository) {
        this.dashboardService = dashboardService;
        this.projectRepository = projectRepository;
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
    
    /**
     * 调试端点：获取所有项目状态分布
     * @return 项目状态分布数据
     */
    @GetMapping("/debug/project-status")
    public ResponseEntity<Map<String, Object>> getProjectStatusDebug() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 项目总数
            long totalCount = projectRepository.count();
            result.put("totalProjects", totalCount);
            
            // 开发阶段项目数
            long developmentCount = projectRepository.countByStatus(ProjectStatus.DEVELOPMENT);
            result.put("developmentProjects", developmentCount);
            
            // 所有项目状态分布
            List<Object[]> statusCounts = projectRepository.countProjectsByStatus();
            Map<String, Long> statusDistribution = new HashMap<>();
            for (Object[] statusCount : statusCounts) {
                statusDistribution.put(statusCount[0].toString(), (Long) statusCount[1]);
            }
            result.put("statusDistribution", statusDistribution);
            
            // 获取前10个项目的详细信息
            List<Project> projects = projectRepository.findAll();
            List<Map<String, Object>> projectDetails = new java.util.ArrayList<>();
            for (int i = 0; i < Math.min(10, projects.size()); i++) {
                Project project = projects.get(i);
                Map<String, Object> projectInfo = new HashMap<>();
                projectInfo.put("id", project.getId());
                projectInfo.put("name", project.getName());
                projectInfo.put("status", project.getStatus());
                projectInfo.put("type", project.getType());
                projectDetails.add(projectInfo);
            }
            result.put("sampleProjects", projectDetails);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取项目状态调试信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get project status debug info");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
