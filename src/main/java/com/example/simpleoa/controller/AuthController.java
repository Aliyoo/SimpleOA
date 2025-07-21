package com.example.simpleoa.controller;

import com.example.simpleoa.model.Permission;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.PermissionService;
import com.example.simpleoa.service.UserService;
import com.example.simpleoa.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider, PermissionService permissionService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.permissionService = permissionService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
            String token = jwtTokenProvider.generateToken(user);

            // 设置 HttpOnly Cookie
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // 在生产环境中应设置为 true，使用 HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24); // 24小时有效期

            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("token", token); // 仍然返回 token 作为标识，但实际认证通过 Cookie
            return ResponseEntity.ok().header("Set-Cookie", String.format("jwt=%s; HttpOnly; Path=/; Max-Age=%d", token, 60 * 60 * 24)).body(response);
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

    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User currentUser = (User) authentication.getPrincipal();
            // 创建用户资料响应对象，不包含敏感信息如密码
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", currentUser.getId());
            profile.put("username", currentUser.getUsername());
            profile.put("name", currentUser.getRealName());
            profile.put("email", currentUser.getEmail());
            profile.put("phone", currentUser.getPhoneNumber());
            return ResponseEntity.ok(profile);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "User not authenticated");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateCurrentUserProfile(@RequestBody ProfileUpdateRequest profileRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User currentUser = (User) authentication.getPrincipal();
            try {
                // 更新用户资料
                currentUser.setRealName(profileRequest.getName());
                currentUser.setEmail(profileRequest.getEmail());
                currentUser.setPhoneNumber(profileRequest.getPhone());
                
                User updatedUser = userService.updateUser(currentUser);
                
                // 创建安全的用户信息响应对象，避免循环引用
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", updatedUser.getId());
                userInfo.put("username", updatedUser.getUsername());
                userInfo.put("realName", updatedUser.getRealName());
                userInfo.put("email", updatedUser.getEmail());
                userInfo.put("phoneNumber", updatedUser.getPhoneNumber());
                userInfo.put("department", updatedUser.getDepartment());
                userInfo.put("employeeNumber", updatedUser.getEmployeeNumber());
                userInfo.put("enabled", updatedUser.getEnabled());
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "个人信息更新成功");
                response.put("user", userInfo);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "个人信息更新失败: " + e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "User not authenticated");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User currentUser = (User) authentication.getPrincipal();
            try {
                // 验证原密码
                if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), currentUser.getPassword())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "error");
                    response.put("message", "原密码错误");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 更新密码
                currentUser.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
                userService.updateUser(currentUser);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "密码修改成功");
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "密码修改失败: " + e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
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

class ProfileUpdateRequest {
    private String name;
    private String email;
    private String phone;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;

    // Getters and setters
    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
