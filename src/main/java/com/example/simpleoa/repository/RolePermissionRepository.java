package com.example.simpleoa.repository;

import com.example.simpleoa.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    
    /**
     * Find role permissions by role id
     */
    @Query("SELECT rp FROM RolePermission rp JOIN rp.role r WHERE r.id = :roleId")
    List<RolePermission> findByRoleId(Long roleId);
    
    /**
     * Find role permissions by permission id
     */
    @Query("SELECT rp FROM RolePermission rp JOIN rp.permission p WHERE p.id = :permissionId")
    List<RolePermission> findByPermissionId(Long permissionId);
    
    /**
     * Delete role permissions by role id
     */
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
    
    /**
     * Delete role permissions by permission id
     */
    //@Modifying
    //@Query("DELETE FROM RolePermission rp")
    //void deleteByPermissionId(@Param("permissionId") Long permissionId);
    //
    ///**
    // * Check if role has permission
    // */
    //boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
