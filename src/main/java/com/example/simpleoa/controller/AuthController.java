package com.example.simpleoa.controller;

import com.example.simpleoa.model.Permission;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.PermissionService;
import com.example.simpleoa.service.UserService;
import com.example.simpleoa.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionService permissionService;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider, PermissionService permissionService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.permissionService = permissionService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
            String token = jwtTokenProvider.generateToken(user);

            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Invalid credentials");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Logout successful");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User currentUser = (User) authentication.getPrincipal();
            return ResponseEntity.ok(currentUser);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "User not authenticated");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/permissions")
    public ResponseEntity<?> getCurrentUserPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User currentUser = (User) authentication.getPrincipal();

            // 获取用户所有角色的权限
            Set<Permission> permissions = new HashSet<>();
            if (currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()) {
                currentUser.getRoles().forEach(role -> {
                    permissions.addAll(permissionService.getRolePermissions(role.getId()));
                });
            } else {
                // 如果用户没有角色，返回基本权限
                permissions.add(permissionService.getPermissionByName("profile:view"));
                permissions.add(permissionService.getPermissionByName("profile:edit"));
                permissions.add(permissionService.getPermissionByName("dashboard:view"));
            }

            // 提取权限名称列表
            List<String> permissionNames = permissions.stream()
                .filter(Permission::getIsActive)
                .map(Permission::getName)
                .collect(Collectors.toList());

            return ResponseEntity.ok(permissionNames);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "User not authenticated");
            return ResponseEntity.badRequest().body(response);
        }
    }
}

class LoginRequest {
    private String username;
    private String password;

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
