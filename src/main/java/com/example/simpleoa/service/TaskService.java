package com.example.simpleoa.service;

import com.example.simpleoa.model.Task;
import java.util.List;

public interface TaskService {
    Task createTask(Task task);
    Task updateTask(Task task);
    void deleteTask(Long id);
    Task getTaskById(Long id);
    List<Task> getAllTasks();
    List<Task> getTasksByProjectId(Long projectId);
} 