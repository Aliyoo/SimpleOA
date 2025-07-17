package com.example.simpleoa.service;

import com.example.simpleoa.model.Permission;
import com.example.simpleoa.model.Role;

import java.util.List;
import java.util.Set;

public interface RoleService {
    List<Role> getAllRoles();
    Role createRole(Role role);
    Role createRoleWithPermissions(Role role, List<Long> permissionIds);
    Role updateRole(Role role);
    Role updateRoleWithPermissions(Role role, List<Long> permissionIds);
    void deleteRole(Long id);
    Set<Permission> getRolePermissions(Long roleId);
    void assignPermissionsToRole(Long roleId, List<Long> permissionIds);
} 