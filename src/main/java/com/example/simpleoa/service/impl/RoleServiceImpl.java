package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.Permission;
import com.example.simpleoa.model.Role;
import com.example.simpleoa.repository.PermissionRepository;
import com.example.simpleoa.repository.RoleRepository;
import com.example.simpleoa.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Role createRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public Role createRoleWithPermissions(Role role, List<Long> permissionIds) {
        // 先保存角色
        Role savedRole = roleRepository.save(role);
        
        // 如果有权限ID，则设置权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(permissionIds);
            Set<Permission> permissionSet = new HashSet<>(permissions);
            savedRole.setPermissions(permissionSet);
            savedRole = roleRepository.save(savedRole);
        }
        
        return savedRole;
    }

    @Override
    @Transactional
    public Role updateRoleWithPermissions(Role role, List<Long> permissionIds) {
        // 获取现有的角色，保留用户关联关系
        Role existingRole = roleRepository.findById(role.getId())
            .orElseThrow(() -> new RuntimeException("角色不存在"));
        
        // 只更新基本信息，保留用户关联
        existingRole.setName(role.getName());
        existingRole.setDescription(role.getDescription());
        
        // 设置权限（包括清空权限的情况）
        if (permissionIds != null) {
            if (permissionIds.isEmpty()) {
                // 清空权限
                existingRole.setPermissions(new HashSet<>());
            } else {
                // 设置新权限
                List<Permission> permissions = permissionRepository.findAllById(permissionIds);
                Set<Permission> permissionSet = new HashSet<>(permissions);
                existingRole.setPermissions(permissionSet);
            }
        }
        
        // 保存更新后的角色（保留用户关联）
        Role savedRole = roleRepository.save(existingRole);
        
        return savedRole;
    }

    @Override
    public Role updateRole(Role role) {
        return roleRepository.save(role);
    }

    @Override
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }

    @Override
    public Set<Permission> getRolePermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("角色不存在"));
        return role.getPermissions();
    }

    @Override
    @Transactional
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("角色不存在"));
        
        // 处理权限分配
        if (permissionIds != null) {
            if (permissionIds.isEmpty()) {
                // 清空权限
                role.setPermissions(new HashSet<>());
            } else {
                // 设置新权限
                List<Permission> permissions = permissionRepository.findAllById(permissionIds);
                Set<Permission> permissionSet = new HashSet<>(permissions);
                role.setPermissions(permissionSet);
            }
        }
        
        roleRepository.save(role);
    }
} 