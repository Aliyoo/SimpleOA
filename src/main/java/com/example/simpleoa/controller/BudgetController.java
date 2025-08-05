package com.example.simpleoa.controller;

import com.example.simpleoa.dto.BudgetRequestDTO;
import com.example.simpleoa.dto.BudgetExpenseRequestDTO;
import com.example.simpleoa.dto.BudgetSearchDTO;
import com.example.simpleoa.dto.PagedResponse;
import com.example.simpleoa.model.Budget;
import com.example.simpleoa.model.BudgetAlert;
import com.example.simpleoa.model.BudgetExpense;
import com.example.simpleoa.model.BudgetItem;
import com.example.simpleoa.model.Project;
import com.example.simpleoa.model.User;
import com.example.simpleoa.service.BudgetService;
import com.example.simpleoa.service.ProjectService;
import com.example.simpleoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;
    private final ProjectService projectService;
    private final UserService userService;

    @Autowired
    public BudgetController(BudgetService budgetService, ProjectService projectService, UserService userService) {
        this.budgetService = budgetService;
        this.projectService = projectService;
        this.userService = userService;
    }

    // 预算基本管理
    @PostMapping
    @PreAuthorize("hasAnyAuthority('budget:edit:all') or hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    public Budget createBudget(@RequestBody BudgetRequestDTO budgetRequestDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        boolean hasCreateAllPermission = auth.getAuthorities().contains(new SimpleGrantedAuthority("budget:edit:all")) ||
                                        auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ||
                                        auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_FINANCE"));
        
        // 获取当前用户实体以检查角色
        User currentUserForCreate = (User) userService.loadUserByUsername(username);
        boolean isManagerForCreate = currentUserForCreate.getRoles() != null && currentUserForCreate.getRoles().stream()
                .anyMatch(role -> {
                    String roleName = role.getName();
                    return "ROLE_MANAGER".equals(roleName) || 
                           "MANAGER".equals(roleName) || 
                           "项目经理".equals(roleName);
                });
        
        if (!hasCreateAllPermission && isManagerForCreate) {
            if (currentUserForCreate != null && budgetRequestDTO.getProjectId() != null) {
                Project project = projectService.getProjectById(budgetRequestDTO.getProjectId());
                if (project == null || !currentUserForCreate.getId().equals(project.getManager().getId())) {
                    throw new RuntimeException("您只能为自己管理的项目创建预算");
                }
            } else {
                throw new RuntimeException("必须为项目创建预算，且您只能为自己管理的项目创建预算");
            }
        } else if (!hasCreateAllPermission) {
            throw new RuntimeException("您没有权限创建预算");
        }
        
        // 将DTO转换为Budget实体
        Budget budget = new Budget();
        budget.setName(budgetRequestDTO.getName());
        budget.setTotalAmount(budgetRequestDTO.getTotalAmount());
        budget.setUsedAmount(budgetRequestDTO.getUsedAmount());
        budget.setRemainingAmount(budgetRequestDTO.getRemainingAmount());
        budget.setStartDate(budgetRequestDTO.getStartDate());
        budget.setEndDate(budgetRequestDTO.getEndDate());
        budget.setStatus(budgetRequestDTO.getStatus());
        budget.setDescription(budgetRequestDTO.getDescription());
        budget.setCreateTime(budgetRequestDTO.getCreateTime());
        budget.setLastUpdateTime(budgetRequestDTO.getLastUpdateTime());
        
        // 设置项目关联
        if (budgetRequestDTO.getProjectId() != null) {
            Project project = new Project();
            project.setId(budgetRequestDTO.getProjectId());
            budget.setProject(project);
        }
        
        return budgetService.createBudget(budget);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('budget:edit:all') or hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    public Budget updateBudget(@PathVariable Long id, @RequestBody Budget budget) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        Budget existingBudget = budgetService.getBudgetById(id);
        if (existingBudget == null) {
            throw new RuntimeException("预算不存在");
        }
        
        boolean hasEditAllPermission = auth.getAuthorities().contains(new SimpleGrantedAuthority("budget:edit:all")) ||
                                      auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ||
                                      auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_FINANCE"));
        
        // 获取当前用户实体以检查角色
        User currentUserForEdit = (User) userService.loadUserByUsername(username);
        boolean isManagerForEdit = currentUserForEdit.getRoles() != null && currentUserForEdit.getRoles().stream()
                .anyMatch(role -> {
                    String roleName = role.getName();
                    return "ROLE_MANAGER".equals(roleName) || 
                           "MANAGER".equals(roleName) || 
                           "项目经理".equals(roleName);
                });
        
        if (!hasEditAllPermission && isManagerForEdit) {
            if (currentUserForEdit != null) {
                if (existingBudget.getProject() != null) {
                    Project project = projectService.getProjectById(existingBudget.getProject().getId());
                    if (project == null || !currentUserForEdit.getId().equals(project.getManager().getId())) {
                        throw new RuntimeException("您只能修改自己管理项目的预算");
                    }
                } else {
                    throw new RuntimeException("该预算未关联项目，您无权修改");
                }
            }
        } else if (!hasEditAllPermission) {
            throw new RuntimeException("您没有权限修改预算");
        }
        
        budget.setId(id);
        return budgetService.updateBudget(budget);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('budget:delete:all') or hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    public void deleteBudget(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        Budget existingBudget = budgetService.getBudgetById(id);
        if (existingBudget == null) {
            throw new RuntimeException("预算不存在");
        }
        
        boolean hasDeleteAllPermission = auth.getAuthorities().contains(new SimpleGrantedAuthority("budget:delete:all")) ||
                                        auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ||
                                        auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_FINANCE"));
        
        // 获取当前用户实体以检查角色
        User currentUserForDelete = (User) userService.loadUserByUsername(username);
        boolean isManagerForDelete = currentUserForDelete.getRoles() != null && currentUserForDelete.getRoles().stream()
                .anyMatch(role -> {
                    String roleName = role.getName();
                    return "ROLE_MANAGER".equals(roleName) || 
                           "MANAGER".equals(roleName) || 
                           "项目经理".equals(roleName);
                });
        
        if (!hasDeleteAllPermission && isManagerForDelete) {
            if (currentUserForDelete != null) {
                if (existingBudget.getProject() != null) {
                    Project project = projectService.getProjectById(existingBudget.getProject().getId());
                    if (project == null || !currentUserForDelete.getId().equals(project.getManager().getId())) {
                        throw new RuntimeException("您只能删除自己管理项目的预算");
                    }
                } else {
                    throw new RuntimeException("该预算未关联项目，您无权删除");
                }
            }
        } else if (!hasDeleteAllPermission) {
            throw new RuntimeException("您没有权限删除预算");
        }
        
        budgetService.deleteBudget(id);
    }

    @GetMapping("/{id}")
    public Budget getBudgetById(@PathVariable Long id) {
        return budgetService.getBudgetById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('budget:view:all') or hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    public List<Budget> getAllBudgets() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // 调试信息：打印当前用户的所有权限
        System.out.println("=== 预算查询权限调试 ===");
        System.out.println("用户名: " + username);
        System.out.println("用户权限: " + auth.getAuthorities());
        
        // 获取当前用户实体以检查角色
        User currentUser = (User) userService.loadUserByUsername(username);
        System.out.println("用户角色: " + (currentUser.getRoles() != null ? 
            currentUser.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toList()) : 
            "无角色"));
        
        // 检查用户角色
        boolean hasAdminRole = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || role.getName().contains("管理员"));
                
        boolean hasFinanceRole = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> "ROLE_FINANCE".equals(role.getName()) || role.getName().contains("财务"));
                
        // 检查用户权限（只有 budget:view:all 权限才能查看所有预算）
        boolean hasBudgetViewAllPermission = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String authName = authority.getAuthority();
                    return "budget:view:all".equals(authName);
                });
                
        boolean hasViewAllPermission = hasAdminRole || hasFinanceRole || hasBudgetViewAllPermission;
        
        // 检查是否是项目经理（检查角色而不是权限）
        boolean isManager = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> {
                    String roleName = role.getName();
                    return "ROLE_MANAGER".equals(roleName) || 
                           "MANAGER".equals(roleName) || 
                           "项目经理".equals(roleName);
                });
        
        System.out.println("hasAdminRole: " + hasAdminRole);
        System.out.println("hasFinanceRole: " + hasFinanceRole);
        System.out.println("hasBudgetViewAllPermission: " + hasBudgetViewAllPermission);
        System.out.println("isManager: " + isManager);
        System.out.println("hasViewAllPermission: " + hasViewAllPermission);
        
        if (hasViewAllPermission) {
            System.out.println("=== 执行全权限分支：管理员或财务可以查看所有预算 ===");
            // 管理员和财务可以查看所有预算
            List<Budget> allBudgets = budgetService.getAllBudgets();
            System.out.println("查询到的预算数量: " + (allBudgets != null ? allBudgets.size() : 0));
            return allBudgets;
        } else if (isManager) {
            System.out.println("=== 执行项目经理分支：只能查看自己管理的项目预算 ===");
            // 项目经理只能查看自己管理的项目的预算
            if (currentUser != null) {
                try {
                    List<Project> managedProjects = projectService.getProjectsByManagerId(currentUser.getId());
                    List<Budget> budgets = new ArrayList<>();
                    
                    if (managedProjects != null && !managedProjects.isEmpty()) {
                        for (Project project : managedProjects) {
                            List<Budget> projectBudgets = budgetService.getBudgetsByProject(project.getId());
                            if (projectBudgets != null && !projectBudgets.isEmpty()) {
                                budgets.addAll(projectBudgets);
                            }
                        }
                    }
                    
                    return budgets;
                } catch (Exception e) {
                    return new ArrayList<>();
                }
            }
        }
        
        // 其他用户（包括普通项目参与者）无权查看预算列表
        System.out.println("用户无权限，返回空列表");
        return new ArrayList<>();
    }
    
    @GetMapping("/debug/count")
    public Map<String, Object> getDebugInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("username", username);
        debugInfo.put("authorities", auth.getAuthorities().toString());
        
        // 获取当前用户实体以检查角色
        User currentUser = (User) userService.loadUserByUsername(username);
        debugInfo.put("roles", currentUser.getRoles() != null ? 
            currentUser.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toList()) : 
            java.util.Arrays.asList("无角色"));
        
        // 查询数据库中预算总数
        List<Budget> allBudgets = budgetService.getAllBudgets();
        debugInfo.put("totalBudgetsInDb", allBudgets != null ? allBudgets.size() : 0);
        
        // 检查用户角色
        boolean hasAdminRole = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || role.getName().contains("管理员"));
                
        boolean hasFinanceRole = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> "ROLE_FINANCE".equals(role.getName()) || role.getName().contains("财务"));
                
        // 检查用户权限（只有 budget:view:all 权限才能查看所有预算）
        boolean hasBudgetViewAllPermission = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String authName = authority.getAuthority();
                    return "budget:view:all".equals(authName);
                });
                
        boolean hasViewAllPermission = hasAdminRole || hasFinanceRole || hasBudgetViewAllPermission;
        
        debugInfo.put("hasAdminRole", hasAdminRole);
        debugInfo.put("hasFinanceRole", hasFinanceRole);
        debugInfo.put("hasBudgetViewAllPermission", hasBudgetViewAllPermission);
        debugInfo.put("hasViewAllPermission", hasViewAllPermission);
        
        return debugInfo;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('budget:view:all') or hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    public PagedResponse<Budget> searchBudgets(BudgetSearchDTO searchDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // 获取当前用户实体以检查角色
        User currentUser = (User) userService.loadUserByUsername(username);
        
        // 检查用户角色
        boolean hasAdminRole = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || role.getName().contains("管理员"));
                
        boolean hasFinanceRole = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> "ROLE_FINANCE".equals(role.getName()) || role.getName().contains("财务"));
                
        // 检查用户权限（只有 budget:view:all 权限才能查看所有预算）
        boolean hasBudgetViewAllPermission = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String authName = authority.getAuthority();
                    return "budget:view:all".equals(authName);
                });
                
        boolean hasViewAllPermission = hasAdminRole || hasFinanceRole || hasBudgetViewAllPermission;
        
        // 检查是否是项目经理（检查角色而不是权限）
        boolean isManager = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> {
                    String roleName = role.getName();
                    return "ROLE_MANAGER".equals(roleName) || 
                           "MANAGER".equals(roleName) || 
                           "项目经理".equals(roleName);
                });
        
        if (hasViewAllPermission) {
            // 管理员和财务可以搜索所有预算
            return budgetService.searchBudgets(searchDTO);
        } else if (isManager) {
            // 项目经理只能搜索自己管理的项目的预算
            if (currentUser != null) {
                try {
                    List<Project> managedProjects = projectService.getProjectsByManagerId(currentUser.getId());
                    List<Long> projectIds = managedProjects != null ? 
                        managedProjects.stream().map(Project::getId).toList() : 
                        new ArrayList<>();
                    
                    return budgetService.searchBudgetsForManager(searchDTO, projectIds);
                } catch (Exception e) {
                    return new PagedResponse<>(new ArrayList<>(), searchDTO.getPage(), searchDTO.getSize(), 0);
                }
            }
        }
        
        // 其他用户无权搜索预算
        return new PagedResponse<>(new ArrayList<>(), searchDTO.getPage(), searchDTO.getSize(), 0);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyAuthority('budget:view:all') or hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    public List<Budget> getBudgetsByProject(@PathVariable Long projectId) {
        // 检查权限 - 只有管理员、财务和项目经理可以查看特定项目的预算
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // 获取当前用户实体以检查角色
        User currentUser = (User) userService.loadUserByUsername(username);
        
        // 检查用户角色
        boolean hasAdminRole = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || role.getName().contains("管理员"));
                
        boolean hasFinanceRole = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> "ROLE_FINANCE".equals(role.getName()) || role.getName().contains("财务"));
                
        // 检查用户权限（只有 budget:view:all 权限才能查看所有预算）
        boolean hasBudgetViewAllPermission = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String authName = authority.getAuthority();
                    return "budget:view:all".equals(authName);
                });
                
        boolean hasViewAllPermission = hasAdminRole || hasFinanceRole || hasBudgetViewAllPermission;
        
        // 检查是否是项目经理（检查角色而不是权限）
        boolean isManager = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(role -> {
                    String roleName = role.getName();
                    return "ROLE_MANAGER".equals(roleName) || 
                           "MANAGER".equals(roleName) || 
                           "项目经理".equals(roleName);
                });
        
        if (!hasViewAllPermission && isManager) {
            // 项目经理只能查看自己管理的项目的预算
            if (currentUser != null) {
                Project project = projectService.getProjectById(projectId);
                if (project == null || !currentUser.getId().equals(project.getManager().getId())) {
                    throw new RuntimeException("您没有权限查看此项目的预算");
                }
            }
        } else if (!hasViewAllPermission) {
            // 非管理员、非财务、非项目经理的用户无权查看项目预算
            throw new RuntimeException("您没有权限查看项目预算");
        }
        
        return budgetService.getBudgetsByProject(projectId);
    }

    @GetMapping("/status/{status}")
    public List<Budget> getBudgetsByStatus(@PathVariable String status) {
        return budgetService.getBudgetsByStatus(status);
    }

    @GetMapping("/date-range")
    public List<Budget> getBudgetsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return budgetService.getBudgetsByDateRange(startDate, endDate);
    }

    @GetMapping("/active/project/{projectId}/date")
    public List<Budget> getActiveBudgetsByProjectAndDate(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        return budgetService.getActiveBudgetsByProjectAndDate(projectId, date);
    }

    @GetMapping("/project/{projectId}/total")
    public Double getTotalBudgetAmountByProject(@PathVariable Long projectId) {
        return budgetService.getTotalBudgetAmountByProject(projectId);
    }

    @GetMapping("/project/{projectId}/used")
    public Double getTotalUsedAmountByProject(@PathVariable Long projectId) {
        return budgetService.getTotalUsedAmountByProject(projectId);
    }

    // 预算项目管理
    @PostMapping("/{budgetId}/items")
    public BudgetItem createBudgetItem(@PathVariable Long budgetId, @RequestBody BudgetItem budgetItem) {
        Budget budget = new Budget();
        budget.setId(budgetId);
        budgetItem.setBudget(budget);
        return budgetService.createBudgetItem(budgetItem);
    }

    @PutMapping("/items/{id}")
    public BudgetItem updateBudgetItem(@PathVariable Long id, @RequestBody BudgetItem budgetItem) {
        budgetItem.setId(id);
        return budgetService.updateBudgetItem(budgetItem);
    }

    @DeleteMapping("/items/{id}")
    public void deleteBudgetItem(@PathVariable Long id) {
        budgetService.deleteBudgetItem(id);
    }

    @GetMapping("/items/{id}")
    public BudgetItem getBudgetItemById(@PathVariable Long id) {
        return budgetService.getBudgetItemById(id);
    }

    @GetMapping("/{budgetId}/items")
    public List<BudgetItem> getBudgetItemsByBudget(@PathVariable Long budgetId) {
        return budgetService.getBudgetItemsByBudget(budgetId);
    }

    @GetMapping("/items/category/{category}")
    public List<BudgetItem> getBudgetItemsByCategory(@PathVariable String category) {
        return budgetService.getBudgetItemsByCategory(category);
    }

    @GetMapping("/items/status/{status}")
    public List<BudgetItem> getBudgetItemsByStatus(@PathVariable String status) {
        return budgetService.getBudgetItemsByStatus(status);
    }

    @GetMapping("/project/{projectId}/items")
    public List<BudgetItem> getBudgetItemsByProject(@PathVariable Long projectId) {
        return budgetService.getBudgetItemsByProject(projectId);
    }

    @GetMapping("/{budgetId}/items/low-remaining")
    public List<BudgetItem> getLowRemainingItems(@PathVariable Long budgetId) {
        return budgetService.getLowRemainingItems(budgetId);
    }

    @GetMapping("/{budgetId}/items/total")
    public Double getTotalAmountByBudget(@PathVariable Long budgetId) {
        return budgetService.getTotalAmountByBudget(budgetId);
    }

    @GetMapping("/{budgetId}/items/used")
    public Double getTotalUsedAmountByBudget(@PathVariable Long budgetId) {
        return budgetService.getTotalUsedAmountByBudget(budgetId);
    }

    // 预算支出管理
    @PostMapping("/{budgetId}/expenses")
    public BudgetExpense createBudgetExpense(@PathVariable Long budgetId, @RequestBody BudgetExpense budgetExpense) {
        Budget budget = new Budget();
        budget.setId(budgetId);
        budgetExpense.setBudget(budget);
        return budgetService.createBudgetExpense(budgetExpense);
    }
    
    // 使用DTO创建预算支出（推荐方式）
    @PostMapping("/expenses")
    public BudgetExpense createBudgetExpenseWithDTO(@RequestBody BudgetExpenseRequestDTO expenseRequestDTO) {
        // 将DTO转换为BudgetExpense实体
        BudgetExpense budgetExpense = new BudgetExpense();
        budgetExpense.setAmount(expenseRequestDTO.getAmount());
        budgetExpense.setExpenseDate(expenseRequestDTO.getExpenseDate());
        budgetExpense.setExpenseType(expenseRequestDTO.getExpenseType());
        budgetExpense.setReferenceNumber(expenseRequestDTO.getReferenceNumber());
        budgetExpense.setStatus(expenseRequestDTO.getStatus());
        budgetExpense.setDescription(expenseRequestDTO.getDescription());
        budgetExpense.setRecordTime(expenseRequestDTO.getRecordTime());
        budgetExpense.setCreateTime(expenseRequestDTO.getCreateTime());
        budgetExpense.setLastUpdateTime(expenseRequestDTO.getLastUpdateTime());
        
        // 设置预算关联
        if (expenseRequestDTO.getBudgetId() != null) {
            Budget budget = new Budget();
            budget.setId(expenseRequestDTO.getBudgetId());
            budgetExpense.setBudget(budget);
        }
        
        // 设置预算项目关联（可选）
        if (expenseRequestDTO.getBudgetItemId() != null) {
            BudgetItem budgetItem = new BudgetItem();
            budgetItem.setId(expenseRequestDTO.getBudgetItemId());
            budgetExpense.setBudgetItem(budgetItem);
        }
        
        // 设置记录人关联（可选）
        if (expenseRequestDTO.getRecordedBy() != null) {
            User recordedBy = new User();
            recordedBy.setId(expenseRequestDTO.getRecordedBy());
            budgetExpense.setRecordedBy(recordedBy);
        }
        
        return budgetService.createBudgetExpense(budgetExpense);
    }

    @PutMapping("/expenses/{id}")
    public BudgetExpense updateBudgetExpense(@PathVariable Long id, @RequestBody BudgetExpenseRequestDTO expenseRequestDTO) {
        // 将DTO转换为BudgetExpense实体
        BudgetExpense budgetExpense = new BudgetExpense();
        budgetExpense.setId(id);
        budgetExpense.setAmount(expenseRequestDTO.getAmount());
        budgetExpense.setExpenseDate(expenseRequestDTO.getExpenseDate());
        budgetExpense.setExpenseType(expenseRequestDTO.getExpenseType());
        budgetExpense.setReferenceNumber(expenseRequestDTO.getReferenceNumber());
        budgetExpense.setStatus(expenseRequestDTO.getStatus());
        budgetExpense.setDescription(expenseRequestDTO.getDescription());
        budgetExpense.setRecordTime(expenseRequestDTO.getRecordTime());
        
        // 设置预算关联
        if (expenseRequestDTO.getBudgetId() != null) {
            Budget budget = new Budget();
            budget.setId(expenseRequestDTO.getBudgetId());
            budgetExpense.setBudget(budget);
        }
        
        // 设置预算项目关联（可选）
        if (expenseRequestDTO.getBudgetItemId() != null) {
            BudgetItem budgetItem = new BudgetItem();
            budgetItem.setId(expenseRequestDTO.getBudgetItemId());
            budgetExpense.setBudgetItem(budgetItem);
        }
        
        // 设置记录人关联（可选）
        if (expenseRequestDTO.getRecordedBy() != null) {
            User recordedBy = new User();
            recordedBy.setId(expenseRequestDTO.getRecordedBy());
            budgetExpense.setRecordedBy(recordedBy);
        }
        
        return budgetService.updateBudgetExpense(budgetExpense);
    }

    @DeleteMapping("/expenses/{id}")
    public void deleteBudgetExpense(@PathVariable Long id) {
        budgetService.deleteBudgetExpense(id);
    }

    @GetMapping("/expenses/{id}")
    public BudgetExpense getBudgetExpenseById(@PathVariable Long id) {
        return budgetService.getBudgetExpenseById(id);
    }

    @GetMapping("/expenses")
    public List<BudgetExpense> getAllBudgetExpenses() {
        return budgetService.getAllBudgetExpenses();
    }

    @GetMapping("/{budgetId}/expenses")
    public List<BudgetExpense> getBudgetExpensesByBudget(@PathVariable Long budgetId) {
        // 确保调用最新的 Service 方法
        return budgetService.getBudgetExpensesByBudget(budgetId);
    }

    @GetMapping("/items/{budgetItemId}/expenses")
    public List<BudgetExpense> getBudgetExpensesByBudgetItem(@PathVariable Long budgetItemId) {
        return budgetService.getBudgetExpensesByBudgetItem(budgetItemId);
    }

    @GetMapping("/expenses/type/{expenseType}")
    public List<BudgetExpense> getBudgetExpensesByExpenseType(@PathVariable String expenseType) {
        return budgetService.getBudgetExpensesByExpenseType(expenseType);
    }

    @GetMapping("/expenses/date-range")
    public List<BudgetExpense> getBudgetExpensesByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return budgetService.getBudgetExpensesByDateRange(startDate, endDate);
    }

    @GetMapping("/expenses/status/{status}")
    public List<BudgetExpense> getBudgetExpensesByStatus(@PathVariable String status) {
        return budgetService.getBudgetExpensesByStatus(status);
    }

    /**
     * 获取项目级预算支出（已过时）
     * @deprecated 请使用 /api/projects/{projectId}/expenses
     */
    @Deprecated
    @GetMapping("/project/{projectId}/expenses")
    public List<BudgetExpense> getBudgetExpensesByProject(@PathVariable Long projectId) {
        return budgetService.getBudgetExpensesByProject(projectId);
    }

    // 预算监控
    @GetMapping("/{id}/monitor")
    public Map<String, Object> getBudgetMonitorStats(@PathVariable Long id) {
        return budgetService.getBudgetMonitorStats(id);
    }

    @GetMapping("/project/{projectId}/monitor")
    public Map<String, Object> getProjectBudgetMonitorStats(@PathVariable Long projectId) {
        return budgetService.getProjectBudgetMonitorStats(projectId);
    }

    // 预算预警管理
    @PostMapping("/{id}/alerts")
    public BudgetAlert createBudgetAlert(@PathVariable Long id, @RequestBody BudgetAlert budgetAlert) {
        Budget budget = new Budget();
        budget.setId(id);
        budgetAlert.setBudget(budget);
        return budgetService.createBudgetAlert(budgetAlert);
    }

    @PutMapping("/alerts/{id}")
    public BudgetAlert updateBudgetAlert(@PathVariable Long id, @RequestBody BudgetAlert budgetAlert) {
        budgetAlert.setId(id);
        return budgetService.updateBudgetAlert(budgetAlert);
    }

    @DeleteMapping("/alerts/{id}")
    public void deleteBudgetAlert(@PathVariable Long id) {
        budgetService.deleteBudgetAlert(id);
    }

    @GetMapping("/alerts/{id}")
    public BudgetAlert getBudgetAlertById(@PathVariable Long id) {
        return budgetService.getBudgetAlertById(id);
    }

    @GetMapping("/{budgetId}/alerts")
    public List<BudgetAlert> getBudgetAlertsByBudget(@PathVariable Long budgetId) {
        return budgetService.getBudgetAlertsByBudget(budgetId);
    }

    @GetMapping("/items/{budgetItemId}/alerts")
    public List<BudgetAlert> getBudgetAlertsByBudgetItem(@PathVariable Long budgetItemId) {
        return budgetService.getBudgetAlertsByBudgetItem(budgetItemId);
    }

    @GetMapping("/alerts/type/{alertType}")
    public List<BudgetAlert> getBudgetAlertsByAlertType(@PathVariable String alertType) {
        return budgetService.getBudgetAlertsByAlertType(alertType);
    }

    @GetMapping("/alerts/level/{alertLevel}")
    public List<BudgetAlert> getBudgetAlertsByAlertLevel(@PathVariable String alertLevel) {
        return budgetService.getBudgetAlertsByAlertLevel(alertLevel);
    }

    @GetMapping("/alerts/status/{status}")
    public List<BudgetAlert> getBudgetAlertsByStatus(@PathVariable String status) {
        return budgetService.getBudgetAlertsByStatus(status);
    }

    @GetMapping("/alerts/date-range")
    public List<BudgetAlert> getBudgetAlertsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return budgetService.getBudgetAlertsByDateRange(startDate, endDate);
    }

    @GetMapping("/project/{projectId}/alerts")
    public List<BudgetAlert> getBudgetAlertsByProject(@PathVariable Long projectId) {
        return budgetService.getBudgetAlertsByProject(projectId);
    }

    @GetMapping("/project/{projectId}/alerts/unresolved")
    public List<BudgetAlert> getUnresolvedAlertsByProject(@PathVariable Long projectId) {
        return budgetService.getUnresolvedAlertsByProject(projectId);
    }

    @GetMapping("/{budgetId}/alerts/count/unresolved")
    public Long countUnresolvedAlertsByBudget(@PathVariable Long budgetId) {
        return budgetService.countUnresolvedAlertsByBudget(budgetId);
    }

    @GetMapping("/project/{projectId}/alerts/count/unresolved")
    public Long countUnresolvedAlertsByProject(@PathVariable Long projectId) {
        return budgetService.countUnresolvedAlertsByProject(projectId);
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public BudgetAlert resolveBudgetAlert(
            @PathVariable Long alertId,
            @RequestParam String resolution,
            @RequestParam Long resolvedById) {
        return budgetService.resolveBudgetAlert(alertId, resolution, resolvedById);
    }

    // 预算检查
    @GetMapping("/project/{projectId}/check")
    public boolean checkBudgetAvailability(
            @PathVariable Long projectId,
            @RequestParam Double amount) {
        return budgetService.checkBudgetAvailability(projectId, amount);
    }

    @GetMapping("/items/{budgetItemId}/check")
    public boolean checkBudgetItemAvailability(
            @PathVariable Long budgetItemId,
            @RequestParam Double amount) {
        return budgetService.checkBudgetItemAvailability(budgetItemId, amount);
    }

    // 预算统计分析
    @GetMapping("/stats/project/{projectId}")
    public Map<String, Object> getBudgetStatsByProject(@PathVariable Long projectId) {
        return budgetService.getBudgetStatsByProject(projectId);
    }

    @GetMapping("/stats/date-range")
    public Map<String, Object> getBudgetStatsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        return budgetService.getBudgetStatsByDateRange(startDate, endDate);
    }

    @GetMapping("/stats/performance")
    public Map<String, Object> getBudgetPerformanceStats() {
        return budgetService.getBudgetPerformanceStats();
    }

    @GetMapping("/stats/trend")
    public Map<String, Object> getBudgetTrend(@RequestParam(defaultValue = "12") int months) {
        return budgetService.getBudgetTrend(months);
    }

    // 报销相关的预算查询
    @GetMapping("/expenses/reimbursement/{reimbursementId}")
    public List<BudgetExpense> getBudgetExpensesByReimbursement(@PathVariable Long reimbursementId) {
        return budgetService.getBudgetExpensesByReimbursement(reimbursementId);
    }

    @GetMapping("/project/{projectId}/available-budgets")
    @PreAuthorize("hasAnyAuthority('budget:view:all') or hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    public List<Budget> getAvailableBudgetsForProject(@PathVariable Long projectId) {
        // 检查权限 - 只有管理员、财务和项目经理可以查看项目可用预算
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // 分别检查角色和权限
        boolean hasAdminRole = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String authName = authority.getAuthority();
                    return "ROLE_ADMIN".equals(authName) || authName.contains("管理员");
                });
                
        boolean hasFinanceRole = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String authName = authority.getAuthority();
                    return "ROLE_FINANCE".equals(authName) || authName.contains("财务");
                });
                
        boolean hasBudgetViewPermission = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String authName = authority.getAuthority();
                    return "budget:view:all".equals(authName) || 
                           "budget:view".equals(authName);
                });
                
        boolean hasViewAllPermission = hasAdminRole || hasFinanceRole || hasBudgetViewPermission;
        
        // 检查是否是项目经理
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String role = authority.getAuthority();
                    return "ROLE_MANAGER".equals(role) || 
                           "MANAGER".equals(role) || 
                           "项目经理".equals(role) ||
                           "manager-time:manage".equals(role) ||
                           "project:edit".equals(role);
                });
        
        if (!hasViewAllPermission && isManager) {
            // 项目经理只能查看自己管理的项目的预算
            User currentUser = (User) userService.loadUserByUsername(username);
            if (currentUser != null) {
                Project project = projectService.getProjectById(projectId);
                if (project == null || !currentUser.getId().equals(project.getManager().getId())) {
                    throw new RuntimeException("您没有权限查看此项目的预算");
                }
            }
        } else if (!hasViewAllPermission) {
            // 非管理员、非财务、非项目经理的用户无权查看项目预算
            throw new RuntimeException("您没有权限查看此项目的预算");
        }
        
        return budgetService.getAvailableBudgetsForProject(projectId);
    }

    @GetMapping("/project/{projectId}/budget-items")
    @PreAuthorize("hasAnyAuthority('budget:view:all') or hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    public List<BudgetItem> getAvailableBudgetItemsForProject(@PathVariable Long projectId) {
        // 检查权限 - 只有管理员、财务和项目经理可以查看项目预算项目
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // 分别检查角色和权限
        boolean hasAdminRole = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String authName = authority.getAuthority();
                    return "ROLE_ADMIN".equals(authName) || authName.contains("管理员");
                });
                
        boolean hasFinanceRole = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String authName = authority.getAuthority();
                    return "ROLE_FINANCE".equals(authName) || authName.contains("财务");
                });
                
        boolean hasBudgetViewPermission = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String authName = authority.getAuthority();
                    return "budget:view:all".equals(authName) || 
                           "budget:view".equals(authName);
                });
                
        boolean hasViewAllPermission = hasAdminRole || hasFinanceRole || hasBudgetViewPermission;
        
        // 检查是否是项目经理
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String role = authority.getAuthority();
                    return "ROLE_MANAGER".equals(role) || 
                           "MANAGER".equals(role) || 
                           "项目经理".equals(role) ||
                           "manager-time:manage".equals(role) ||
                           "project:edit".equals(role);
                });
        
        if (!hasViewAllPermission && isManager) {
            // 项目经理只能查看自己管理的项目的预算项目
            User currentUser = (User) userService.loadUserByUsername(username);
            if (currentUser != null) {
                Project project = projectService.getProjectById(projectId);
                if (project == null || !currentUser.getId().equals(project.getManager().getId())) {
                    throw new RuntimeException("您没有权限查看此项目的预算项目");
                }
            }
        } else if (!hasViewAllPermission) {
            // 非管理员、非财务、非项目经理的用户无权查看项目预算项目
            throw new RuntimeException("您没有权限查看此项目的预算项目");
        }
        
        return budgetService.getBudgetItemsByProject(projectId).stream()
                .filter(item -> item.getRemainingAmount() != null && item.getRemainingAmount() > 0)
                .toList();
    }
}
