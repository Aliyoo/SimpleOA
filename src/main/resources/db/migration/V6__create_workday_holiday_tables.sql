-- V6: 创建工作日和节假日表
-- 作者：工作日管理系统
-- 日期：2024-07-21

-- 删除旧的payday表（如果存在）
DROP TABLE IF EXISTS payday;

-- 创建节假日表
CREATE TABLE IF NOT EXISTS holiday (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '节假日ID，主键，自增',
    date DATE NOT NULL COMMENT '节假日日期，非空',
    name VARCHAR(100) NOT NULL COMMENT '节假日名称，非空',
    type VARCHAR(50) DEFAULT 'PUBLIC' COMMENT '节假日类型：PUBLIC(法定节假日), COMPANY(公司节假日), OTHER(其他)',
    description VARCHAR(255) COMMENT '节假日描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    UNIQUE KEY uk_holiday_date (date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='节假日表';

-- 创建工作日表
CREATE TABLE IF NOT EXISTS workday (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '工作日ID，主键，自增',
    date DATE NOT NULL COMMENT '工作日日期，非空',
    description VARCHAR(255) COMMENT '工作日描述，如调休、补班等',
    work_type VARCHAR(20) DEFAULT 'NORMAL' COMMENT '工作日类型：NORMAL(正常工作日), MAKEUP(调休补班), OVERTIME(加班日)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    UNIQUE KEY uk_workday_date (date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='工作日表';

-- 插入一些常见的节假日数据示例（2024年）
INSERT IGNORE INTO holiday (date, name, type, description) VALUES
('2024-01-01', '元旦', 'PUBLIC', '法定节假日'),
('2024-02-10', '春节', 'PUBLIC', '法定节假日'),
('2024-02-11', '春节', 'PUBLIC', '法定节假日'),
('2024-02-12', '春节', 'PUBLIC', '法定节假日'),
('2024-02-13', '春节', 'PUBLIC', '法定节假日'),
('2024-02-14', '春节', 'PUBLIC', '法定节假日'),
('2024-02-15', '春节', 'PUBLIC', '法定节假日'),
('2024-02-16', '春节', 'PUBLIC', '法定节假日'),
('2024-04-04', '清明节', 'PUBLIC', '法定节假日'),
('2024-04-05', '清明节', 'PUBLIC', '法定节假日'),
('2024-04-06', '清明节', 'PUBLIC', '法定节假日'),
('2024-05-01', '劳动节', 'PUBLIC', '法定节假日'),
('2024-05-02', '劳动节', 'PUBLIC', '法定节假日'),
('2024-05-03', '劳动节', 'PUBLIC', '法定节假日'),
('2024-06-10', '端午节', 'PUBLIC', '法定节假日'),
('2024-09-15', '中秋节', 'PUBLIC', '法定节假日'),
('2024-09-16', '中秋节', 'PUBLIC', '法定节假日'),
('2024-09-17', '中秋节', 'PUBLIC', '法定节假日'),
('2024-10-01', '国庆节', 'PUBLIC', '法定节假日'),
('2024-10-02', '国庆节', 'PUBLIC', '法定节假日'),
('2024-10-03', '国庆节', 'PUBLIC', '法定节假日'),
('2024-10-04', '国庆节', 'PUBLIC', '法定节假日'),
('2024-10-05', '国庆节', 'PUBLIC', '法定节假日'),
('2024-10-06', '国庆节', 'PUBLIC', '法定节假日'),
('2024-10-07', '国庆节', 'PUBLIC', '法定节假日');

-- 插入一些调休补班的工作日示例（2024年）
INSERT IGNORE INTO workday (date, description, work_type) VALUES
('2024-02-04', '春节调休补班', 'MAKEUP'),
('2024-02-18', '春节调休补班', 'MAKEUP'),
('2024-04-07', '清明节调休补班', 'MAKEUP'),
('2024-04-28', '劳动节调休补班', 'MAKEUP'),
('2024-05-11', '劳动节调休补班', 'MAKEUP'),
('2024-09-14', '中秋节调休补班', 'MAKEUP'),
('2024-09-29', '国庆节调休补班', 'MAKEUP'),
('2024-10-12', '国庆节调休补班', 'MAKEUP');

-- 2025年数据
INSERT IGNORE INTO holiday (date, name, type, description) VALUES
('2025-01-01', '元旦', 'PUBLIC', '法定节假日'),
('2025-01-28', '春节', 'PUBLIC', '法定节假日'),
('2025-01-29', '春节', 'PUBLIC', '法定节假日'),
('2025-01-30', '春节', 'PUBLIC', '法定节假日'),
('2025-01-31', '春节', 'PUBLIC', '法定节假日'),
('2025-02-01', '春节', 'PUBLIC', '法定节假日'),
('2025-02-02', '春节', 'PUBLIC', '法定节假日'),
('2025-02-03', '春节', 'PUBLIC', '法定节假日'),
('2025-04-05', '清明节', 'PUBLIC', '法定节假日'),
('2025-04-06', '清明节', 'PUBLIC', '法定节假日'),
('2025-04-07', '清明节', 'PUBLIC', '法定节假日'),
('2025-05-01', '劳动节', 'PUBLIC', '法定节假日'),
('2025-05-02', '劳动节', 'PUBLIC', '法定节假日'),
('2025-05-03', '劳动节', 'PUBLIC', '法定节假日'),
('2025-05-31', '端午节', 'PUBLIC', '法定节假日'),
('2025-06-01', '端午节', 'PUBLIC', '法定节假日'),
('2025-06-02', '端午节', 'PUBLIC', '法定节假日'),
('2025-10-01', '国庆节', 'PUBLIC', '法定节假日'),
('2025-10-02', '国庆节', 'PUBLIC', '法定节假日'),
('2025-10-03', '国庆节', 'PUBLIC', '法定节假日'),
('2025-10-04', '国庆节', 'PUBLIC', '法定节假日'),
('2025-10-05', '国庆节', 'PUBLIC', '法定节假日'),
('2025-10-06', '国庆节', 'PUBLIC', '法定节假日'),
('2025-10-07', '国庆节', 'PUBLIC', '法定节假日');

-- 2025年调休补班工作日
INSERT IGNORE INTO workday (date, description, work_type) VALUES
('2025-01-26', '春节调休补班', 'MAKEUP'),
('2025-02-08', '春节调休补班', 'MAKEUP'),
('2025-04-13', '清明节调休补班', 'MAKEUP'),
('2025-04-27', '劳动节调休补班', 'MAKEUP'),
('2025-09-28', '国庆节调休补班', 'MAKEUP'),
('2025-10-11', '国庆节调休补班', 'MAKEUP');
