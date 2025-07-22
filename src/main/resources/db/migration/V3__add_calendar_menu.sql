-- 添加日历管理菜单到 OA办公模块
INSERT INTO menu (id, name, path, component, parent_id, icon, sort_order, is_hidden, description)
VALUES (16, '日历管理', '/calendar', 'CalendarView', 2, 'Calendar', 7, FALSE, '日历管理');

-- 添加日历管理权限
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES 
('calendar:view', '查看日历', 'FUNCTIONAL', 'CALENDAR', 'READ', TRUE),
('calendar:add', '添加日历事件', 'FUNCTIONAL', 'CALENDAR', 'CREATE', TRUE),
('calendar:edit', '编辑日历事件', 'FUNCTIONAL', 'CALENDAR', 'UPDATE', TRUE),
('calendar:delete', '删除日历事件', 'FUNCTIONAL', 'CALENDAR', 'DELETE', TRUE);

-- 将日历管理权限分配给管理员角色（假设管理员角色ID为1）
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE name IN ('calendar:view', 'calendar:add', 'calendar:edit', 'calendar:delete');

-- 将日历菜单权限关联
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 16, id FROM permission WHERE name = 'calendar:view';

-- 添加工资日管理菜单到 OA办公模块
INSERT INTO menu (id, name, path, component, parent_id, icon, sort_order, is_hidden, description)
VALUES (17, '工资日管理', '/payday-management', 'PaydayManagement', 2, 'Money', 8, FALSE, '工资日管理');

-- 添加工资日管理权限
INSERT INTO permission (name, description, permission_type, resource, action, is_active)
VALUES 
('payday:view', '查看工资日', 'FUNCTIONAL', 'PAYDAY', 'READ', TRUE),
('payday:add', '添加工资日', 'FUNCTIONAL', 'PAYDAY', 'CREATE', TRUE),
('payday:edit', '编辑工资日', 'FUNCTIONAL', 'PAYDAY', 'UPDATE', TRUE),
('payday:delete', '删除工资日', 'FUNCTIONAL', 'PAYDAY', 'DELETE', TRUE),
('payday:generate', '生成工资日', 'FUNCTIONAL', 'PAYDAY', 'GENERATE', TRUE);

-- 将工资日管理权限分配给管理员角色（假设管理员角色ID为1）
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE name IN ('payday:view', 'payday:add', 'payday:edit', 'payday:delete', 'payday:generate');

-- 将工资日菜单权限关联
INSERT INTO menu_permission (menu_id, permission_id)
SELECT 17, id FROM permission WHERE name = 'payday:view';
