-- V10: 添加财务角色和报销相关权限

-- 添加财务角色
INSERT INTO role (name, description) VALUES ('ROLE_FINANCE', '财务管理员');

-- 添加报销相关权限（仅添加不存在的权限）
INSERT INTO permission (name, description, permission_type, resource, action)
SELECT 'reimbursement:finance_approve', '财务审批报销', 'FUNCTIONAL', 'reimbursement', 'finance_approve'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE name = 'reimbursement:finance_approve');

-- 为管理员分配所有报销权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission 
WHERE name LIKE 'reimbursement:%'
AND id NOT IN (SELECT permission_id FROM role_permission WHERE role_id = 1);

-- 为普通用户分配基本报销权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 2, id FROM permission 
WHERE name IN ('reimbursement:view', 'reimbursement:add', 'reimbursement:edit', 'reimbursement:delete')
AND id NOT IN (SELECT permission_id FROM role_permission WHERE role_id = 2);

-- 为部门经理分配报销审批权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 3, id FROM permission 
WHERE name IN ('reimbursement:view', 'reimbursement:approve')
AND id NOT IN (SELECT permission_id FROM role_permission WHERE role_id = 3);

-- 为财务角色分配财务审批权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id 
FROM role r, permission p 
WHERE r.name = 'ROLE_FINANCE' 
AND p.name IN ('reimbursement:view', 'reimbursement:finance_approve')
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);