package com.example.simpleoa.service;

import com.example.simpleoa.model.Menu;

import java.util.List;
import java.util.Map;

public interface MenuService {
    
    /**
     * 获取所有菜单
     */
    List<Menu> getAllMenus();
    
    /**
     * 获取菜单树
     */
    List<Map<String, Object>> getMenuTree();
    
    /**
     * 根据用户ID获取菜单树
     */
    List<Map<String, Object>> getMenuTreeByUserId(Long userId);
    
    /**
     * 根据角色ID获取菜单树
     */
    List<Map<String, Object>> getMenuTreeByRoleId(Long roleId);
    
    /**
     * 根据ID获取菜单
     */
    Menu getMenuById(Long id);
    
    /**
     * 创建菜单
     */
    Menu createMenu(Menu menu);
    
    /**
     * 更新菜单
     */
    Menu updateMenu(Menu menu);
    
    /**
     * 删除菜单
     */
    void deleteMenu(Long id);
    
    /**
     * 为菜单分配权限
     */
    void assignPermissionsToMenu(Long menuId, List<Long> permissionIds);
}
