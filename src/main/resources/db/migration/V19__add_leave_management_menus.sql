-- 添加假期管理高级功能菜单
-- 年假规则配置和假期余额管理菜单项

-- 插入年假规则配置菜单
INSERT INTO menu (id, name, path, component, parent_id, icon, sort_order, is_hidden, description)
VALUES
(50, '年假规则配置', '/annual-leave-rules', 'AnnualLeaveRuleManagement', 2, 'Setting', 7, FALSE, '年假规则配置管理'),
(51, '假期余额管理', '/leave-balance', 'LeaveBalanceManagement', 2, 'Wallet', 8, FALSE, '假期余额管理');

-- 插入功能权限 - 年假规则管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('annual-rule:view', '查看年假规则', 'FUNCTIONAL', 'ANNUAL_LEAVE_RULE', 'READ', TRUE),
('annual-rule:add', '添加年假规则', 'FUNCTIONAL', 'ANNUAL_LEAVE_RULE', 'CREATE', TRUE),
('annual-rule:edit', '编辑年假规则', 'FUNCTIONAL', 'ANNUAL_LEAVE_RULE', 'UPDATE', TRUE),
('annual-rule:delete', '删除年假规则', 'FUNCTIONAL', 'ANNUAL_LEAVE_RULE', 'DELETE', TRUE),
('annual-rule:toggle', '启用/禁用年假规则', 'FUNCTIONAL', 'ANNUAL_LEAVE_RULE', 'TOGGLE', TRUE);

-- 插入功能权限 - 假期余额管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('balance:view', '查看假期余额', 'FUNCTIONAL', 'LEAVE_BALANCE', 'READ', TRUE),
('balance:adjust', '调整假期余额', 'FUNCTIONAL', 'LEAVE_BALANCE', 'ADJUST', TRUE),
('balance:init', '初始化假期余额', 'FUNCTIONAL', 'LEAVE_BALANCE', 'INIT', TRUE),
('balance:carryover', '执行假期结转', 'FUNCTIONAL', 'LEAVE_BALANCE', 'CARRYOVER', TRUE),
('balance:statistics', '查看余额统计', 'FUNCTIONAL', 'LEAVE_BALANCE', 'STATISTICS', TRUE),
('balance:repair', '修复余额数据', 'FUNCTIONAL', 'LEAVE_BALANCE', 'REPAIR', TRUE);

-- 为年假规则配置菜单分配权限
INSERT INTO menu_permission (menu_id, permission_id)
SELECT m.id, p.id
FROM menu m, permission p
WHERE m.name = '年假规则配置'
  AND p.permission_type = 'FUNCTIONAL'
  AND p.resource = 'ANNUAL_LEAVE_RULE';

-- 为假期余额管理菜单分配权限
INSERT INTO menu_permission (menu_id, permission_id)
SELECT m.id, p.id
FROM menu m, permission p
WHERE m.name = '假期余额管理'
  AND p.permission_type = 'FUNCTIONAL'
  AND p.resource = 'LEAVE_BALANCE';

-- 为管理员角色分配新权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'ROLE_ADMIN'
  AND p.permission_type = 'FUNCTIONAL'
  AND p.resource IN ('ANNUAL_LEAVE_RULE', 'LEAVE_BALANCE');

-- 为HR经理角色分配新权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'HR_MANAGER'
  AND p.permission_type = 'FUNCTIONAL'
  AND p.resource IN ('ANNUAL_LEAVE_RULE', 'LEAVE_BALANCE');

-- 为员工角色分配只读权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'EMPLOYEE'
  AND p.permission_type = 'FUNCTIONAL'
  AND p.action = 'READ'
  AND p.resource = 'LEAVE_BALANCE';