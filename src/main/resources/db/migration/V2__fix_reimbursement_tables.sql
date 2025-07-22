-- 修复报销申请表结构以匹配Java实体类
-- 删除旧的报销申请表
DROP TABLE IF EXISTS reimbursement_request_attachment;
DROP TABLE IF EXISTS reimbursement_request;

-- 创建新的报销申请表
CREATE TABLE IF NOT EXISTS reimbursement_request
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '报销申请ID，主键，自增',
    applicant_id BIGINT         NOT NULL COMMENT '申请人ID，非空',
    project_id   BIGINT COMMENT '项目ID，可空',
    title        VARCHAR(200)   NOT NULL COMMENT '报销标题，非空',
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总金额，非空',
    status       VARCHAR(50)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态，默认为DRAFT',
    comment      VARCHAR(500) COMMENT '审批意见',
    create_time  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    update_time  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (applicant_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='报销申请表';

-- 创建报销费用明细表
CREATE TABLE IF NOT EXISTS reimbursement_item
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '明细ID，主键，自增',
    reimbursement_request_id BIGINT         NOT NULL COMMENT '报销申请ID，非空',
    expense_date            DATE           NOT NULL COMMENT '费用日期，非空',
    item_category           VARCHAR(50)    NOT NULL COMMENT '费用类别，非空',
    description             VARCHAR(500) COMMENT '费用描述',
    amount                  DECIMAL(10, 2) NOT NULL COMMENT '金额，非空',
    FOREIGN KEY (reimbursement_request_id) REFERENCES reimbursement_request (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='报销费用明细表';

-- 创建报销申请附件表
CREATE TABLE IF NOT EXISTS reimbursement_attachments
(
    request_id      BIGINT       NOT NULL COMMENT '报销申请ID',
    attachment_path VARCHAR(500) NOT NULL COMMENT '附件路径',
    PRIMARY KEY (request_id, attachment_path),
    FOREIGN KEY (request_id) REFERENCES reimbursement_request (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='报销申请附件表';

-- 更新审批流程表的外键约束
ALTER TABLE approval_flow ADD CONSTRAINT approval_flow_ibfk_6
    FOREIGN KEY (reimbursement_request_id) REFERENCES reimbursement_request (id) ON DELETE CASCADE;
