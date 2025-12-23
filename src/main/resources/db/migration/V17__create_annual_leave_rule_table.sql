-- 创建年假规则配置表
-- 支持基于工龄的阶梯式年假计算规则

-- 创建年假规则配置表
CREATE TABLE annual_leave_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    min_years DECIMAL(5,1) NOT NULL COMMENT '最小工龄（年），支持小数',
    max_years DECIMAL(5,1) COMMENT '最大工龄（年），支持小数，NULL表示无上限',
    annual_days INT NOT NULL COMMENT '年假天数',
    carryover_limit INT DEFAULT 5 COMMENT '年假结转限制天数',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    priority INT DEFAULT 0 COMMENT '优先级，数字越大优先级越高，用于处理重叠区间',
    description VARCHAR(255) COMMENT '规则描述',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_active_priority (is_active, priority),
    INDEX idx_seniority_range (min_years, max_years),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年假规则配置表';

-- 添加外键约束
ALTER TABLE annual_leave_rule
ADD CONSTRAINT fk_annual_rule_created_by
FOREIGN KEY (created_by) REFERENCES user(id) ON DELETE SET NULL;

ALTER TABLE annual_leave_rule
ADD CONSTRAINT fk_annual_rule_updated_by
FOREIGN KEY (updated_by) REFERENCES user(id) ON DELETE SET NULL;

-- 添加工龄范围约束
ALTER TABLE annual_leave_rule
ADD CONSTRAINT chk_seniority_range
CHECK (max_years IS NULL OR min_years < max_years);

-- 添加年假天数约束
ALTER TABLE annual_leave_rule
ADD CONSTRAINT chk_annual_days
CHECK (annual_days > 0 AND annual_days <= 30);

-- 添加结转限制约束
ALTER TABLE annual_leave_rule
ADD CONSTRAINT chk_carryover_limit
CHECK (carryover_limit >= 0 AND carryover_limit <= 30);

-- 添加优先级约束
ALTER TABLE annual_leave_rule
ADD CONSTRAINT chk_priority
CHECK (priority >= 0);

-- 插入默认年假规则
INSERT INTO annual_leave_rule (min_years, max_years, annual_days, carryover_limit, priority, description) VALUES
(0, 1, 5, 2, 1, '工龄0-1年，年假5天'),
(1, 3, 10, 5, 2, '工龄1-3年，年假10天'),
(3, 5, 15, 5, 3, '工龄3-5年，年假15天'),
(5, 10, 15, 5, 4, '工龄5-10年，年假15天'),
(10, 15, 20, 5, 5, '工龄10-15年，年假20天'),
(15, NULL, 25, 5, 6, '工龄15年以上，年假25天');

-- 创建年假规则变更记录表（用于审计）
CREATE TABLE annual_leave_rule_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL COMMENT '年假规则ID',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型：CREATE, UPDATE, DELETE',
    min_years DECIMAL(5,1) COMMENT '变更前最小工龄',
    max_years DECIMAL(5,1) COMMENT '变更前最大工龄',
    annual_days INT COMMENT '变更前年假天数',
    carryover_limit INT COMMENT '变更前结转限制',
    is_active BOOLEAN COMMENT '变更前状态',
    description VARCHAR(255) COMMENT '变更前描述',
    operated_by BIGINT NOT NULL COMMENT '操作人ID',
    operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remarks TEXT COMMENT '变更备注',

    INDEX idx_rule_id (rule_id),
    INDEX idx_operated_at (operated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年假规则变更历史表';

-- 添加外键约束
ALTER TABLE annual_leave_rule_history
ADD CONSTRAINT fk_rule_history_rule
FOREIGN KEY (rule_id) REFERENCES annual_leave_rule(id) ON DELETE CASCADE;

ALTER TABLE annual_leave_rule_history
ADD CONSTRAINT fk_rule_history_operated_by
FOREIGN KEY (operated_by) REFERENCES user(id) ON DELETE SET NULL;

-- 验证表创建成功
SELECT '年假规则配置表创建完成' as message;
SHOW TABLES LIKE 'annual_leave_rule%';
DESCRIBE annual_leave_rule;