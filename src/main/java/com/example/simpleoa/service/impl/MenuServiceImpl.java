package com.example.simpleoa.service.impl;

import com.example.simpleoa.model.Menu;
import com.example.simpleoa.model.Permission;
import com.example.simpleoa.repository.MenuRepository;
import com.example.simpleoa.repository.PermissionRepository;
import com.example.simpleoa.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuRepository menuRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public List<Menu> getAllMenus() {
        return menuRepository.findAll();
    }

    @Override
    public List<Map<String, Object>> getMenuTree() {
        List<Menu> rootMenus = menuRepository.findByParentIsNullOrderBySortOrderAsc();
        return buildMenuTree(rootMenus);
    }

    @Override
    public List<Map<String, Object>> getMenuTreeByUserId(Long userId) {
        List<Menu> userMenus = menuRepository.findMenusByUserId(userId);
        // 获取所有顶级菜单
        List<Menu> allRootMenus = menuRepository.findByParentIsNullOrderBySortOrderAsc();
        // 过滤出用户有权限访问的顶级菜单或包含有权限子菜单的顶级菜单
        List<Menu> rootMenus = allRootMenus.stream()
                .filter(rootMenu -> {
                    // 如果顶级菜单本身在用户菜单中，则包含它
                    if (userMenus.contains(rootMenu)) {
                        return true;
                    }
                    // 否则检查是否有子菜单在用户菜单中
                    return userMenus.stream()
                            .anyMatch(menu -> menu.getParent() != null && menu.getParent().getId().equals(rootMenu.getId()));
                })
                .sorted(Comparator.comparing(Menu::getSortOrder))
                .collect(Collectors.toList());
        
        return buildMenuTreeWithUserPermissions(rootMenus, userMenus);
    }

    @Override
    public List<Map<String, Object>> getMenuTreeByRoleId(Long roleId) {
        List<Menu> roleMenus = menuRepository.findMenusByRoleId(roleId);
        // 过滤出顶级菜单
        List<Menu> rootMenus = roleMenus.stream()
                .filter(menu -> menu.getParent() == null)
                .sorted(Comparator.comparing(Menu::getSortOrder))
                .collect(Collectors.toList());
        
        return buildMenuTreeWithUserPermissions(rootMenus, roleMenus);
    }

    @Override
    public Menu getMenuById(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("菜单不存在"));
    }

    @Override
    @Transactional
    public Menu createMenu(Menu menu) {
        return menuRepository.save(menu);
    }

    @Override
    @Transactional
    public Menu updateMenu(Menu menu) {
        Menu existingMenu = getMenuById(menu.getId());
        
        existingMenu.setName(menu.getName());
        existingMenu.setPath(menu.getPath());
        existingMenu.setComponent(menu.getComponent());
        existingMenu.setIcon(menu.getIcon());
        existingMenu.setSortOrder(menu.getSortOrder());
        existingMenu.setIsHidden(menu.getIsHidden());
        existingMenu.setDescription(menu.getDescription());
        existingMenu.setUpdatedAt(new Date());
        
        if (menu.getParent() != null) {
            existingMenu.setParent(menu.getParent());
        }
        
        return menuRepository.save(existingMenu);
    }

    @Override
    @Transactional
    public void deleteMenu(Long id) {
        menuRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void assignPermissionsToMenu(Long menuId, List<Long> permissionIds) {
        Menu menu = getMenuById(menuId);
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        
        menu.getPermissions().clear();
        menu.getPermissions().addAll(permissions);
        
        menuRepository.save(menu);
    }
    
    /**
     * 构建菜单树
     */
    private List<Map<String, Object>> buildMenuTree(List<Menu> rootMenus) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Menu menu : rootMenus) {
            Map<String, Object> menuMap = convertMenuToMap(menu);
            
            if (!menu.getChildren().isEmpty()) {
                List<Menu> children = menu.getChildren().stream()
                        .sorted(Comparator.comparing(Menu::getSortOrder))
                        .collect(Collectors.toList());
                menuMap.put("children", buildMenuTree(children));
            }
            
            result.add(menuMap);
        }
        
        return result;
    }
    
    /**
     * 构建带用户权限的菜单树
     */
    private List<Map<String, Object>> buildMenuTreeWithUserPermissions(List<Menu> rootMenus, List<Menu> userMenus) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Menu menu : rootMenus) {
            Map<String, Object> menuMap = convertMenuToMap(menu);
            
            // 查找子菜单
            List<Menu> children = userMenus.stream()
                    .filter(m -> m.getParent() != null && m.getParent().getId().equals(menu.getId()))
                    .sorted(Comparator.comparing(Menu::getSortOrder))
                    .collect(Collectors.toList());
            
            if (!children.isEmpty()) {
                menuMap.put("children", buildMenuTreeWithUserPermissions(children, userMenus));
            }
            
            result.add(menuMap);
        }
        
        return result;
    }
    
    /**
     * 将菜单对象转换为Map
     */
    private Map<String, Object> convertMenuToMap(Menu menu) {
        Map<String, Object> menuMap = new HashMap<>();
        menuMap.put("id", menu.getId());
        menuMap.put("name", menu.getName());
        menuMap.put("path", menu.getPath());
        menuMap.put("component", menu.getComponent());
        menuMap.put("icon", menu.getIcon());
        menuMap.put("sortOrder", menu.getSortOrder());
        menuMap.put("isHidden", menu.getIsHidden());
        menuMap.put("description", menu.getDescription());
        
        // 添加权限信息
        List<Map<String, Object>> permissionList = menu.getPermissions().stream()
                .map(permission -> {
                    Map<String, Object> permMap = new HashMap<>();
                    permMap.put("id", permission.getId());
                    permMap.put("name", permission.getName());
                    permMap.put("description", permission.getDescription());
                    return permMap;
                })
                .collect(Collectors.toList());
        
        menuMap.put("permissions", permissionList);
        
        return menuMap;
    }
}
