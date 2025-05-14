package com.example.simpleoa.repository;

import com.example.simpleoa.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    
    // 查找顶级菜单（parent_id为null的菜单）
    List<Menu> findByParentIsNullOrderBySortOrderAsc();
    
    // 根据父菜单ID查找子菜单
    List<Menu> findByParentIdOrderBySortOrderAsc(Long parentId);
    
    // 根据路径查找菜单
    Menu findByPath(String path);
    
    // 根据用户ID查找有权限访问的菜单
    @Query("SELECT DISTINCT m FROM Menu m JOIN m.permissions p JOIN p.roles r JOIN r.users u WHERE u.id = :userId ORDER BY m.sortOrder ASC")
    List<Menu> findMenusByUserId(@Param("userId") Long userId);
    
    // 根据角色ID查找有权限访问的菜单
    @Query("SELECT DISTINCT m FROM Menu m JOIN m.permissions p JOIN p.roles r WHERE r.id = :roleId ORDER BY m.sortOrder ASC")
    List<Menu> findMenusByRoleId(@Param("roleId") Long roleId);
}
