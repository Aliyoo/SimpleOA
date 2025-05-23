package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.Permission;
import com.example.simpleoa.model.Role;
import com.example.simpleoa.model.RolePermission;
import com.example.simpleoa.repository.PermissionRepository;
import com.example.simpleoa.repository.RolePermissionRepository;
import com.example.simpleoa.repository.RoleRepository;
import com.example.simpleoa.repository.UserRepository;
import com.example.simpleoa.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void deletePermission(Long id) {
        permissionRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        // First remove existing permissions for this role
        rolePermissionRepository.deleteByRoleId(roleId);

        // Then add new permissions
        List<RolePermission> rolePermissions = new ArrayList<>();
        for (Long permissionId : permissionIds) {
            RolePermission rolePermission = new RolePermission();
            // 查询角色信息
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new NoSuchElementException("Role not found with id: " + roleId));
            rolePermission.setRole(role);
            // 查询权限信息
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new NoSuchElementException("Permission not found with id: " + permissionId));
            rolePermission.setPermission(permission);
            rolePermissions.add(rolePermissionRepository.save(rolePermission));
        }
    }

    public List<Permission> findPermissionsByRoleId(Long roleId) {
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
        List<Long> permissionIds = rolePermissions.stream()
                .map(rolePermission -> rolePermission.getPermission().getId())
                .collect(Collectors.toList());

        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }

        return permissionRepository.findAllById(permissionIds);
    }

    @Transactional
    public Permission configureFunctionPermission(Long permissionId, String functionName, Boolean hasAccess) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NoSuchElementException("Permission not found with id: " + permissionId));

        Map<String, Boolean> functionPermissions = permission.getFunctionPermissions();
        if (functionPermissions == null) {
            functionPermissions = new HashMap<>();
        }

        functionPermissions.put(functionName, hasAccess);
        permission.setFunctionPermissions(functionPermissions);

        return permissionRepository.save(permission);
    }

    @Transactional
    public Permission configureDataPermission(Long permissionId, String dataType, String accessLevel) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NoSuchElementException("Permission not found with id: " + permissionId));

        Map<String, String> dataPermissions = permission.getDataPermissions();
        if (dataPermissions == null) {
            dataPermissions = new HashMap<>();
        }

        dataPermissions.put(dataType, accessLevel);
        permission.setDataPermissions(dataPermissions);

        return permissionRepository.save(permission);
    }

    @Override
    public Boolean checkUserPermission(Long userId, String permissionName) {
        // Get user's roles
        //List<Long> roleIds = userRepository.findRoleIdsByUserId(userId);
        //
        //// Get permissions for these roles
        //List<Permission> permissions = new ArrayList<>();
        //for (Long roleId : roleIds) {
        //    permissions.addAll(findPermissionsByRoleId(roleId));
        //}
        //
        //// Check if any permission matches the requested name
        //return permissions.stream()
        //        .anyMatch(p -> p.getName().equals(permissionName));
        return permissionRepository.hasPermission(userId, permissionName);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    public Permission getPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在"));
    }

    @Override
    public Permission getPermissionByName(String name) {
        Permission permission = permissionRepository.findByName(name);
        if (permission == null) {
            throw new RuntimeException("权限不存在: " + name);
        }
        return permission;
    }

    @Override
    public Permission createPermission(Permission permission) {
        return permissionRepository.save(permission);
    }

    @Override
    public Permission updatePermission(Permission permission) {
        return permissionRepository.save(permission);
    }

    @Override
    public Set<Permission> getRolePermissions(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"))
                .getPermissions();
    }

    //@Override
    //@Transactional
    //public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
    //    Role role = roleRepository.findById(roleId)
    //            .orElseThrow(() -> new RuntimeException("角色不存在"));
    //    List<Permission> permissions = permissionRepository.findAllById(permissionIds);
    //    // 转set
    //    Set<Permission> permissionSet = new HashSet<>(permissions);
    //    role.setPermissions(permissionSet);
    //    roleRepository.save(role);
    //}

    @Override
    @Transactional
    public void saveFunctionPermissionConfig(Long permissionId, String functionName, Boolean hasAccess) {
        Permission permission = getPermission(permissionId);
        if (permission.getPermissionType().equals("FUNCTIONAL")) {
            // 更新功能权限配置
            permission.setAction(functionName);
            permission.setIsActive(hasAccess);
            permissionRepository.save(permission);
        } else {
            throw new RuntimeException("该权限不是功能权限类型");
        }
    }

    @Override
    @Transactional
    public void saveDataPermissionConfig(Long permissionId, String dataType, String accessLevel) {
        Permission permission = getPermission(permissionId);
        if (permission.getPermissionType().equals("DATA")) {
            // 更新数据权限配置
            permission.setResource(dataType);
            permission.setDataScope(accessLevel);
            permissionRepository.save(permission);
        } else {
            throw new RuntimeException("该权限不是数据权限类型");
        }
    }
}
