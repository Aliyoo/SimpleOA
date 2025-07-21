package com.example.simpleoa.controller;

import com.example.simpleoa.model.Role;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.registerUser(user);
    }

    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userService.registerUser(user);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return userService.updateUser(user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PatchMapping("/{id}/status")
    public User updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> statusUpdate) {
        Integer enabled = statusUpdate.get("enabled");
        return userService.updateUserStatus(id, enabled);
    }

    @GetMapping("/{id}/roles")
    public List<Role> getUserRoles(@PathVariable Long id) {
        return userService.getUserRoles(id);
    }

    @PutMapping("/{id}/roles")
    public void assignRolesToUser(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRolesToUser(id, roleIds);
    }

    @GetMapping("/selectable")
    public List<User> getSelectableUsers() {
        return userService.getSelectableUsers();
    }

    @GetMapping("/is-approver")
    public boolean isApprover() {
        // In a real application, this should check the current user's roles.
        return true;
    }
}