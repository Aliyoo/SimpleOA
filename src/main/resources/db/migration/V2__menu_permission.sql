-- 创建菜单表
CREATE TABLE IF NOT EXISTS menu
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID，主键，自增',
    name        VARCHAR(50)  NOT NULL COMMENT '菜单名称，非空',
    path        VARCHAR(100) NOT NULL COMMENT '菜单路径，非空',
    component   VARCHAR(100) COMMENT '组件路径',
    parent_id   BIGINT COMMENT '父菜单ID',
    icon        VARCHAR(50) COMMENT '图标',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    is_hidden   BOOLEAN DEFAULT FALSE COMMENT '是否隐藏',
    description VARCHAR(200) COMMENT '描述',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (parent_id) REFERENCES menu (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='菜单表';

-- 创建菜单权限关联表
CREATE TABLE IF NOT EXISTS menu_permission
(
    menu_id       BIGINT NOT NULL COMMENT '菜单ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (menu_id, permission_id),
    FOREIGN KEY (menu_id) REFERENCES menu (id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permission (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='菜单权限关联表';

-- 插入菜单数据
-- 主菜单
INSERT INTO menu (id, name, path, component, parent_id, icon, sort_order, is_hidden, description)
VALUES
(1, '仪表盘', '/dashboard', 'Dashboard', NULL, 'Odometer', 1, FALSE, '系统仪表盘'),
(2, 'OA办公', 'oa', NULL, NULL, 'OfficeBuilding', 2, FALSE, 'OA办公模块'),
(3, '项目管理', 'pm', NULL, NULL, 'Folder', 3, FALSE, '项目管理模块'),
(4, '系统管理', 'sys', NULL, NULL, 'Setting', 4, FALSE, '系统管理模块');

-- OA办公子菜单
INSERT INTO menu (id, name, path, component, parent_id, icon, sort_order, is_hidden, description)
VALUES
(10, '工时管理', '/time-management', 'TimeManagement', 2, 'Clock', 1, FALSE, '工时管理'),
(11, '项目工时', '/project-manager-time', 'ProjectManagerTimeManagement', 2, 'Clock', 2, FALSE, '项目工时管理'),
(12, '请假管理', '/leave-management', 'LeaveManagement', 2, 'Calendar', 3, FALSE, '请假管理'),
(13, '出差管理', '/travel-management', 'TravelManagement', 2, 'Position', 4, FALSE, '出差管理'),
(14, '报销管理', '/reimbursement', 'Reimbursement', 2, 'Wallet', 5, FALSE, '报销管理'),
(15, '审批管理', '/approvals', 'Approvals', 2, 'Document', 6, FALSE, '审批管理');

-- 项目管理子菜单
INSERT INTO menu (id, name, path, component, parent_id, icon, sort_order, is_hidden, description)
VALUES
(20, '项目信息', '/projects', 'Projects', 3, 'Tickets', 1, FALSE, '项目信息'),
(21, '任务管理', '/tasks', 'Tasks', 3, 'List', 2, FALSE, '任务管理'),
(22, '外包管理', '/outsourcing-management', 'OutsourcingManagement', 3, 'Connection', 3, FALSE, '外包管理'),
(23, '付款管理', '/payment-management', 'PaymentManagement', 3, 'Money', 4, FALSE, '付款管理'),
(24, '预算管理', '/budget-management', 'BudgetManagement', 3, 'Coin', 5, FALSE, '预算管理'),
(25, '绩效管理', '/performance-management', 'PerformanceManagement', 3, 'DataAnalysis', 6, FALSE, '绩效管理');

-- 系统管理子菜单
INSERT INTO menu (id, name, path, component, parent_id, icon, sort_order, is_hidden, description)
VALUES
(30, '用户管理', '/user-management', 'UserManagement', 4, 'User', 1, FALSE, '用户管理'),
(31, '角色管理', '/role-management', 'RoleManagement', 4, 'Lock', 2, FALSE, '角色管理'),
(32, '权限管理', '/permission-management', 'PermissionManagement', 4, 'Key', 3, FALSE, '权限管理'),
(33, '日志管理', '/log-management', 'LogManagement', 4, 'DocumentCopy', 4, FALSE, '日志管理'),
(34, '通知管理', '/notification', 'Notification', 4, 'Bell', 5, FALSE, '通知管理'),
(35, '公告管理', '/announcement', 'Announcement', 4, 'Notification', 6, FALSE, '公告管理'),
(36, '系统配置', '/system-config', 'SystemConfig', 4, 'Tools', 7, FALSE, '系统配置');

-- 个人资料
INSERT INTO menu (id, name, path, component, parent_id, icon, sort_order, is_hidden, description)
VALUES
(40, '个人资料', '/profile', 'Profile', NULL, 'User', 5, FALSE, '个人资料');

-- 插入权限数据
-- 功能权限 - 仪表盘
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('dashboard:view', '查看仪表盘', 'FUNCTIONAL', 'DASHBOARD', 'READ', TRUE);

-- 功能权限 - 工时管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('time:view', '查看工时', 'FUNCTIONAL', 'WORKTIME', 'READ', TRUE),
('time:add', '添加工时', 'FUNCTIONAL', 'WORKTIME', 'CREATE', TRUE),
('time:edit', '编辑工时', 'FUNCTIONAL', 'WORKTIME', 'UPDATE', TRUE),
('time:delete', '删除工时', 'FUNCTIONAL', 'WORKTIME', 'DELETE', TRUE),
('time:approve', '审批工时', 'FUNCTIONAL', 'WORKTIME', 'APPROVE', TRUE),
('time:batch', '批量操作工时', 'FUNCTIONAL', 'WORKTIME', 'BATCH', TRUE);

-- 功能权限 - 项目工时
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('manager-time:view', '查看项目工时', 'FUNCTIONAL', 'MANAGER_WORKTIME', 'READ', TRUE),
('manager-time:manage', '管理项目工时', 'FUNCTIONAL', 'MANAGER_WORKTIME', 'MANAGE', TRUE);

-- 功能权限 - 请假管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('leave:view', '查看请假', 'FUNCTIONAL', 'LEAVE', 'READ', TRUE),
('leave:add', '申请请假', 'FUNCTIONAL', 'LEAVE', 'CREATE', TRUE),
('leave:edit', '编辑请假', 'FUNCTIONAL', 'LEAVE', 'UPDATE', TRUE),
('leave:delete', '删除请假', 'FUNCTIONAL', 'LEAVE', 'DELETE', TRUE),
('leave:approve', '审批请假', 'FUNCTIONAL', 'LEAVE', 'APPROVE', TRUE);

-- 功能权限 - 出差管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('travel:view', '查看出差', 'FUNCTIONAL', 'TRAVEL', 'READ', TRUE),
('travel:add', '申请出差', 'FUNCTIONAL', 'TRAVEL', 'CREATE', TRUE),
('travel:edit', '编辑出差', 'FUNCTIONAL', 'TRAVEL', 'UPDATE', TRUE),
('travel:delete', '删除出差', 'FUNCTIONAL', 'TRAVEL', 'DELETE', TRUE),
('travel:approve', '审批出差', 'FUNCTIONAL', 'TRAVEL', 'APPROVE', TRUE);

-- 功能权限 - 报销管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('reimbursement:view', '查看报销', 'FUNCTIONAL', 'REIMBURSEMENT', 'READ', TRUE),
('reimbursement:add', '申请报销', 'FUNCTIONAL', 'REIMBURSEMENT', 'CREATE', TRUE),
('reimbursement:edit', '编辑报销', 'FUNCTIONAL', 'REIMBURSEMENT', 'UPDATE', TRUE),
('reimbursement:delete', '删除报销', 'FUNCTIONAL', 'REIMBURSEMENT', 'DELETE', TRUE),
('reimbursement:approve', '审批报销', 'FUNCTIONAL', 'REIMBURSEMENT', 'APPROVE', TRUE);

-- 功能权限 - 审批管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('approval:view', '查看审批', 'FUNCTIONAL', 'APPROVAL', 'READ', TRUE),
('approval:process', '处理审批', 'FUNCTIONAL', 'APPROVAL', 'PROCESS', TRUE);

-- 功能权限 - 项目管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('project:view', '查看项目', 'FUNCTIONAL', 'PROJECT', 'READ', TRUE),
('project:add', '添加项目', 'FUNCTIONAL', 'PROJECT', 'CREATE', TRUE),
('project:edit', '编辑项目', 'FUNCTIONAL', 'PROJECT', 'UPDATE', TRUE),
('project:delete', '删除项目', 'FUNCTIONAL', 'PROJECT', 'DELETE', TRUE);

-- 功能权限 - 任务管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('task:view', '查看任务', 'FUNCTIONAL', 'TASK', 'READ', TRUE),
('task:add', '添加任务', 'FUNCTIONAL', 'TASK', 'CREATE', TRUE),
('task:edit', '编辑任务', 'FUNCTIONAL', 'TASK', 'UPDATE', TRUE),
('task:delete', '删除任务', 'FUNCTIONAL', 'TASK', 'DELETE', TRUE);

-- 功能权限 - 外包管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('outsourcing:view', '查看外包', 'FUNCTIONAL', 'OUTSOURCING', 'READ', TRUE),
('outsourcing:add', '添加外包', 'FUNCTIONAL', 'OUTSOURCING', 'CREATE', TRUE),
('outsourcing:edit', '编辑外包', 'FUNCTIONAL', 'OUTSOURCING', 'UPDATE', TRUE),
('outsourcing:delete', '删除外包', 'FUNCTIONAL', 'OUTSOURCING', 'DELETE', TRUE);

-- 功能权限 - 付款管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('payment:view', '查看付款', 'FUNCTIONAL', 'PAYMENT', 'READ', TRUE),
('payment:add', '添加付款', 'FUNCTIONAL', 'PAYMENT', 'CREATE', TRUE),
('payment:edit', '编辑付款', 'FUNCTIONAL', 'PAYMENT', 'UPDATE', TRUE),
('payment:delete', '删除付款', 'FUNCTIONAL', 'PAYMENT', 'DELETE', TRUE),
('payment:approve', '审批付款', 'FUNCTIONAL', 'PAYMENT', 'APPROVE', TRUE);

-- 功能权限 - 预算管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('budget:view', '查看预算', 'FUNCTIONAL', 'BUDGET', 'READ', TRUE),
('budget:add', '添加预算', 'FUNCTIONAL', 'BUDGET', 'CREATE', TRUE),
('budget:edit', '编辑预算', 'FUNCTIONAL', 'BUDGET', 'UPDATE', TRUE),
('budget:delete', '删除预算', 'FUNCTIONAL', 'BUDGET', 'DELETE', TRUE);

-- 功能权限 - 绩效管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('performance:view', '查看绩效', 'FUNCTIONAL', 'PERFORMANCE', 'READ', TRUE),
('performance:add', '添加绩效', 'FUNCTIONAL', 'PERFORMANCE', 'CREATE', TRUE),
('performance:edit', '编辑绩效', 'FUNCTIONAL', 'PERFORMANCE', 'UPDATE', TRUE),
('performance:delete', '删除绩效', 'FUNCTIONAL', 'PERFORMANCE', 'DELETE', TRUE),
('performance:evaluate', '评估绩效', 'FUNCTIONAL', 'PERFORMANCE', 'EVALUATE', TRUE);

-- 功能权限 - 用户管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('user:view', '查看用户', 'FUNCTIONAL', 'USER', 'READ', TRUE),
('user:add', '添加用户', 'FUNCTIONAL', 'USER', 'CREATE', TRUE),
('user:edit', '编辑用户', 'FUNCTIONAL', 'USER', 'UPDATE', TRUE),
('user:delete', '删除用户', 'FUNCTIONAL', 'USER', 'DELETE', TRUE);

-- 功能权限 - 角色管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('role:view', '查看角色', 'FUNCTIONAL', 'ROLE', 'READ', TRUE),
('role:add', '添加角色', 'FUNCTIONAL', 'ROLE', 'CREATE', TRUE),
('role:edit', '编辑角色', 'FUNCTIONAL', 'ROLE', 'UPDATE', TRUE),
('role:delete', '删除角色', 'FUNCTIONAL', 'ROLE', 'DELETE', TRUE),
('role:assign', '分配角色权限', 'FUNCTIONAL', 'ROLE', 'ASSIGN', TRUE);

-- 功能权限 - 权限管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('permission:view', '查看权限', 'FUNCTIONAL', 'PERMISSION', 'READ', TRUE),
('permission:add', '添加权限', 'FUNCTIONAL', 'PERMISSION', 'CREATE', TRUE),
('permission:edit', '编辑权限', 'FUNCTIONAL', 'PERMISSION', 'UPDATE', TRUE),
('permission:delete', '删除权限', 'FUNCTIONAL', 'PERMISSION', 'DELETE', TRUE);

-- 功能权限 - 日志管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('log:view', '查看日志', 'FUNCTIONAL', 'LOG', 'READ', TRUE),
('log:export', '导出日志', 'FUNCTIONAL', 'LOG', 'EXPORT', TRUE);

-- 功能权限 - 通知管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('notification:view', '查看通知', 'FUNCTIONAL', 'NOTIFICATION', 'READ', TRUE),
('notification:add', '添加通知', 'FUNCTIONAL', 'NOTIFICATION', 'CREATE', TRUE),
('notification:edit', '编辑通知', 'FUNCTIONAL', 'NOTIFICATION', 'UPDATE', TRUE),
('notification:delete', '删除通知', 'FUNCTIONAL', 'NOTIFICATION', 'DELETE', TRUE),
('notification:send', '发送通知', 'FUNCTIONAL', 'NOTIFICATION', 'SEND', TRUE);

-- 功能权限 - 公告管理
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('announcement:view', '查看公告', 'FUNCTIONAL', 'ANNOUNCEMENT', 'READ', TRUE),
('announcement:add', '添加公告', 'FUNCTIONAL', 'ANNOUNCEMENT', 'CREATE', TRUE),
('announcement:edit', '编辑公告', 'FUNCTIONAL', 'ANNOUNCEMENT', 'UPDATE', TRUE),
('announcement:delete', '删除公告', 'FUNCTIONAL', 'ANNOUNCEMENT', 'DELETE', TRUE),
('announcement:publish', '发布公告', 'FUNCTIONAL', 'ANNOUNCEMENT', 'PUBLISH', TRUE);

-- 功能权限 - 系统配置
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('system:view', '查看系统配置', 'FUNCTIONAL', 'SYSTEM', 'READ', TRUE),
('system:edit', '编辑系统配置', 'FUNCTIONAL', 'SYSTEM', 'UPDATE', TRUE);

-- 功能权限 - 个人资料
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES
('profile:view', '查看个人资料', 'FUNCTIONAL', 'PROFILE', 'READ', TRUE),
('profile:edit', '编辑个人资料', 'FUNCTIONAL', 'PROFILE', 'UPDATE', TRUE);
