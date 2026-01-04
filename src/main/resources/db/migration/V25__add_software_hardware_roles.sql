-- =====================================================
-- V25: 添加软件/硬件侧角色和权限
-- =====================================================

-- 1. 新增角色
INSERT INTO role (name, description) VALUES
('ROLE_SOFTWARE_STAFF', '软件侧人员'),
('ROLE_HARDWARE_STAFF', '硬件侧人员'),
('ROLE_SOFTWARE_LEADER', '软件侧领导'),
('ROLE_HARDWARE_LEADER', '硬件侧领导');

-- 2. 新增报销审查权限
INSERT INTO permission (name, description, permission_type, resource, action)
SELECT 'reimbursement:finance_review', '财务审查报销', 'FUNCTIONAL', 'reimbursement', 'finance_review'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE name = 'reimbursement:finance_review');

-- 3. 为软件侧人员分配基本权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'ROLE_SOFTWARE_STAFF'
AND p.name IN ('reimbursement:view', 'reimbursement:add', 'reimbursement:edit', 'reimbursement:delete')
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 4. 为硬件侧人员分配基本权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'ROLE_HARDWARE_STAFF'
AND p.name IN ('reimbursement:view', 'reimbursement:add', 'reimbursement:edit', 'reimbursement:delete')
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 5. 为软件侧领导分配审批权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'ROLE_SOFTWARE_LEADER'
AND p.name IN ('reimbursement:view', 'reimbursement:approve')
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 6. 为硬件侧领导分配审批权限
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'ROLE_HARDWARE_LEADER'
AND p.name IN ('reimbursement:view', 'reimbursement:approve')
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 7. 为财务角色分配审查权限(保留审批权限用于向后兼容)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'ROLE_FINANCE'
AND p.name IN ('reimbursement:view', 'reimbursement:finance_review')
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 8. 为管理员分配所有权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission
WHERE name LIKE 'reimbursement:%'
AND id NOT IN (SELECT permission_id FROM role_permission WHERE role_id = 1);

-- 9. 创建示例用户(可选,用于测试)
-- 注意:密码均为 123456 的MD5
INSERT INTO user (username, password, email, real_name, department, employee_number, enabled)
VALUES
('soft_staff', 'e10adc3949ba59abbe56e057f20f883e', 'soft_staff@example.com', '软件人员', '软件部', 'SOFT001', TRUE),
('hard_staff', 'e10adc3949ba59abbe56e057f20f883e', 'hard_staff@example.com', '硬件人员', '硬件部', 'HARD001', TRUE),
('soft_leader', 'e10adc3949ba59abbe56e057f20f883e', 'soft_leader@example.com', '软件领导', '软件部', 'SOFTLEAD001', TRUE),
('hard_leader', 'e10adc3949ba59abbe56e057f20f883e', 'hard_leader@example.com', '硬件领导', '硬件部', 'HARDLEAD001', TRUE)
ON DUPLICATE KEY UPDATE email=VALUES(email);

-- 10. 为示例用户分配角色(使用role_user关联表)
-- 注意：根据实际DDL，表名应为role_user，主键为(role_id, user_id)
-- 检查是否存在role_user表，如果不存在则创建
CREATE TABLE IF NOT EXISTS role_user (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, user_id),
    CONSTRAINT FK_ROLE_USER_USER FOREIGN KEY (user_id) REFERENCES user(id),
    CONSTRAINT FK_ROLE_USER_ROLE FOREIGN KEY (role_id) REFERENCES role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 为示例用户分配角色
INSERT INTO role_user (user_id, role_id)
SELECT u.id, r.id
FROM user u, role r
WHERE (u.username = 'soft_staff' AND r.name = 'ROLE_SOFTWARE_STAFF')
   OR (u.username = 'hard_staff' AND r.name = 'ROLE_HARDWARE_STAFF')
   OR (u.username = 'soft_leader' AND r.name = 'ROLE_SOFTWARE_LEADER')
   OR (u.username = 'hard_leader' AND r.name = 'ROLE_HARDWARE_LEADER')
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

-- 11. 查找OA办公菜单的ID
-- 添加财务审查菜单（注意：路径不包含 /oa 前缀，因为路由配置在根级别）
INSERT INTO menu (name, path, component, parent_id, icon, sort_order, is_hidden, description)
SELECT '财务审查', '/reimbursement/finance-review', 'oa/ReimbursementFinanceReview',
       (SELECT id FROM menu WHERE name = 'OA办公' LIMIT 1),
       'Money', 6, FALSE, '财务审查报销申请'
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE name = '财务审查');

-- 12. 为财务审查菜单分配权限
INSERT INTO menu_permission (menu_id, permission_id)
SELECT m.id, p.id
FROM menu m, permission p
WHERE m.name = '财务审查'
AND p.name = 'reimbursement:finance_review'
AND NOT EXISTS (
    SELECT 1 FROM menu_permission mp
    WHERE mp.menu_id = m.id AND mp.permission_id = p.id
);

-- 13. 为财务角色分配财务审查权限(已在第7步完成，这里只需确保)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.name = 'ROLE_FINANCE'
AND p.name = 'reimbursement:finance_review'
AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
