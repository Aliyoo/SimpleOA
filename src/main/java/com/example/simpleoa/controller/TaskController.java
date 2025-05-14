package com.example.simpleoa.controller;

import com.example.simpleoa.model.Task;
import com.example.simpleoa.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create task", "message", e.getMessage()));
        }
    }

    // 修改任务
    @PostMapping("/update")
    public ResponseEntity<?> updateTask(@RequestBody Task task) {
        try {
            Task updated = taskService.updateTask(task);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update task", "message", e.getMessage()));
        }
    }

    // 删除任务
    @PostMapping("/delete")
    public ResponseEntity<?> deleteTask(@RequestBody Map<String, Long> req) {
        try {
            Long id = req.get("id");
            taskService.deleteTask(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete task", "message", e.getMessage()));
        }
    }

    // 查询单个任务
    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Task not found"));
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