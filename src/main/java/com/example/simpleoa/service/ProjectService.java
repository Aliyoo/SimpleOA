package com.example.simpleoa.service;

import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.ProjectStatus;
import com.example.simpleoa.model.Task;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.ProjectRepository;
import com.example.simpleoa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkTimeService workTimeService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, @org.springframework.context.annotation.Lazy WorkTimeService workTimeService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.workTimeService = workTimeService;
    }

    @Transactional
    public Project createProject(Project project) {
        logger.info("Creating new project: {}", project.getName());

        try {
            // 设置默认状态
            if (project.getStatus() == null) {
                project.setStatus(ProjectStatus.PLANNING);
                logger.info("Setting default status to PLANNING");
            }

            // 处理项目经理关系
            if (project.getManager() != null && project.getManager().getId() != null) {
                logger.info("Processing manager ID: {}", project.getManager().getId());
                User manager = userRepository.findById(project.getManager().getId())
                    .orElseThrow(() -> new RuntimeException("Manager not found with ID: " + project.getManager().getId()));
                project.setManager(manager);
            }

            // 处理项目成员关系
            if (project.getMembers() != null && !project.getMembers().isEmpty()) {
                Set<User> validMembers = new HashSet<>();
                for (User member : project.getMembers()) {
                    if (member != null && member.getId() != null) {
                        logger.info("Processing member ID: {}", member.getId());
                        userRepository.findById(member.getId()).ifPresent(validMembers::add);
                    }
                }
                project.setMembers(validMembers);
                logger.info("Processed {} valid members", validMembers.size());
            }

            // 保存项目
            Project savedProject = projectRepository.save(project);
            logger.info("Project successfully saved with ID: {}", savedProject.getId());
            return savedProject;
        } catch (Exception e) {
            logger.error("Failed to create project", e);
            throw e;
        }
    }

    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElse(null);
    }

    public Project addMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        project.getMembers().add(user);
        return projectRepository.save(project);
    }

    public Project removeMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        project.getMembers().remove(user);
        return projectRepository.save(project);
    }

    public Project addTask(Long projectId, Task task) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        project.getTasks().add(task);
        return projectRepository.save(project);
    }

    public Project updateTask(Long projectId, Task task) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        project.getTasks().removeIf(t -> t.getId().equals(task.getId()));
        project.getTasks().add(task);
        return projectRepository.save(project);
    }

    public Project updateStatus(Long projectId, String status) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        project.setStatus(ProjectStatus.valueOf(status));
        return projectRepository.save(project);
    }

    public List<Task> getTasksByProject(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        return new ArrayList<>(project.getTasks());
    }

    public Set<User> getProjectMembers(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        return project.getMembers();
    }

    public Double getTotalHoursByProject(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        return workTimeService.getTotalHoursByProject(project);
    }

    public Map<String, Object> getDetailedStatsByProject(Long projectId, Date startDate, Date endDate) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        LocalDate startDateL = startDate != null ? startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        LocalDate endDateL = endDate != null ? endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        return workTimeService.getDetailedStatsByProject(project, startDateL, endDateL);
    }

    public Map<String, Double> getHoursByProjectType(Long projectId, Date startDate, Date endDate) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        LocalDate startDateL = startDate != null ? startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        LocalDate endDateL = endDate != null ? endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        return workTimeService.getHoursByProjectType(project, startDateL, endDateL);
    }

    public Map<ProjectStatus, Double> getHoursByProjectStatus(Long projectId, Date startDate, Date endDate) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        LocalDate startDateL = startDate != null ? startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        LocalDate endDateL = endDate != null ? endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        return workTimeService.getHoursByProjectStatus(project, startDateL, endDateL);
    }

    public Map<String, Double> getHoursByProjectPriority(Long projectId, Date startDate, Date endDate) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        LocalDate startDateL = startDate != null ? startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        LocalDate endDateL = endDate != null ? endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        return workTimeService.getHoursByProjectPriority(project, startDateL, endDateL);
    }

    /**
     * 获取用户参与的项目列表
     * @param userId 用户ID
     * @return 用户参与的项目列表
     */
    public List<Project> getProjectsByUserId(Long userId) {
        logger.info("Getting projects for user ID: {}", userId);
        return projectRepository.findByUserId(userId);
    }

    /**
     * 获取项目经理管理的项目列表
     * @param managerId 项目经理ID
     * @return 项目经理管理的项目列表
     */
    public List<Project> getProjectsByManagerId(Long managerId) {
        logger.info("Getting projects managed by manager ID: {}", managerId);
        return projectRepository.findByManagerId(managerId);
    }
}