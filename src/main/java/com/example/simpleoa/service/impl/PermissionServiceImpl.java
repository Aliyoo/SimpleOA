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
        // 获取现有角色，保留用户关联
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found with id: " + roleId));
        
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
        
        // 保存角色（这会自动处理role_permission关联表）
        roleRepository.save(role);
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
    public List<Map<String, Object>> getPermissionTree() {
        List<Permission> allPermissions = permissionRepository.findAll();
        
        // 按资源分组
        Map<String, List<Permission>> groupedPermissions = allPermissions.stream()
                .collect(Collectors.groupingBy(Permission::getResource));
        
        List<Map<String, Object>> tree = new ArrayList<>();
        
        // 资源名称映射
        Map<String, String> resourceNameMap = new HashMap<>();
        resourceNameMap.put("DASHBOARD", "仪表盘");
        resourceNameMap.put("WORKTIME", "工时管理");
        resourceNameMap.put("MANAGER_WORKTIME", "项目工时");
        resourceNameMap.put("LEAVE", "请假管理");
        resourceNameMap.put("TRAVEL", "出差管理");
        resourceNameMap.put("REIMBURSEMENT", "报销管理");
        resourceNameMap.put("APPROVAL", "审批管理");
        resourceNameMap.put("PROJECT", "项目管理");
        resourceNameMap.put("TASK", "任务管理");
        resourceNameMap.put("OUTSOURCING", "外包管理");
        resourceNameMap.put("PAYMENT", "付款管理");
        resourceNameMap.put("BUDGET", "预算管理");
        resourceNameMap.put("PERFORMANCE", "绩效管理");
        resourceNameMap.put("USER", "用户管理");
        resourceNameMap.put("ROLE", "角色管理");
        resourceNameMap.put("PERMISSION", "权限管理");
        resourceNameMap.put("LOG", "日志管理");
        resourceNameMap.put("NOTIFICATION", "通知管理");
        resourceNameMap.put("ANNOUNCEMENT", "公告管理");
        resourceNameMap.put("SYSTEM", "系统配置");
        resourceNameMap.put("PROFILE", "个人资料");
        
        for (Map.Entry<String, List<Permission>> entry : groupedPermissions.entrySet()) {
            String resource = entry.getKey();
            List<Permission> permissions = entry.getValue();
            
            Map<String, Object> resourceNode = new HashMap<>();
            resourceNode.put("id", "resource_" + resource);
            resourceNode.put("name", resourceNameMap.getOrDefault(resource, resource));
            resourceNode.put("isGroup", true);
            
            List<Map<String, Object>> children = new ArrayList<>();
            for (Permission permission : permissions) {
                Map<String, Object> permissionNode = new HashMap<>();
                permissionNode.put("id", permission.getId());
                permissionNode.put("name", permission.getDescription());
                permissionNode.put("permissionCode", permission.getName());
                permissionNode.put("isGroup", false);
                children.add(permissionNode);
            }
            
            resourceNode.put("children", children);
            tree.add(resourceNode);
        }
        
        return tree;
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
