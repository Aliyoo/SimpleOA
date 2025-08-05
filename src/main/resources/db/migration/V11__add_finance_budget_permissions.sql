-- V11: 为财务角色添加预算管理权限

-- 为财务角色分配所有预算相关权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id 
FROM role r, permission p 
WHERE r.name = 'ROLE_FINANCE' 
AND p.name IN (
    'budget:view', 
    'budget:add', 
    'budget:edit', 
    'budget:delete',
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

-- 确保超管角色拥有所有预算权限（如果不存在的话）
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, p.id 
FROM permission p 
WHERE p.name IN (
    'budget:view', 
    'budget:add', 
    'budget:edit', 
    'budget:delete',
    'budget:expense:view',
    'budget:expense:add',
    'budget:expense:edit',
    'budget:expense:delete',
    'budget:expense:approve'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp 
    WHERE rp.role_id = 1 AND rp.permission_id = p.id
);

-- 为财务角色分配预算管理菜单权限
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 24, p.id 
FROM permission p 
WHERE p.name IN ('budget:view', 'budget:add', 'budget:edit', 'budget:delete')
AND NOT EXISTS (
    SELECT 1 FROM menu_permission mp 
    WHERE mp.menu_id = 24 AND mp.permission_id = p.id
);

-- 为财务角色分配预算支出管理菜单权限
INSERT INTO menu_permission (menu_id, permission_id)
SELECT m.id, p.id 
FROM menu m, permission p 
WHERE m.name = '预算支出管理'
AND p.name IN (
    'budget:expense:view',
    'budget:expense:add',
    'budget:expense:edit',
    'budget:expense:delete',
    'budget:expense:approve'
)
AND NOT EXISTS (
    SELECT 1 FROM menu_permission mp 
    WHERE mp.menu_id = m.id AND mp.permission_id = p.id
);