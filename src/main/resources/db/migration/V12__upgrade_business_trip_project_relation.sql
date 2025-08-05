-- (逻辑已废弃)升级出差管理表结构，添加项目关联和二级审批支持
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