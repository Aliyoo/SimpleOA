package com.example.simpleoa.controller;

import com.example.simpleoa.model.BudgetExpense;
import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.ProjectStatus;
import com.example.simpleoa.model.Task;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.BudgetService;
import com.example.simpleoa.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;
    private final BudgetService budgetService;

    @Autowired
    public ProjectController(ProjectService projectService, BudgetService budgetService) {
        this.projectService = projectService;
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody Project project) {
        try {
            logger.info("Creating project: {}", project.getName());
            Project savedProject = projectService.createProject(project);
            logger.info("Project created successfully: ID={}", savedProject.getId());
            return ResponseEntity.ok(savedProject);
        } catch (Exception e) {
            logger.error("Failed to create project", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create project");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public Project updateProject(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        return projectService.updateProject(project);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @PostMapping("/{projectId}/members/{userId}")
    public Project addMember(@PathVariable Long projectId, @PathVariable Long userId) {
        return projectService.addMember(projectId, userId);
    }

    @PostMapping("/{projectId}/tasks")
    public Project addTask(@PathVariable Long projectId, @RequestBody Task task) {
        return projectService.addTask(projectId, task);
    }

    @PutMapping("/{projectId}/tasks/{taskId}")
    public Project updateTask(@PathVariable Long projectId, @PathVariable Long taskId, @RequestBody Task task) {
        task.setId(taskId);
        return projectService.updateTask(projectId, task);
    }

    @PutMapping("/{projectId}/status")
    public Project updateStatus(@PathVariable Long projectId, @RequestParam String status) {
        return projectService.updateStatus(projectId, status);
    }

    @GetMapping("/{projectId}/tasks")
    public List<Task> getTasksByProject(@PathVariable Long projectId) {
        return projectService.getTasksByProject(projectId);
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public Project removeMember(@PathVariable Long projectId, @PathVariable Long userId) {
        return projectService.removeMember(projectId, userId);
    }

    @GetMapping("/{projectId}/members")
    public Set<User> getProjectMembers(@PathVariable Long projectId) {
        return projectService.getProjectMembers(projectId);
    }

    @GetMapping("/{projectId}/expenses")
    public List<BudgetExpense> getExpensesByProject(@PathVariable Long projectId) {
        return budgetService.getBudgetExpensesByProject(projectId);
    }

    @GetMapping("/{projectId}/total-hours")
    public Double getTotalHoursByProject(@PathVariable Long projectId) {
        return projectService.getTotalHoursByProject(projectId);
    }

    @GetMapping("/{projectId}/stats")
    public Map<String, Object> getDetailedStatsByProject(
            @PathVariable Long projectId,
            @RequestParam Date startDate,
            @RequestParam Date endDate) {
        return projectService.getDetailedStatsByProject(projectId, startDate, endDate);
    }

    @GetMapping("/{projectId}/hours-by-type")
    public Map<String, Double> getHoursByProjectType(
            @PathVariable Long projectId,
            @RequestParam Date startDate,
            @RequestParam Date endDate) {
        return projectService.getHoursByProjectType(projectId, startDate, endDate);
    }

    @GetMapping("/{projectId}/hours-by-status")
    public Map<ProjectStatus, Double> getHoursByProjectStatus(
            @PathVariable Long projectId,
            @RequestParam Date startDate,
            @RequestParam Date endDate) {
        return projectService.getHoursByProjectStatus(projectId, startDate, endDate);
    }

    @GetMapping("/{projectId}/hours-by-priority")
    public Map<String, Double> getHoursByProjectPriority(
            @PathVariable Long projectId,
            @RequestParam Date startDate,
            @RequestParam Date endDate) {
        return projectService.getHoursByProjectPriority(projectId, startDate, endDate);
    }

    /**
     * 获取用户参与的项目列表
     * @param userId 用户ID
     * @return 用户参与的项目列表
     */
    @GetMapping("/user/{userId}")
    public List<Project> getProjectsByUserId(@PathVariable Long userId) {
        logger.info("Getting projects for user ID: {}", userId);
        return projectService.getProjectsByUserId(userId);
    }

    /**
     * 获取项目经理管理的项目列表
     * @param managerId 项目经理ID
     * @return 项目经理管理的项目列表
     */
    @GetMapping("/manager/{managerId}")
    public List<Project> getProjectsByManagerId(@PathVariable Long managerId) {
        logger.info("Getting projects managed by manager ID: {}", managerId);
        return projectService.getProjectsByManagerId(managerId);
    }

    /**
     * 批量获取多个项目的成员列表（优化N+1查询）
     * @param requestBody 包含projectIds的请求体
     * @return Map<projectId, List<User>>
     */
    @PostMapping("/batch/members")
    public ResponseEntity<?> getBatchProjectMembers(@RequestBody Map<String, Object> requestBody) {
        try {
            Object idsObj = requestBody.get("projectIds");
            List<Long> projectIds = new java.util.ArrayList<>();
            if (idsObj instanceof List<?>) {
                for (Object o : (List<?>) idsObj) {
                    if (o == null) continue;
                    if (o instanceof Number) {
                        projectIds.add(((Number) o).longValue());
                    } else if (o instanceof String) {
                        try {
                            projectIds.add(Long.parseLong((String) o));
                        } catch (NumberFormatException nfe) {
                            logger.warn("无法解析项目ID: {}", o);
                        }
                    }
                }
            }
            
            logger.info("批量获取项目成员: projectIds={}", projectIds);
            
            Map<Long, Set<User>> result = projectService.getBatchProjectMembers(projectIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取项目成员成功");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("批量获取项目成员失败", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "批量获取项目成员失败");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}