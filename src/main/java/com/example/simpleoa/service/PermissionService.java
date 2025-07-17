package com.example.simpleoa.service;

import com.example.simpleoa.model.Permission;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PermissionService {
    List<Permission> getAllPermissions();
    List<Map<String, Object>> getPermissionTree();
    Permission getPermission(Long id);
    Permission getPermissionByName(String name);
    Permission createPermission(Permission permission);
    Permission updatePermission(Permission permission);
    void deletePermission(Long id);
    Set<Permission> getRolePermissions(Long roleId);
    void assignPermissionsToRole(Long roleId, List<Long> permissionIds);
    void saveFunctionPermissionConfig(Long permissionId, String functionName, Boolean hasAccess);
    void saveDataPermissionConfig(Long permissionId, String dataType, String accessLevel);

    Boolean checkUserPermission(Long userId, String permissionName);
}
