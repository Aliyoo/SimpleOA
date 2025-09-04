-- 创建请假余额表
CREATE TABLE IF NOT EXISTS leave_balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '请假余额ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    leave_type VARCHAR(50) NOT NULL COMMENT '请假类型',
    year INT NOT NULL COMMENT '年份',
    total_days INT NOT NULL DEFAULT 0 COMMENT '总天数',
    used_days INT NOT NULL DEFAULT 0 COMMENT '已使用天数',
    remaining_days INT NOT NULL DEFAULT 0 COMMENT '剩余天数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_type_year (user_id, leave_type, year),
    INDEX idx_user_year (user_id, year),
    INDEX idx_leave_type (leave_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '请假余额表';

-- 为现有用户初始化当年的请假余额
INSERT INTO leave_balance (user_id, leave_type, year, total_days, used_days, remaining_days)
SELECT
    u.id,
    'ANNUAL_LEAVE',
    YEAR(CURDATE()),
    15, -- 年假默认15天
    0,
    15
FROM user u
WHERE u.enabled = TRUE
AND NOT EXISTS (
    SELECT 1 FROM leave_balance lb
    WHERE lb.user_id = u.id
    AND lb.leave_type = 'ANNUAL_LEAVE'
    AND lb.year = YEAR(CURDATE())
);

INSERT INTO leave_balance (user_id, leave_type, year, total_days, used_days, remaining_days)
SELECT
    u.id,
    'SICK_LEAVE',
    YEAR(CURDATE()),
    10, -- 病假默认10天
    0,
    10
FROM user u
WHERE u.enabled = TRUE
AND NOT EXISTS (
    SELECT 1 FROM leave_balance lb
    WHERE lb.user_id = u.id
    AND lb.leave_type = 'SICK_LEAVE'
    AND lb.year = YEAR(CURDATE())
);

INSERT INTO leave_balance (user_id, leave_type, year, total_days, used_days, remaining_days)
SELECT
    u.id,
    'PERSONAL_LEAVE',
    YEAR(CURDATE()),
    5, -- 事假默认5天
    0,
    5
FROM user u
WHERE u.enabled = TRUE
AND NOT EXISTS (
    SELECT 1 FROM leave_balance lb
    WHERE lb.user_id = u.id
    AND lb.leave_type = 'PERSONAL_LEAVE'
    AND lb.year = YEAR(CURDATE())
);

-- 其他假期类型通常按需申请，暂不初始化余额
