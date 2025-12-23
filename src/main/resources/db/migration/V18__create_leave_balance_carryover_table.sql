-- 创建假期结转记录表和增强假期余额表
-- 支持年假结转功能和更详细的余额管理

-- 创建假期结转记录表
CREATE TABLE leave_balance_carryover (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    from_year INT NOT NULL COMMENT '来源年份',
    to_year INT NOT NULL COMMENT '目标年份',
    leave_type VARCHAR(50) NOT NULL COMMENT '假期类型',
    carryover_days DECIMAL(4,1) NOT NULL COMMENT '结转天数',
    used_days DECIMAL(4,1) DEFAULT 0 COMMENT '已使用结转天数',
    is_used BOOLEAN DEFAULT FALSE COMMENT '是否已使用完',
    expire_date DATE COMMENT '过期日期（结转年假通常到次年3月31日过期）',
    carryover_rule_id BIGINT COMMENT '应用的结转规则ID',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_user_from_to_year_type (user_id, from_year, to_year, leave_type),
    INDEX idx_user_year (user_id, to_year),
    INDEX idx_expire_date (expire_date),
    INDEX idx_is_used (is_used),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='假期结转记录表';

-- 添加外键约束
ALTER TABLE leave_balance_carryover
ADD CONSTRAINT fk_carryover_user
FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

ALTER TABLE leave_balance_carryover
ADD CONSTRAINT fk_carryover_created_by
FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE SET NULL;

ALTER TABLE leave_balance_carryover
ADD CONSTRAINT fk_carryover_updated_by
FOREIGN KEY (updated_by) REFERENCES user(id) ON DELETE SET NULL;

-- 添加结转天数约束
ALTER TABLE leave_balance_carryover
ADD CONSTRAINT chk_carryover_days_positive
CHECK (carryover_days > 0);

-- 添加已使用天数约束
ALTER TABLE leave_balance_carryover
ADD CONSTRAINT chk_used_days_range
CHECK (used_days >= 0 AND used_days <= carryover_days);

-- 添加年份约束
ALTER TABLE leave_balance_carryover
ADD CONSTRAINT chk_year_range
CHECK (to_year = from_year + 1);

-- 增强现有假期余额表
-- 首先检查字段是否存在
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'leave_balance'
     AND COLUMN_NAME = 'seniority_years') > 0,
    'SELECT "seniority_years字段已存在" as message',
    'ALTER TABLE leave_balance ADD COLUMN seniority_years DECIMAL(5,1) COMMENT "计算时工龄（年）"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'leave_balance'
     AND COLUMN_NAME = 'applied_rule_id') > 0,
    'SELECT "applied_rule_id字段已存在" as message',
    'ALTER TABLE leave_balance ADD COLUMN applied_rule_id BIGINT COMMENT "应用的年假规则ID"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'leave_balance'
     AND COLUMN_NAME = 'is_carryover') > 0,
    'SELECT "is_carryover字段已存在" as message',
    'ALTER TABLE leave_balance ADD COLUMN is_carryover BOOLEAN DEFAULT FALSE COMMENT "是否为结转余额"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'leave_balance'
     AND COLUMN_NAME = 'carryover_from_year') > 0,
    'SELECT "carryover_from_year字段已存在" as message',
    'ALTER TABLE leave_balance ADD COLUMN carryover_from_year INT COMMENT "结转来源年份"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'leave_balance'
     AND INDEX_NAME = 'idx_user_year_type') > 0,
    'SELECT "idx_user_year_type索引已存在" as message',
    'CREATE INDEX idx_user_year_type ON leave_balance(user_id, year, leave_type)'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建余额调整记录表（用于手动调整的审计）
CREATE TABLE leave_balance_adjustment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    year INT NOT NULL COMMENT '年份',
    leave_type VARCHAR(50) NOT NULL COMMENT '假期类型',
    adjustment_type VARCHAR(20) NOT NULL COMMENT '调整类型：INCREASE, DECREASE',
    adjustment_days DECIMAL(4,1) NOT NULL COMMENT '调整天数',
    before_total_days DECIMAL(4,1) COMMENT '调整前总天数',
    after_total_days DECIMAL(4,1) COMMENT '调整后总天数',
    before_remaining_days DECIMAL(4,1) COMMENT '调整前剩余天数',
    after_remaining_days DECIMAL(4,1) COMMENT '调整后剩余天数',
    reason VARCHAR(255) NOT NULL COMMENT '调整原因',
    approved_by BIGINT COMMENT '审批人ID',
    approved_at TIMESTAMP NULL COMMENT '审批时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_user_year_type (user_id, year, leave_type),
    INDEX idx_adjustment_type (adjustment_type),
    INDEX idx_created_at (created_at),
    INDEX idx_approved_by (approved_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='假期余额调整记录表';

-- 添加外键约束
ALTER TABLE leave_balance_adjustment
ADD CONSTRAINT fk_adjustment_user
FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

ALTER TABLE leave_balance_adjustment
ADD CONSTRAINT fk_adjustment_created_by
FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE SET NULL;

ALTER TABLE leave_balance_adjustment
ADD CONSTRAINT fk_adjustment_approved_by
FOREIGN KEY (approved_by) REFERENCES user(id) ON DELETE SET NULL;

-- 添加调整类型约束
ALTER TABLE leave_balance_adjustment
ADD CONSTRAINT chk_adjustment_type
CHECK (adjustment_type IN ('INCREASE', 'DECREASE'));

-- 添加调整天数约束
ALTER TABLE leave_balance_adjustment
ADD CONSTRAINT chk_adjustment_days
CHECK (adjustment_days != 0);

-- 添加年份约束
ALTER TABLE leave_balance_adjustment
ADD CONSTRAINT chk_adjustment_year
CHECK (year > 2000 AND year < 2100);

-- 创建假期配置表（系统级配置）
CREATE TABLE leave_system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    config_type VARCHAR(20) DEFAULT 'STRING' COMMENT '配置类型：STRING, INTEGER, BOOLEAN, DECIMAL',
    description VARCHAR(255) COMMENT '配置描述',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否为系统配置（系统配置不可删除）',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_config_key (config_key),
    INDEX idx_config_type (config_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='假期系统配置表';

-- 添加外键约束
ALTER TABLE leave_system_config
ADD CONSTRAINT fk_config_created_by
FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE SET NULL;

ALTER TABLE leave_system_config
ADD CONSTRAINT fk_config_updated_by
FOREIGN KEY (updated_by) REFERENCES user(id) ON DELETE SET NULL;

-- 插入默认系统配置
INSERT INTO leave_system_config (config_key, config_value, config_type, description, is_system) VALUES
('leave.carryover.enabled', 'true', 'BOOLEAN', '是否启用假期结转功能', TRUE),
('leave.carryover.expire_months', '3', 'INTEGER', '结转假期过期月数（年假结转后3个月过期）', TRUE),
('leave.carryover.max_days', '5', 'INTEGER', '最大结转天数限制', TRUE),
('leave.annual.auto_init', 'true', 'BOOLEAN', '是否自动初始化年假余额', TRUE),
('leave.annual.init_month', '1', 'INTEGER', '年假初始化月份（1表示1月）', TRUE),
('leave.annual.init_day', '1', 'INTEGER', '年假初始化日期', TRUE),
('leave.balance.warning_threshold', '1', 'INTEGER', '假期余额警告阈值', TRUE),
('leave.adjustment.max_days', '30', 'INTEGER', '单次调整最大天数限制', TRUE),
('leave.adjustment.frequency_days', '30', 'INTEGER', '同类型假期调整频率限制（天）', TRUE);

-- 验证表创建成功
SELECT '假期结转和配置表创建完成' as message;
SHOW TABLES LIKE 'leave_%';
SHOW TABLES LIKE 'annual_%';
SHOW TABLES LIKE 'leave_balance_carryover';
SHOW TABLES LIKE 'leave_balance_adjustment';
SHOW TABLES LIKE 'leave_system_config';