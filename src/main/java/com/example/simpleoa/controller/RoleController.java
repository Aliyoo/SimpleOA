package com.example.simpleoa.controller;

import com.example.simpleoa.model.Permission;
import com.example.simpleoa.model.Role;
import com.example.simpleoa.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @PostMapping
    public Role createRole(@RequestBody Map<String, Object> requestBody) {
        String name = (String) requestBody.get("name");
        String description = (String) requestBody.get("description");
@SuppressWarnings("unchecked")
        List<Integer> permissionsAsIntegers = (List<Integer>) requestBody.get("permissions");
        List<Long> permissionIds = permissionsAsIntegers.stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());
        
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        
        return roleService.createRoleWithPermissions(role, permissionIds);
    }

    @PutMapping("/{id}")
    public Role updateRole(@PathVariable Long id, @RequestBody Map<String, Object> requestBody) {
        String name = (String) requestBody.get("name");
        String description = (String) requestBody.get("description");
@SuppressWarnings("unchecked")
        List<Integer> permissionsAsIntegers = (List<Integer>) requestBody.get("permissions");
        List<Long> permissionIds = permissionsAsIntegers.stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());
        
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setDescription(description);
        
        return roleService.updateRoleWithPermissions(role, permissionIds);
    }

    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
    }

    @GetMapping("/{id}/permissions")
    public Set<Permission> getRolePermissions(@PathVariable Long id) {
        return roleService.getRolePermissions(id);
    }

    @PutMapping("/{id}/permissions")
    public void assignPermissionsToRole(@PathVariable Long id, @RequestBody Map<String, List<Long>> requestBody) {
        List<Long> permissionIds = requestBody.get("permissionIds");
        roleService.assignPermissionsToRole(id, permissionIds);
    }
}