-- 修复leave_request表结构
ALTER TABLE leave_request
MODIFY COLUMN start_date DATETIME,
MODIFY COLUMN end_date DATETIME;

-- 如果表不存在，创建它
CREATE TABLE IF NOT EXISTS leave_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_type VARCHAR(50),
    start_date DATETIME,
    end_date DATETIME,
    reason TEXT,
    status VARCHAR(20) DEFAULT '待审批',
    comment TEXT,
    applicant_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (applicant_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
