package com.example.simpleoa.service;

import com.example.simpleoa.model.Role;
import com.example.simpleoa.model.User;
import com.example.simpleoa.repository.PermissionRepository;
import com.example.simpleoa.repository.RoleRepository;
import com.example.simpleoa.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired; // Ensure Autowired is imported if not already
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder; // Import PasswordEncoder
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Changed type to PasswordEncoder and removed local initialization
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Autowired // Added Autowired for constructor injection consistency
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, // Injected PasswordEncoder
                       RoleRepository roleRepository,
                       PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder; // Use injected instance
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public User registerUser(User user) {
        // 检查用户名是否已存在
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 加密密码
        user.setPassword(this.passwordEncoder.encode(user.getPassword())); // Use this.passwordEncoder
        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return user;
    }

    public User login(String username, String password) {
        // 1. 先根据用户名查找用户
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 2. 验证密码是否匹配
        String storedEncodedPassword = user.getPassword(); // 数据库中存储的加密密码
        if (!this.passwordEncoder.matches(password, storedEncodedPassword)) { // Use this.passwordEncoder
            throw new RuntimeException("用户名或密码错误");
        }
        
        return user;
    }

    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId()).orElse(null);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    //public boolean hasPermission(Long userId, String permissionName) {
    //    User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
    //    if (user.getRole() == null) {
    //        return false;
    //    }
    //    return permissionRepository.existsByNameAndRole(permissionName, user.getRole());
    //}

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUserStatus(Long id, Integer status) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setEnabled(status);
        return userRepository.save(user);
    }

    public List<Role> getUserRoles(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        return user.getRoles();
    }

    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        List<Role> roles = roleRepository.findAllById(roleIds);
        user.setRoles(roles);
        userRepository.save(user);
    }

    public List<User> getSelectableUsers() {
        // 这里可以添加逻辑来获取可选用户，除开管理员都可以选中
        List<String> usernames = Arrays.asList("admin");
        return userRepository.findByUsernameNotIn(usernames);
    }
}