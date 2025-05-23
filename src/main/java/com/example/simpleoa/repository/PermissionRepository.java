package com.example.simpleoa.repository;

import com.example.simpleoa.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    // 按权限名称查询
    Permission findByName(String name);
    
    // 按权限类型查询
    //List<Permission> findByType(String type);
    
    // 检查权限名是否存在
    boolean existsByName(String name);
    
    // 查询角色的所有权限
    // @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId")
    // List<Permission> findByRoleId(@Param("roleId") Long roleId);
    
    // 查询用户的所有权限
    @Query("SELECT DISTINCT p FROM User u JOIN u.roles r JOIN r.permissions p WHERE u.id = :userId")
    List<Permission> findByUserId(@Param("userId") Long userId);
    
    // 按模块查询权限
    //List<Permission> findByModule(String module);
    
    // 按操作类型查询权限
    //List<Permission> findByOperation(String operation);
    
    // 检查用户是否拥有特定权限
    @Query("SELECT COUNT(DISTINCT p) > 0 FROM User u JOIN u.roles r JOIN r.permissions p WHERE u.id = :userId AND p.name = :permissionName")
    boolean hasPermission(@Param("userId") Long userId, @Param("permissionName") String permissionName);
}
