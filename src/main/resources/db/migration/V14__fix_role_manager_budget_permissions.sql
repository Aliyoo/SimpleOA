-- V14: 修复ROLE_MANAGER角色的预算管理权限
-- 作者: System
-- 日期: 2025-08-01
-- 描述: 确保ROLE_MANAGER角色有正确的预算管理权限，支持项目经理查看和管理自己的项目预算

-- 1. 确保ROLE_MANAGER角色存在
INSERT INTO role (name, description) 
SELECT 'ROLE_MANAGER', '部门经理'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ROLE_MANAGER');

-- 2. 确保预算管理相关权限存在
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
SELECT * FROM (
    SELECT 'budget:view', '查看预算', 'FUNCTIONAL', 'BUDGET', 'READ', TRUE
    UNION ALL
    SELECT 'budget:add', '添加预算', 'FUNCTIONAL', 'BUDGET', 'CREATE', TRUE
    UNION ALL
    SELECT 'budget:edit', '编辑预算', 'FUNCTIONAL', 'BUDGET', 'UPDATE', TRUE
    UNION ALL
    SELECT 'budget:delete', '删除预算', 'FUNCTIONAL', 'BUDGET', 'DELETE', TRUE
    UNION ALL
    SELECT 'budget:view:own', '查看自己项目的预算', 'FUNCTIONAL', 'BUDGET', 'READ_OWN', TRUE
    UNION ALL
    SELECT 'budget:edit:own', '编辑自己项目的预算', 'FUNCTIONAL', 'BUDGET', 'UPDATE_OWN', TRUE
    UNION ALL
    SELECT 'budget:delete:own', '删除自己项目的预算', 'FUNCTIONAL', 'BUDGET', 'DELETE_OWN', TRUE
    UNION ALL
    SELECT 'budget:view:all', '查看所有项目的预算', 'FUNCTIONAL', 'BUDGET', 'READ_ALL', TRUE
    UNION ALL
    SELECT 'budget:edit:all', '编辑所有项目的预算', 'FUNCTIONAL', 'BUDGET', 'UPDATE_ALL', TRUE
    UNION ALL
    SELECT 'budget:delete:all', '删除所有项目的预算', 'FUNCTIONAL', 'BUDGET', 'DELETE_ALL', TRUE
    UNION ALL
    SELECT 'budget:expense:view', '查看预算支出', 'FUNCTIONAL', 'BUDGET', 'EXPENSE_READ', TRUE
    UNION ALL
    SELECT 'budget:expense:add', '添加预算支出', 'FUNCTIONAL', 'BUDGET', 'EXPENSE_CREATE', TRUE
    UNION ALL
    SELECT 'budget:expense:edit', '编辑预算支出', 'FUNCTIONAL', 'BUDGET', 'EXPENSE_UPDATE', TRUE
) AS new_permissions
WHERE NOT EXISTS (
    SELECT 1 FROM permission p 
    WHERE p.name = new_permissions.name
);

-- 3. 为ROLE_MANAGER角色分配预算管理权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id 
FROM role r, permission p 
WHERE r.name = 'ROLE_MANAGER' 
AND p.name IN (
    'budget:view', 
    'budget:add', 
    'budget:view:own', 
    'budget:edit:own', 
    'budget:delete:own',
    'budget:expense:view',
    'budget:expense:add',
    'budget:expense:edit'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 4. 更新预算管理菜单权限（如果菜单存在）
INSERT INTO menu_permission (menu_id, permission_id)
SELECT m.id, p.id 
FROM menu m, permission p 
WHERE m.name IN ('预算管理', '预算支出管理')
AND p.name IN ('budget:view', 'budget:view:own', 'budget:edit:own', 'budget:delete:own')
AND NOT EXISTS (
    SELECT 1 FROM menu_permission mp 
    WHERE mp.menu_id = m.id AND mp.permission_id = p.id
);