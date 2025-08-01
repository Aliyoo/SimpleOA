-- V13: 更新费用类别说明
-- 该迁移脚本用于记录费用类别的更新，实际的费用类别在应用层控制

-- 创建费用类别参考表（仅用于文档记录）
CREATE TABLE IF NOT EXISTS expense_category_reference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    active BOOLEAN DEFAULT TRUE,
    display_order INT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入新的费用类别参考数据
INSERT INTO expense_category_reference (category_name, description, display_order) VALUES
('劳务费', '支付给个人或单位的劳务报酬', 1),
('房屋费', '房租、物业费等房屋相关费用', 2),
('差旅费', '出差期间的交通、住宿、补贴等费用', 3),
('交通费', '日常交通费用，如打车、公交等', 4),
('办公费', '办公用品、文具等费用', 5),
('通信费', '电话费、网络费等通信费用', 6),
('车辆费', '车辆使用、维护、保养等费用', 7),
('货运费', '物流、快递、货运等费用', 8),
('物料消耗费', '生产或项目所需的物料消耗', 9),
('评审验收费', '项目评审、验收等相关费用', 10),
('加班餐费', '加班期间的餐饮费用', 11),
('质保维护费(不含人工)', '质保期内的维护费用，不包含人工成本', 12),
('业务招待费', '业务招待、商务宴请等费用', 13);

-- 更新系统配置，记录费用类别版本
INSERT INTO system_config (config_key, config_value, description, created_time)
VALUES ('expense_category_version', 'v2.0', '费用类别版本：新增13种类别', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE 
    config_value = 'v2.0',
    description = '费用类别版本：新增13种类别',
    updated_time = CURRENT_TIMESTAMP;

-- 添加注释说明
COMMENT ON TABLE expense_category_reference IS '费用类别参考表，仅用于文档记录，实际使用在应用层控制';
