-- V11: 添加项目经理角色和预算管理权限
-- 作者: System
-- 日期: 2025-08-01
-- 描述: 支持项目经理自己维护预算，财务和超管可查看维护所有
-- 注意: 此脚本中的ROLE_MANAGER已废弃，实际使用ROLE_MANAGER角色

-- 1. 添加项目经理角色（如果不存在）
INSERT INTO role (name, description) 
SELECT 'ROLE_MANAGER', '项目经理'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ROLE_MANAGER');

-- 2. 确保预算管理相关权限存在
-- 添加预算管理的细分权限
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
SELECT * FROM (
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
) AS new_permissions
WHERE NOT EXISTS (
    SELECT 1 FROM permission p 
    WHERE p.name = new_permissions.name
);

-- 3. 为项目经理角色分配预算管理权限（只能管理自己的项目）
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

-- 4. 更新管理员角色权限（可以管理所有预算）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id 
FROM role r, permission p 
WHERE r.name = 'ROLE_ADMIN' 
AND p.name IN (
    'budget:view:all', 
    'budget:edit:all', 
    'budget:delete:all'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 5. 为财务角色分配预算查看和维护权限（可以管理所有预算）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id 
FROM role r, permission p 
WHERE r.name = 'ROLE_FINANCE' 
AND p.name IN (
    'budget:view',
    'budget:add',
    'budget:edit',
    'budget:delete',
    'budget:view:all', 
    'budget:edit:all',
    'budget:delete:all',
    'budget:expense:view',
    'budget:expense:add',
    'budget:expense:edit',
    'budget:expense:delete',
    'budget:expense:approve'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 6. 更新部门经理角色权限（可以查看预算，但不能编辑）
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id 
FROM role r, permission p 
WHERE r.name = 'ROLE_MANAGER' 
AND p.name IN (
    'budget:view',
    'budget:expense:view'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 7. 创建一个示例项目经理用户（如果需要）
-- 注意：密码是 '123456' 的 BCrypt 加密结果
INSERT INTO user (username, password, real_name, email, phone, is_active, created_at, updated_at)
SELECT 'pm001', '$2a$10$Ux7mYBAXVtquBXcV2UqBKeCsEYkUCT8BQ7XbKyNg8i/3O0Kb6fH1y', '张项目经理', 'pm001@example.com', '13900000001', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'pm001');

-- 8. 为示例项目经理分配角色
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id 
FROM user u, role r 
WHERE u.username = 'pm001' 
AND r.name = 'ROLE_MANAGER'
AND NOT EXISTS (
    SELECT 1 FROM user_role ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- 9. 更新预算管理菜单权限，添加项目经理的相关权限
INSERT INTO menu_permission (menu_id, permission_id)
SELECT m.id, p.id 
FROM menu m, permission p 
WHERE m.name = '预算管理' 
AND p.name IN ('budget:view:own', 'budget:edit:own', 'budget:delete:own')
AND NOT EXISTS (
    SELECT 1 FROM menu_permission mp 
    WHERE mp.menu_id = m.id AND mp.permission_id = p.id
);

-- 10. 更新预算支出管理菜单权限
INSERT INTO menu_permission (menu_id, permission_id)
SELECT m.id, p.id 
FROM menu m, permission p 
WHERE m.name = '预算支出管理' 
AND p.name IN ('budget:view:own', 'budget:edit:own')
AND NOT EXISTS (
    SELECT 1 FROM menu_permission mp 
    WHERE mp.menu_id = m.id AND mp.permission_id = p.id
);
