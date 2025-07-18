-- 修复business_trip_request表结构
-- 添加缺失的字段以匹配实体类

-- 添加start_time和end_time字段
ALTER TABLE business_trip_request 
ADD COLUMN start_time DATETIME COMMENT '开始时间',
ADD COLUMN end_time DATETIME COMMENT '结束时间',
ADD COLUMN days INT COMMENT '出差天数',
ADD COLUMN comment TEXT COMMENT '评论';

-- 如果有现有数据，将start_date和end_date的值复制到start_time和end_time
UPDATE business_trip_request 
SET start_time = CONCAT(start_date, ' 09:00:00'),
    end_time = CONCAT(end_date, ' 18:00:00')
WHERE start_date IS NOT NULL AND end_date IS NOT NULL;

-- 插入一些测试数据
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
    created_at,
    updated_at
) VALUES 
(
    '上海', 
    '2024-01-15', 
    '2024-01-17', 
    '2024-01-15 09:00:00', 
    '2024-01-17 18:00:00', 
    3, 
    '客户会议', 
    2500.00, 
    'APPROVED', 
    '出差顺利', 
    1,
    '2024-01-15 08:00:00',
    '2024-01-15 08:00:00'
),
(
    '北京', 
    '2024-02-20', 
    '2024-02-22', 
    '2024-02-20 08:00:00', 
    '2024-02-22 20:00:00', 
    3, 
    '技术培训', 
    3000.00, 
    'APPROVED', 
    '培训完成', 
    2,
    '2024-02-20 07:00:00',
    '2024-02-20 07:00:00'
),
(
    '深圳', 
    '2024-03-10', 
    '2024-03-12', 
    '2024-03-10 10:00:00', 
    '2024-03-12 16:00:00', 
    3, 
    '项目调研', 
    2800.00, 
    'PENDING', 
    '待审批', 
    1,
    '2024-03-10 09:00:00',
    '2024-03-10 09:00:00'
),
(
    '广州', 
    '2024-04-05', 
    '2024-04-07', 
    '2024-04-05 09:30:00', 
    '2024-04-07 17:30:00', 
    3, 
    '市场调研', 
    2200.00, 
    'APPROVED', 
    '调研顺利', 
    2,
    '2024-04-05 08:30:00',
    '2024-04-05 08:30:00'
),
(
    '杭州', 
    '2024-05-15', 
    '2024-05-16', 
    '2024-05-15 08:00:00', 
    '2024-05-16 19:00:00', 
    2, 
    '合作洽谈', 
    1500.00, 
    'APPROVED', 
    '合作成功', 
    1,
    '2024-05-15 07:00:00',
    '2024-05-15 07:00:00'
);
