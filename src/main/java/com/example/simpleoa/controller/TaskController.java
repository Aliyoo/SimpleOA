package com.example.simpleoa.controller;

import com.example.simpleoa.model.Task;
import com.example.simpleoa.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // 新增任务
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task task) {
        try {
            Task saved = taskService.createTask(task);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create task");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    // 修改任务
    @PostMapping("/update")
    public ResponseEntity<?> updateTask(@RequestBody Task task) {
        try {
            Task updated = taskService.updateTask(task);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update task");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    // 删除任务
    @PostMapping("/delete")
    public ResponseEntity<?> deleteTask(@RequestBody Map<String, Long> req) {
        try {
            Long id = req.get("id");
            taskService.deleteTask(id);
            Map<String, Boolean> successResponse = new HashMap<>();
            successResponse.put("success", true);
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to delete task");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    // 查询单个任务
    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        if (task == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Task not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        return ResponseEntity.ok(task);
    }

    // 查询所有任务
    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    // 查询某项目下所有任务
    @GetMapping("/project/{projectId}")
    public List<Task> getTasksByProjectId(@PathVariable Long projectId) {
        return taskService.getTasksByProjectId(projectId);
    }
} 