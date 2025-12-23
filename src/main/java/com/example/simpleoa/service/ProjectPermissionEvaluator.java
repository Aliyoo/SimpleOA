package com.example.simpleoa.service;

import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.User;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 项目权限评估器
 * 统一项目相关的权限检查逻辑
 */
@Component
public class ProjectPermissionEvaluator {

    /**
     * 检查用户是否可以管理指定项目
     * @param user 用户
     * @param project 项目
     * @return 是否可以管理
     */
    public boolean canManageProject(User user, Project project) {
        if (user == null || project == null) {
            return false;
        }

        // 管理员和财务角色可以管理所有项目
        if (hasRole(user, "ROLE_ADMIN") || hasRole(user, "ROLE_FINANCE")) {
            return true;
        }

        // 项目经理可以管理自己的项目
        if (project.getManager() != null &&
            user.getId().equals(project.getManager().getId())) {
            return true;
        }

        // 项目创建者可以管理项目
        if (project.getCreatedBy() != null &&
            user.getId().equals(project.getCreatedBy())) {
            return true;
        }

        return false;
    }

    /**
     * 检查用户是否可以查看指定项目
     * @param user 用户
     * @param project 项目
     * @return 是否可以查看
     */
    public boolean canViewProject(User user, Project project) {
        if (user == null || project == null) {
            return false;
        }

        // 可以管理项目的人一定可以查看
        if (canManageProject(user, project)) {
            return true;
        }

        // 项目成员可以查看
        if (project.getMembers() != null &&
            project.getMembers().stream()
                .anyMatch(member -> member.getId().equals(user.getId()))) {
            return true;
        }

        return false;
    }

    /**
     * 检查用户是否可以创建项目
     * @param user 用户
     * @return 是否可以创建
     */
    public boolean canCreateProject(User user) {
        if (user == null) {
            return false;
        }

        // 管理员和项目经理可以创建项目
        return hasRole(user, "ROLE_ADMIN") ||
               hasRole(user, "ROLE_PROJECT_MANAGER") ||
               hasRole(user, "项目经理") ||
               hasAnyManagementRole(user);
    }

    /**
     * 检查用户是否可以删除指定项目
     * @param user 用户
     * @param project 项目
     * @return 是否可以删除
     */
    public boolean canDeleteProject(User user, Project project) {
        if (user == null || project == null) {
            return false;
        }

        // 只有管理员可以删除项目
        return hasRole(user, "ROLE_ADMIN");
    }

    /**
     * 检查用户是否可以管理项目预算
     * @param user 用户
     * @param project 项目
     * @return 是否可以管理预算
     */
    public boolean canManageBudget(User user, Project project) {
        if (user == null || project == null) {
            return false;
        }

        // 管理员和财务角色可以管理所有预算
        if (hasRole(user, "ROLE_ADMIN") || hasRole(user, "ROLE_FINANCE")) {
            return true;
        }

        // 项目经理可以管理自己项目的预算
        if (project.getManager() != null &&
            user.getId().equals(project.getManager().getId())) {
            return true;
        }

        return false;
    }

    /**
     * 检查用户是否可以查看项目预算
     * @param user 用户
     * @param project 项目
     * @return 是否可以查看预算
     */
    public boolean canViewBudget(User user, Project project) {
        if (user == null || project == null) {
            return false;
        }

        // 可以管理预算的人一定可以查看
        if (canManageBudget(user, project)) {
            return true;
        }

        // 项目成员可以查看预算
        return canViewProject(user, project);
    }

    /**
     * 检查用户是否可以管理项目工时
     * @param user 用户
     * @param project 项目
     * @return 是否可以管理工时
     */
    public boolean canManageWorkTime(User user, Project project) {
        if (user == null || project == null) {
            return false;
        }

        // 项目经理可以管理工时
        if (project.getManager() != null &&
            user.getId().equals(project.getManager().getId())) {
            return true;
        }

        // 管理员可以管理所有工时
        return hasRole(user, "ROLE_ADMIN");
    }

    /**
     * 检查用户是否有指定角色
     * @param user 用户
     * @param roleName 角色名称
     * @return 是否有该角色
     */
    private boolean hasRole(User user, String roleName) {
        return user.getRoles() != null &&
               user.getRoles().stream()
                   .anyMatch(role -> roleName.equals(role.getName()));
    }

    /**
     * 检查用户是否有管理角色
     * @param user 用户
     * @return 是否有管理角色
     */
    private boolean hasAnyManagementRole(User user) {
        if (user.getRoles() == null) {
            return false;
        }

        return user.getRoles().stream()
            .anyMatch(role -> {
                String roleName = role.getName();
                return roleName != null && (
                    roleName.contains("经理") ||
                    roleName.contains("管理") ||
                    roleName.contains("Manager") ||
                    roleName.contains("Admin")
                );
            });
    }

    /**
     * 获取用户可以访问的项目ID列表
     * @param user 用户
     * @param projects 项目列表
     * @return 可访问的项目ID集合
     */
    public Set<Long> getAccessibleProjectIds(User user, Iterable<Project> projects) {
        return Set.of();
    }
}