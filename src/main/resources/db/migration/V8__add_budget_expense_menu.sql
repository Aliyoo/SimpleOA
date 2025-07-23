-- 添加预算支出管理菜单和权限
-- 作者: System
-- 日期: 2025-01-23
-- 描述: 补充预算支出管理菜单及相关权限配置

-- 1. 添加预算支出管理菜单（在项目管理模块下）
INSERT INTO menu (name, path, component, parent_id, icon, sort_order, is_hidden, description)
VALUES ('预算支出管理', '/budget-expense-management', 'BudgetExpenseManagement', 3, 'Coin', 7, FALSE, '预算支出管理');

-- 2. 添加预算支出相关权限
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES 
('budget:expense:view', '查看预算支出', 'FUNCTIONAL', 'BUDGET_EXPENSE', 'READ', TRUE),
('budget:expense:add', '添加预算支出', 'FUNCTIONAL', 'BUDGET_EXPENSE', 'CREATE', TRUE),
('budget:expense:edit', '编辑预算支出', 'FUNCTIONAL', 'BUDGET_EXPENSE', 'UPDATE', TRUE),
('budget:expense:delete', '删除预算支出', 'FUNCTIONAL', 'BUDGET_EXPENSE', 'DELETE', TRUE),
('budget:expense:approve', '审批预算支出', 'FUNCTIONAL', 'BUDGET_EXPENSE', 'APPROVE', TRUE);

-- 3. 关联预算支出管理菜单和权限
INSERT INTO menu_permission (menu_id, permission_id)
SELECT m.id, p.id 
FROM menu m, permission p 
WHERE m.name = '预算支出管理' 
AND p.name IN ('budget:expense:view', 'budget:expense:add', 'budget:expense:edit', 'budget:expense:delete', 'budget:expense:approve');

-- 4. 为管理员角色分配预算支出管理的所有权限
INSERT INTO role_permission (permission_id, role_id)
SELECT p.id, 1 
FROM permission p 
WHERE p.name IN ('budget:expense:view', 'budget:expense:add', 'budget:expense:edit', 'budget:expense:delete', 'budget:expense:approve')
AND p.id NOT IN (SELECT permission_id FROM role_permission WHERE role_id = 1);

-- 5. 为部门经理角色分配预算支出管理的基本权限（查看、编辑、审批）
INSERT INTO role_permission (permission_id, role_id)
SELECT p.id, 3 
FROM permission p 
WHERE p.name IN ('budget:expense:view', 'budget:expense:edit', 'budget:expense:approve')
AND p.id NOT IN (SELECT permission_id FROM role_permission WHERE role_id = 3);

-- 6. 为普通用户角色分配预算支出的查看权限
INSERT INTO role_permission (permission_id, role_id)
SELECT p.id, 2 
FROM permission p 
WHERE p.name = 'budget:expense:view'
AND p.id NOT IN (SELECT permission_id FROM role_permission WHERE role_id = 2);
