-- 菜单权限关联
-- 仪表盘
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 1, id FROM permission WHERE name = 'dashboard:view';

-- 工时管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 10, id FROM permission WHERE name IN ('time:view', 'time:add', 'time:edit', 'time:delete', 'time:approve', 'time:batch');

-- 项目经理工时
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 11, id FROM permission WHERE name IN ('manager-time:view', 'manager-time:manage');

-- 请假管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 12, id FROM permission WHERE name IN ('leave:view', 'leave:add', 'leave:edit', 'leave:delete', 'leave:approve');

-- 出差管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 13, id FROM permission WHERE name IN ('travel:view', 'travel:add', 'travel:edit', 'travel:delete', 'travel:approve');

-- 报销管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 14, id FROM permission WHERE name IN ('reimbursement:view', 'reimbursement:add', 'reimbursement:edit', 'reimbursement:delete', 'reimbursement:approve');

-- 审批管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 15, id FROM permission WHERE name IN ('approval:view', 'approval:process');

-- 项目信息
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 20, id FROM permission WHERE name IN ('project:view', 'project:add', 'project:edit', 'project:delete');

-- 任务管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 21, id FROM permission WHERE name IN ('task:view', 'task:add', 'task:edit', 'task:delete');

-- 外包管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 22, id FROM permission WHERE name IN ('outsourcing:view', 'outsourcing:add', 'outsourcing:edit', 'outsourcing:delete');

-- 付款管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 23, id FROM permission WHERE name IN ('payment:view', 'payment:add', 'payment:edit', 'payment:delete', 'payment:approve');

-- 预算管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 24, id FROM permission WHERE name IN ('budget:view', 'budget:add', 'budget:edit', 'budget:delete');

-- 绩效管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 25, id FROM permission WHERE name IN ('performance:view', 'performance:add', 'performance:edit', 'performance:delete', 'performance:evaluate');

-- 用户管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 30, id FROM permission WHERE name IN ('user:view', 'user:add', 'user:edit', 'user:delete');

-- 角色管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 31, id FROM permission WHERE name IN ('role:view', 'role:add', 'role:edit', 'role:delete', 'role:assign');

-- 权限管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 32, id FROM permission WHERE name IN ('permission:view', 'permission:add', 'permission:edit', 'permission:delete');

-- 日志管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 33, id FROM permission WHERE name IN ('log:view', 'log:export');

-- 通知管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 34, id FROM permission WHERE name IN ('notification:view', 'notification:add', 'notification:edit', 'notification:delete', 'notification:send');

-- 公告管理
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 35, id FROM permission WHERE name IN ('announcement:view', 'announcement:add', 'announcement:edit', 'announcement:delete', 'announcement:publish');

-- 系统配置
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 36, id FROM permission WHERE name IN ('system:view', 'system:edit');

-- 个人资料
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 40, id FROM permission WHERE name IN ('profile:view', 'profile:edit');

-- 为管理员角色分配所有权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE id NOT IN (SELECT permission_id FROM role_permission WHERE role_id = 1);

-- 为普通用户角色分配基本权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 2, id FROM permission 
WHERE name IN (
    'dashboard:view',
    'time:view', 'time:add', 'time:edit',
    'leave:view', 'leave:add', 'leave:edit',
    'travel:view', 'travel:add', 'travel:edit',
    'reimbursement:view', 'reimbursement:add', 'reimbursement:edit',
    'approval:view',
    'project:view',
    'task:view',
    'profile:view', 'profile:edit'
) AND id NOT IN (SELECT permission_id FROM role_permission WHERE role_id = 2);

-- 为部门经理角色分配管理权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 3, id FROM permission 
WHERE name IN (
    'dashboard:view',
    'time:view', 'time:add', 'time:edit', 'time:approve',
    'manager-time:view', 'manager-time:manage',
    'leave:view', 'leave:add', 'leave:edit', 'leave:approve',
    'travel:view', 'travel:add', 'travel:edit', 'travel:approve',
    'reimbursement:view', 'reimbursement:add', 'reimbursement:edit', 'reimbursement:approve',
    'approval:view', 'approval:process',
    'project:view', 'project:add', 'project:edit',
    'task:view', 'task:add', 'task:edit',
    'performance:view', 'performance:evaluate',
    'profile:view', 'profile:edit'
) AND id NOT IN (SELECT permission_id FROM role_permission WHERE role_id = 3);
