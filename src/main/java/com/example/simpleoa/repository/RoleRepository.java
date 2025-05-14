package com.example.simpleoa.repository;

import com.example.simpleoa.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {
    // 按角色名称查询
    Role findByName(String name);
    
    // 查询包含特定权限的角色
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    List<Role> findByPermissionId(@Param("permissionId") Long permissionId);
    
    // 查询用户的所有角色
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId")
    List<Role> findByUserId(@Param("userId") Long userId);
    
    // 查询默认角色
    //List<Role> findByIsDefault(Boolean isDefault);
    
    // 按角色类型查询
    //List<Role> findByType(String type);
    
    // 检查角色名是否存在
    boolean existsByName(String name);
}
