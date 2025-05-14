package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.Task;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.repository.TaskRepository;
import com.example.simpleoa.service.TaskService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public Task createTask(Task task) {
        if (task.getProject() != null && task.getProject().getId() != null) {
            Project project = projectRepository.findById(task.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            task.setProject(project);
        }
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task updateTask(Task task) {
        if (task.getId() == null) {
            throw new IllegalArgumentException("Task id is required for update");
        }
        Task existing = taskRepository.findById(task.getId())
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        // 只更新部分字段
        existing.setName(task.getName());
        existing.setDescription(task.getDescription());
        existing.setStatus(task.getStatus());
        existing.setPriority(task.getPriority());
        existing.setStartDate(task.getStartDate());
        existing.setDueDate(task.getDueDate());
        existing.setCompletedDate(task.getCompletedDate());
        existing.setAssignee(task.getAssignee());
        // 项目变更
        if (task.getProject() != null && task.getProject().getId() != null) {
            Project project = projectRepository.findById(task.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            existing.setProject(project);
        }
        return taskRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new EntityNotFoundException("Task not found");
        }
        taskRepository.deleteById(id);
    }

    @Override
    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public List<Task> getTasksByProjectId(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }
} 