package com.example.simpleoa.controller;

import com.example.simpleoa.model.Menu;
import com.example.simpleoa.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 获取所有菜单
     */
    @GetMapping
    public List<Menu> getAllMenus() {
        return menuService.getAllMenus();
    }

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    public List<Map<String, Object>> getMenuTree() {
        return menuService.getMenuTree();
    }

    /**
     * 获取当前用户的菜单树
     */
    @GetMapping("/user/tree")
    public List<Map<String, Object>> getCurrentUserMenuTree() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.example.simpleoa.model.User) {
            com.example.simpleoa.model.User user = (com.example.simpleoa.model.User) authentication.getPrincipal();

            // 如果用户没有角色，返回基本菜单
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                List<Map<String, Object>> basicMenus = new ArrayList<>();

                // 仪表盘
                Map<String, Object> dashboard = new HashMap<>();
                dashboard.put("id", 1L);
                dashboard.put("name", "仪表盘");
                dashboard.put("path", "/dashboard");
                dashboard.put("component", "Dashboard");
                dashboard.put("icon", "Odometer");
                dashboard.put("sortOrder", 1);
                dashboard.put("isHidden", false);

                // 个人资料
                Map<String, Object> profile = new HashMap<>();
                profile.put("id", 40L);
                profile.put("name", "个人资料");
                profile.put("path", "/profile");
                profile.put("component", "Profile");
                profile.put("icon", "User");
                profile.put("sortOrder", 5);
                profile.put("isHidden", false);

                basicMenus.add(dashboard);
                basicMenus.add(profile);

                return basicMenus;
            }

            List<Map<String, Object>> userMenus = menuService.getMenuTreeByUserId(user.getId());
            // 如果用户的菜单列表为空，也返回基本菜单
            if (userMenus == null || userMenus.isEmpty()) {
                List<Map<String, Object>> basicMenus = new ArrayList<>();

                // 仪表盘
                Map<String, Object> dashboard = new HashMap<>();
                dashboard.put("id", 1L);
                dashboard.put("name", "仪表盘");
                dashboard.put("path", "/dashboard");
                dashboard.put("component", "Dashboard");
                dashboard.put("icon", "Odometer");
                dashboard.put("sortOrder", 1);
                dashboard.put("isHidden", false);

                // 个人资料
                Map<String, Object> profile = new HashMap<>();
                profile.put("id", 40L);
                profile.put("name", "个人资料");
                profile.put("path", "/profile");
                profile.put("component", "Profile");
                profile.put("icon", "User");
                profile.put("sortOrder", 5);
                profile.put("isHidden", false);

                basicMenus.add(dashboard);
                basicMenus.add(profile);

                return basicMenus;
            }

            return userMenus;
        }
        return Collections.emptyList();
    }

    /**
     * 根据用户ID获取菜单树
     */
    @GetMapping("/user/{userId}/tree")
    public List<Map<String, Object>> getUserMenuTree(@PathVariable Long userId) {
        return menuService.getMenuTreeByUserId(userId);
    }

    /**
     * 根据角色ID获取菜单树
     */
    @GetMapping("/role/{roleId}/tree")
    public List<Map<String, Object>> getRoleMenuTree(@PathVariable Long roleId) {
        return menuService.getMenuTreeByRoleId(roleId);
    }

    /**
     * 根据ID获取菜单
     */
    @GetMapping("/{id}")
    public Menu getMenuById(@PathVariable Long id) {
        return menuService.getMenuById(id);
    }

    /**
     * 创建菜单
     */
    @PostMapping
    public Menu createMenu(@RequestBody Menu menu) {
        return menuService.createMenu(menu);
    }

    /**
     * 更新菜单
     */
    @PutMapping("/{id}")
    public Menu updateMenu(@PathVariable Long id, @RequestBody Menu menu) {
        menu.setId(id);
        return menuService.updateMenu(menu);
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 为菜单分配权限
     */
    @PostMapping("/{id}/permissions")
    public ResponseEntity<Void> assignPermissionsToMenu(@PathVariable Long id, @RequestBody Map<String, List<Long>> request) {
        List<Long> permissionIds = request.get("permissionIds");
        menuService.assignPermissionsToMenu(id, permissionIds);
        return ResponseEntity.ok().build();
    }
}
