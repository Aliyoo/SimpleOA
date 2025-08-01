-- 升级出差管理表结构，添加项目关联和二级审批支持
-- V12__upgrade_business_trip_project_relation.sql

-- 添加项目关联字段
ALTER TABLE business_trip_request 
ADD COLUMN project_id BIGINT COMMENT '关联项目ID',
ADD COLUMN create_time DATETIME COMMENT '创建时间',
ADD COLUMN update_time DATETIME COMMENT '更新时间';

-- 添加外键约束
ALTER TABLE business_trip_request 
ADD CONSTRAINT fk_business_trip_project 
FOREIGN KEY (project_id) REFERENCES project(id);

-- 更新现有记录的时间戳
UPDATE business_trip_request 
SET create_time = IFNULL(created_at, NOW()),
    update_time = IFNULL(updated_at, NOW())
WHERE create_time IS NULL;

-- 更新状态字段，支持新的枚举值
-- 原有的PENDING状态更新为PENDING_MANAGER_APPROVAL
UPDATE business_trip_request 
SET status = 'PENDING_MANAGER_APPROVAL' 
WHERE status = 'PENDING';

-- 为现有记录随机分配项目（仅用于测试数据）
UPDATE business_trip_request bt
JOIN (
    SELECT id FROM project ORDER BY RAND() LIMIT 1
) p
SET bt.project_id = p.id
WHERE bt.project_id IS NULL;

-- 插入一些测试数据，展示新的二级审批流程
INSERT INTO business_trip_request (
    destination, 
    start_date, 
    end_date, 
    start_time, 
    end_time, 
    days, 
    purpose, 
    estimated_cost, 
    status, 
    comment, 
    applicant_id,
    project_id,
    create_time,
    update_time
) VALUES 
-- 草稿状态
(
    '成都', 
    '2025-02-15', 
    '2025-02-17', 
    '2025-02-15 09:00:00', 
    '2025-02-17 18:00:00', 
    3, 
    '项目实施现场支持', 
    3200.00, 
    'DRAFT', 
    '草稿待提交', 
    1,
    (SELECT id FROM project LIMIT 1),
    '2025-02-10 08:00:00',
    '2025-02-10 08:00:00'
),
-- 待项目经理审批
(
    '西安', 
    '2025-02-20', 
    '2025-02-22', 
    '2025-02-20 08:30:00', 
    '2025-02-22 17:30:00', 
    3, 
    '客户需求调研', 
    2800.00, 
    'PENDING_MANAGER_APPROVAL', 
    '待项目经理审批', 
    2,
    (SELECT id FROM project LIMIT 1),
    '2025-02-12 09:00:00',
    '2025-02-12 09:00:00'
),
-- 待财务审批
(
    '重庆', 
    '2025-02-25', 
    '2025-02-26', 
    '2025-02-25 10:00:00', 
    '2025-02-26 16:00:00', 
    2, 
    '技术交流会议', 
    1800.00, 
    'PENDING_FINANCE_APPROVAL', 
    '项目经理已审批，待财务审批', 
    1,
    (SELECT id FROM project ORDER BY id LIMIT 1 OFFSET 1),
    '2025-02-14 10:00:00',
    '2025-02-15 14:30:00'
);

-- 创建审批流程记录，对应上面的出差申请
-- 注意：这些INSERT语句依赖于approval_flow表的存在
INSERT INTO approval_flow (
    request_type,
    business_trip_request_id,
    approver_id,
    status,
    comment,
    create_time,
    update_time
) VALUES
-- 为待项目经理审批的出差申请创建审批流程
(
    'BUSINESS_TRIP',
    (SELECT id FROM business_trip_request WHERE destination = '西安' AND status = 'PENDING_MANAGER_APPROVAL'),
    (SELECT manager_id FROM project WHERE id = (SELECT project_id FROM business_trip_request WHERE destination = '西安' LIMIT 1)),
    'PENDING',
    '待项目经理审批',
    '2025-02-12 09:05:00',
    '2025-02-12 09:05:00'
),
-- 为待财务审批的出差申请创建审批流程
(
    'BUSINESS_TRIP',
    (SELECT id FROM business_trip_request WHERE destination = '重庆' AND status = 'PENDING_FINANCE_APPROVAL'),
    (SELECT id FROM user WHERE username = 'finance' LIMIT 1), -- 假设有财务用户
    'PENDING',
    '待财务审批',
    '2025-02-15 14:35:00',
    '2025-02-15 14:35:00'
);