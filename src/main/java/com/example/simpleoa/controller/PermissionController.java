package com.example.simpleoa.controller;

import com.example.simpleoa.model.Permission;
import com.example.simpleoa.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Get all permissions
     */
    @GetMapping
    public List<Permission> getAllPermissions() {
        return permissionService.getAllPermissions();
    }

    /**
     * Get permissions in tree structure
     */
    @GetMapping("/tree")
    public List<Map<String, Object>> getPermissionTree() {
        return permissionService.getPermissionTree();
    }

    /**
     * Get permission by id
     */
    @GetMapping("/{id}")
    public Permission getPermission(@PathVariable Long id) {
        return permissionService.getPermission(id);
    }

    /**
     * Create new permission
     */
    @PostMapping
    public Permission createPermission(@RequestBody Permission permission) {
        return permissionService.createPermission(permission);
    }

    /**
     * Update permission
     */
    @PutMapping("/{id}")
    public Permission updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        permission.setId(id);
        return permissionService.updatePermission(permission);
    }

    /**
     * Delete permission
     */
    @DeleteMapping("/{id}")
    public void deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
    }

    /**
     * Assign permissions to role
     */
    @PostMapping("/assign")
    public void assignPermissions(@RequestBody Map<String, Object> request) {
        Long roleId = Long.valueOf(request.get("roleId").toString());
        @SuppressWarnings("unchecked")
        List<Long> permissionIds = (List<Long>) request.get("permissionIds");
        permissionService.assignPermissionsToRole(roleId, permissionIds);
    }

    /**
     * Get permissions by role
     */
    @GetMapping("/role/{roleId}")
    public Set<Permission> getRolePermissions(@PathVariable Long roleId) {
        return permissionService.getRolePermissions(roleId);
    }

    /**
     * Configure function permissions
     */
    @PostMapping("/function-config")
    public void saveFunctionPermissionConfig(@RequestBody Map<String, Object> config) {
        Long permissionId = Long.valueOf(config.get("permissionId").toString());
        String functionName = (String) config.get("functionName");
        Boolean hasAccess = (Boolean) config.get("hasAccess");
        permissionService.saveFunctionPermissionConfig(permissionId, functionName, hasAccess);
    }

    /**
     * Configure data permissions
     */
    @PostMapping("/data-config")
    public void saveDataPermissionConfig(@RequestBody Map<String, Object> config) {
        Long permissionId = Long.valueOf(config.get("permissionId").toString());
        String dataType = (String) config.get("dataType");
        String accessLevel = (String) config.get("accessLevel");
        permissionService.saveDataPermissionConfig(permissionId, dataType, accessLevel);
    }

    /**
     * Check if user has specific permission
     */
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkPermission(
            @RequestParam Long userId,
            @RequestParam String permissionName) {
        return ResponseEntity.ok(permissionService.checkUserPermission(userId, permissionName));
    }
}
