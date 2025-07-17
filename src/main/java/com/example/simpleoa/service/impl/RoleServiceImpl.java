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
        // 先更新角色基本信息
        Role savedRole = roleRepository.save(role);
        
        // 设置权限（包括清空权限的情况）
        if (permissionIds != null) {
            if (permissionIds.isEmpty()) {
                // 清空权限
                savedRole.setPermissions(new HashSet<>());
            } else {
                // 设置新权限
                List<Permission> permissions = permissionRepository.findAllById(permissionIds);
                Set<Permission> permissionSet = new HashSet<>(permissions);
                savedRole.setPermissions(permissionSet);
            }
            savedRole = roleRepository.save(savedRole);
        }
        
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
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        Set<Permission> existingPermissions = new HashSet<>(permissions);
        role.setPermissions(existingPermissions);
        roleRepository.save(role);
    }
} 