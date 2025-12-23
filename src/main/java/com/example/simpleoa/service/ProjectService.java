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

    @Transactional
    public Project updateProject(Project project) {
        if (project.getId() != null) {
            Project existingProject = projectRepository.findById(project.getId())
                .orElseThrow(() -> new RuntimeException("项目不存在"));

            // 验证状态转换是否合法（只有状态真正改变时才验证）
            if (project.getStatus() != null &&
                !project.getStatus().equals(existingProject.getStatus()) &&
                !isValidStatusTransition(existingProject.getStatus(), project.getStatus())) {
                throw new RuntimeException("非法的状态转换: " +
                    existingProject.getStatus() + " -> " + project.getStatus());
            }

            // 保留关键字段
            project.setCreatedAt(existingProject.getCreatedAt());
            project.setCreatedBy(existingProject.getCreatedBy());

            logger.info("Updating project {} with status transition: {} -> {}",
                project.getId(), existingProject.getStatus(), project.getStatus());
        }

        return projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("项目不存在"));

        logger.info("Attempting to delete project {} ({})", id, project.getName());

        // 检查是否有关联的预算数据
        try {
            // 检查工时记录
            Double totalHours = workTimeService.getTotalHoursByProject(project);
            if (totalHours != null && totalHours > 0) {
                logger.warn("项目 {} 已有 {} 工时记录，执行软删除", id, totalHours);
                // 软删除：标记项目为已取消状态
                project.setStatus(ProjectStatus.CANCELLED);
                projectRepository.save(project);
                return;
            }

            // 检查任务关联
            if (project.getTasks() != null && !project.getTasks().isEmpty()) {
                logger.warn("项目 {} 有 {} 个任务，执行软删除", id, project.getTasks().size());
                project.setStatus(ProjectStatus.CANCELLED);
                projectRepository.save(project);
                return;
            }

            // 检查活跃成员
            if (project.getMembers() != null && !project.getMembers().isEmpty()) {
                logger.warn("项目 {} 有 {} 个成员，执行软删除", id, project.getMembers().size());
                project.setStatus(ProjectStatus.CANCELLED);
                projectRepository.save(project);
                return;
            }

            // 只有在没有关联数据时才物理删除
            logger.info("项目 {} 没有关联数据，执行物理删除", id);
            projectRepository.delete(project);

        } catch (Exception e) {
            logger.error("删除项目 {} 时发生错误，执行软删除", id, e);
            // 发生错误时执行软删除
            project.setStatus(ProjectStatus.CANCELLED);
            projectRepository.save(project);
        }
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

    @Transactional
    public Project updateStatus(Long projectId, String status) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("项目不存在"));

        ProjectStatus newStatus = ProjectStatus.valueOf(status);

        // 验证状态转换是否合法（只有状态真正改变时才验证）
        if (!newStatus.equals(project.getStatus()) &&
            !isValidStatusTransition(project.getStatus(), newStatus)) {
            throw new RuntimeException("非法的状态转换: " +
                project.getStatus() + " -> " + newStatus);
        }

        ProjectStatus oldStatus = project.getStatus();
        project.setStatus(newStatus);

        Project savedProject = projectRepository.save(project);

        logger.info("Project {} status updated: {} -> {}",
            projectId, oldStatus, newStatus);

        return savedProject;
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
//
//     /**
//      * 获取项目成员列表
//      * @param projectId 项目ID
//      * @return 项目成员列表
//      */
//     public List<User> getProjectMembers(Long projectId) {
//         logger.info("Getting members for project ID: {}", projectId);
//         Project project = projectRepository.findById(projectId)
//                 .orElseThrow(() -> new RuntimeException("项目不存在: " + projectId));
//
//         return project.getMembers() != null ?
//                 project.getMembers().stream().toList() :
//                 new ArrayList<>();
//     }

    /**
     * 批量获取多个项目的成员列表（优化N+1查询）
     * @param projectIds 项目ID列表
     * @return Map<projectId, Set<User>>
     */
    @Transactional(readOnly = true)
    public Map<Long, Set<User>> getBatchProjectMembers(List<Long> projectIds) {
        logger.info("批量获取项目成员: projectIds={}", projectIds);
        
        if (projectIds == null || projectIds.isEmpty()) {
            return new HashMap<>();
        }
        
        // 一次性查询所有项目（包含成员关系）
        List<Project> projects = projectRepository.findAllById(projectIds);
        
        // 构建结果Map
        Map<Long, Set<User>> result = new HashMap<>();
        for (Project project : projects) {
            Set<User> members = project.getMembers();
            result.put(project.getId(), members != null ? members : new HashSet<>());
        }
        
        // 对于不存在的项目ID，返回空Set
        for (Long projectId : projectIds) {
            if (!result.containsKey(projectId)) {
                result.put(projectId, new HashSet<>());
            }
        }
        
        logger.info("批量获取项目成员完成: 共{}个项目", result.size());
        return result;
    }

    /**
     * 验证项目状态转换是否合法
     * @param currentStatus 当前状态
     * @param newStatus 新状态
     * @return 是否为合法转换
     */
    private boolean isValidStatusTransition(ProjectStatus currentStatus, ProjectStatus newStatus) {
        if (currentStatus == null) {
            // 新项目可以从任何状态开始
            return true;
        }

        // 已完成或已取消的项目不能再改变状态
        if (currentStatus == ProjectStatus.COMPLETED || currentStatus == ProjectStatus.CANCELLED) {
            return false;
        }

        // 定义合法的状态转换规则
        switch (currentStatus) {
            case PLANNING:
                return newStatus == ProjectStatus.IN_PROGRESS ||
                       newStatus == ProjectStatus.REQUIREMENT ||
                       newStatus == ProjectStatus.DESIGN ||
                       newStatus == ProjectStatus.CANCELLED;

            case IN_PROGRESS:
                return newStatus == ProjectStatus.REQUIREMENT ||
                       newStatus == ProjectStatus.DESIGN ||
                       newStatus == ProjectStatus.DEVELOPMENT ||
                       newStatus == ProjectStatus.ACCEPTANCE ||
                       newStatus == ProjectStatus.COMPLETED ||
                       newStatus == ProjectStatus.CANCELLED;

            case REQUIREMENT:
                return newStatus == ProjectStatus.DESIGN ||
                       newStatus == ProjectStatus.DEVELOPMENT ||
                       newStatus == ProjectStatus.ACCEPTANCE ||
                       newStatus == ProjectStatus.COMPLETED ||
                       newStatus == ProjectStatus.CANCELLED;

            case DESIGN:
                return newStatus == ProjectStatus.DEVELOPMENT ||
                       newStatus == ProjectStatus.ACCEPTANCE ||
                       newStatus == ProjectStatus.COMPLETED ||
                       newStatus == ProjectStatus.CANCELLED;

            case DEVELOPMENT:
                return newStatus == ProjectStatus.ACCEPTANCE ||
                       newStatus == ProjectStatus.COMPLETED ||
                       newStatus == ProjectStatus.CANCELLED;

            case ACCEPTANCE:
                return newStatus == ProjectStatus.COMPLETED ||
                       newStatus == ProjectStatus.CANCELLED;

            default:
                return false;
        }
    }
}