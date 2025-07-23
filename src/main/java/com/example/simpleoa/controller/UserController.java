package com.example.simpleoa.controller;

import com.example.simpleoa.dto.UserDTO;
import com.example.simpleoa.model.Role;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
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
    public User createUser(@RequestBody UserDTO userDTO) {
        // Convert DTO to Entity
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(userDTO.getPassword());
        user.setRealName(userDTO.getRealName());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setEnabled(userDTO.getEnabled());
        user.setDepartment(userDTO.getDepartment());
        user.setEmployeeNumber(userDTO.getEmployeeNumber());
        user.setHireDate(userDTO.getHireDate());
        
        return userService.registerUser(user);
    }

    @PostMapping("/register")
    public User registerUser(@RequestBody UserDTO userDTO) {
        // Convert DTO to Entity
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(userDTO.getPassword());
        user.setRealName(userDTO.getRealName());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setEnabled(userDTO.getEnabled());
        user.setDepartment(userDTO.getDepartment());
        user.setEmployeeNumber(userDTO.getEmployeeNumber());
        user.setHireDate(userDTO.getHireDate());
        
        return userService.registerUser(user);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        return userService.updateUser(id, userDTO);
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