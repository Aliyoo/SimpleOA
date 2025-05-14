-- 创建数据库
CREATE DATABASE IF NOT EXISTS simpleoa_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 创建simpleoa_dev数据库

-- 使用数据库
USE simpleoa_dev;
-- 使用simpleoa_dev数据库

-- 创建角色表
CREATE TABLE IF NOT EXISTS role
( -- 创建role表
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID，主键，自增',
    name        VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称，唯一且非空',
    description VARCHAR(200) COMMENT '角色描述'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色表';

-- 创建职位表
CREATE TABLE IF NOT EXISTS position
( -- 创建position表
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '职位ID，主键，自增',
    name        VARCHAR(50) NOT NULL UNIQUE COMMENT '职位名称，唯一且非空',
    description VARCHAR(200) COMMENT '职位描述'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='职位表';

-- 创建用户表
DROP TABLE IF EXISTS user; -- 删除已存在的user表
CREATE TABLE IF NOT EXISTS user
( -- 创建user表
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID，主键，自增',
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名，唯一且非空',
    password        VARCHAR(100) NOT NULL COMMENT '密码，非空',
    email           VARCHAR(100) NOT NULL COMMENT '邮箱，非空',
    phone_number    VARCHAR(20) COMMENT '手机号',
    department      VARCHAR(50) COMMENT '部门',
    employee_number VARCHAR(50) UNIQUE COMMENT '员工编号，唯一',
    hire_date       DATE COMMENT '入职日期',
    role_id         BIGINT COMMENT '角色ID',
    position_id     BIGINT COMMENT '职位ID',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    enabled         BOOLEAN   DEFAULT TRUE COMMENT '是否启用，默认启用'/*,
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE SET NULL,
    FOREIGN KEY (position_id) REFERENCES position(id) ON DELETE SET NULL*/
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';
-- 插入初始角色数据
INSERT INTO role (name, description)
VALUES -- 插入初始角色数据
       ('ROLE_ADMIN', '系统管理员'), -- 系统管理员角色
       ('ROLE_USER', '普通用户'),    -- 普通用户角色
       ('ROLE_MANAGER', '部门经理'); -- 部门经理角色

-- 插入初始职位数据
INSERT INTO position (name, description)
VALUES -- 插入初始职位数据
       ('系统管理员', '系统管理员职位'), -- 系统管理员职位
       ('开发工程师', '开发工程师职位'), -- 开发工程师职位
       ('市场专员', '市场专员职位'); -- 市场专员职位

-- 插入初始用户数据
INSERT INTO user (username, password, email, phone_number, department, employee_number, hire_date, role_id, position_id,
                  enabled)
VALUES -- 插入初始用户数据
       ('admin', 'e10adc3949ba59abbe56e057f20f883e', 'admin@example.com', '13800138000',
        '管理部', '001', '2020-01-01', 1, 1, TRUE), -- 管理员用户
       ('user1', 'e10adc3949ba59abbe56e057f20f883e', 'user1@example.com', '13800138001',
        '技术部', '002', '2020-01-01', 2, 2, TRUE), -- 技术部开发工程师用户
       ('user2', 'e10adc3949ba59abbe56e057f20f883e', 'user2@example.com', '13800138002',
        '市场部', '003', '2020-01-01', 2, 3, TRUE);
-- 市场部市场专员用户

-- 创建权限表
CREATE TABLE IF NOT EXISTS permission
( -- 创建permission表
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID，主键，自增',
    name             VARCHAR(50) NOT NULL UNIQUE COMMENT '权限名称，唯一且非空',
    description      VARCHAR(200) COMMENT '权限描述',
    permission_type  VARCHAR(20) NOT NULL COMMENT '权限类型(FUNCTIONAL/DATA)',
    resource         VARCHAR(50) COMMENT '资源名称',
    action           VARCHAR(20) COMMENT '操作类型',
    data_scope       VARCHAR(20) COMMENT '数据范围',
    is_active        BOOLEAN   DEFAULT TRUE COMMENT '是否激活',
    created_by       BIGINT COMMENT '创建人ID',
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    FOREIGN KEY (created_by) REFERENCES user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='权限表';

-- 创建权限功能映射表
CREATE TABLE IF NOT EXISTS permission_function_mapping
(
    permission_id BIGINT       NOT NULL COMMENT '权限ID',
    function_name VARCHAR(100) NOT NULL COMMENT '功能名称',
    has_access    BOOLEAN      NOT NULL COMMENT '是否有权限',
    PRIMARY KEY (permission_id, function_name),
    FOREIGN KEY (permission_id) REFERENCES permission (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='权限功能映射表';

-- 创建权限数据映射表
CREATE TABLE IF NOT EXISTS permission_data_mapping
(
    permission_id BIGINT       NOT NULL COMMENT '权限ID',
    data_type     VARCHAR(100) NOT NULL COMMENT '数据类型',
    access_level  VARCHAR(20)  NOT NULL COMMENT '访问级别',
    PRIMARY KEY (permission_id, data_type),
    FOREIGN KEY (permission_id) REFERENCES permission (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='权限数据映射表';

-- 创建角色权限关联表
-- drop table if exists role_permission;
CREATE TABLE IF NOT EXISTS role_permission
( -- 创建role_permission表
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID，主键，自增',
    role_id       BIGINT NOT NULL COMMENT '角色ID，非空',
    permission_id BIGINT NOT NULL COMMENT '权限ID，非空',
    is_active     BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permission (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色权限关联表';

-- 创建部门表
CREATE TABLE IF NOT EXISTS department
( -- 创建department表
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID，主键，自增',
    name        VARCHAR(50) NOT NULL UNIQUE COMMENT '部门名称，唯一且非空',
    description VARCHAR(200) COMMENT '部门描述',
    parent_id   BIGINT COMMENT '父部门ID',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (parent_id) REFERENCES department (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='部门表';


-- 创建项目表
CREATE TABLE IF NOT EXISTS project
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '项目ID，主键，自增',
    name        VARCHAR(100) NOT NULL COMMENT '项目名称，非空',
    description TEXT COMMENT '项目描述',
    status      VARCHAR(20)  NOT NULL COMMENT '项目状态',
    priority    VARCHAR(10)  NOT NULL DEFAULT '0' COMMENT '优先级',
    type        VARCHAR(10)  NOT NULL DEFAULT '0' COMMENT '项目类型',
    start_date  DATE COMMENT '开始日期',
    end_date    DATE COMMENT '结束日期',
    created_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='项目表';

-- 创建审批流程表
DROP TABLE IF EXISTS approval_flow;
-- 创建任务表
CREATE TABLE IF NOT EXISTS task
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID，主键，自增',
    title           VARCHAR(100) NOT NULL COMMENT '任务标题，非空',
    description     TEXT COMMENT '任务描述',
    status          VARCHAR(20) DEFAULT 'TODO' COMMENT '状态，默认为TODO',
    priority        VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '优先级，默认为MEDIUM',
    due_date        DATE COMMENT '截止日期',
    estimated_hours DECIMAL(5, 2) COMMENT '预计工时',
    actual_hours    DECIMAL(5, 2) COMMENT '实际工时',
    project_id      BIGINT       NOT NULL COMMENT '项目ID，非空',
    assignee_id     BIGINT COMMENT '负责人ID',
    creator_id      BIGINT COMMENT '创建人ID',
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    FOREIGN KEY (assignee_id) REFERENCES user (id) ON DELETE SET NULL,
    FOREIGN KEY (creator_id) REFERENCES user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='任务表';

-- 创建绩效评估表
CREATE TABLE IF NOT EXISTS performance_evaluation
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '绩效评估ID，主键，自增',
    evaluation_period VARCHAR(50) NOT NULL COMMENT '评估周期，非空',
    start_date        DATE        NOT NULL COMMENT '开始日期，非空',
    end_date          DATE        NOT NULL COMMENT '结束日期，非空',
    employee_id       BIGINT      NOT NULL COMMENT '员工ID，非空',
    evaluator_id      BIGINT      NOT NULL COMMENT '评估人ID，非空',
    total_score       DECIMAL(5, 2) COMMENT '总分',
    status            VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态，默认为DRAFT',
    comments          TEXT COMMENT '评论',
    created_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (employee_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (evaluator_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='绩效评估表';

-- 创建绩效标准表
CREATE TABLE IF NOT EXISTS performance_criteria
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '绩效标准ID，主键，自增',
    name        VARCHAR(100) NOT NULL COMMENT '标准名称，非空',
    description TEXT COMMENT '标准描述',
    category    VARCHAR(50)  NOT NULL COMMENT '类别，非空',
    weight      DECIMAL(5, 2) DEFAULT 1.0 COMMENT '权重，默认为1.0',
    max_score   INT           DEFAULT 10 COMMENT '最高分，默认为10',
    is_active   BOOLEAN       DEFAULT TRUE COMMENT '是否激活，默认为TRUE',
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='绩效标准表';

-- 创建绩效评估项目表
CREATE TABLE IF NOT EXISTS performance_evaluation_item
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '绩效评估项目ID，主键，自增',
    evaluation_id BIGINT        NOT NULL COMMENT '绩效评估ID，非空',
    criteria_id   BIGINT        NOT NULL COMMENT '绩效标准ID，非空',
    score         DECIMAL(5, 2) NOT NULL COMMENT '分数，非空',
    comments      TEXT COMMENT '评论',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (evaluation_id) REFERENCES performance_evaluation (id) ON DELETE CASCADE,
    FOREIGN KEY (criteria_id) REFERENCES performance_criteria (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='绩效评估项目表';

-- 创建绩效奖金表
CREATE TABLE IF NOT EXISTS performance_bonus
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '绩效奖金ID，主键，自增',
    employee_id   BIGINT         NOT NULL COMMENT '员工ID，非空',
    evaluation_id BIGINT COMMENT '绩效评估ID',
    amount        DECIMAL(10, 2) NOT NULL COMMENT '金额，非空',
    bonus_period  VARCHAR(50)    NOT NULL COMMENT '奖金周期，非空',
    payment_date  DATE COMMENT '支付日期',
    status        VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态，默认为PENDING',
    approver_id   BIGINT COMMENT '审批人ID',
    comments      TEXT COMMENT '评论',
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (employee_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (evaluation_id) REFERENCES performance_evaluation (id) ON DELETE SET NULL,
    FOREIGN KEY (approver_id) REFERENCES user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='绩效奖金表';

-- 创建工时记录表
CREATE TABLE IF NOT EXISTS work_time_record
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '工时记录ID，主键，自增',
    work_type   VARCHAR(50)   NOT NULL COMMENT '工时类型，非空',
    date        DATE          NOT NULL COMMENT '日期，非空',
    hours       DECIMAL(5, 2) NOT NULL COMMENT '工时，非空',
    description VARCHAR(500) COMMENT '描述',
    approved    BOOLEAN   DEFAULT FALSE COMMENT '是否批准，默认为FALSE',
    user_id     BIGINT        NOT NULL COMMENT '用户ID，非空',
    project_id  BIGINT        NOT NULL COMMENT '项目ID，非空',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='工时记录表';

-- 创建出差申请表
CREATE TABLE IF NOT EXISTS business_trip_request
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '出差申请ID，主键，自增',
    destination    VARCHAR(100) NOT NULL COMMENT '目的地，非空',
    start_date     DATE         NOT NULL COMMENT '开始日期，非空',
    end_date       DATE         NOT NULL COMMENT '结束日期，非空',
    purpose        VARCHAR(500) COMMENT '目的',
    estimated_cost DECIMAL(10, 2) COMMENT '预计费用',
    status         VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态，默认为PENDING',
    applicant_id   BIGINT       NOT NULL COMMENT '申请人ID，非空',
    created_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (applicant_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='出差申请表';

-- 创建请假申请表
CREATE TABLE IF NOT EXISTS leave_request
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '请假申请ID，主键，自增',
    leave_type   VARCHAR(50) NOT NULL COMMENT '请假类型，非空',
    start_date   DATE        NOT NULL COMMENT '开始日期，非空',
    end_date     DATE        NOT NULL COMMENT '结束日期，非空',
    reason       VARCHAR(500) COMMENT '原因',
    status       VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态，默认为PENDING',
    applicant_id BIGINT      NOT NULL COMMENT '申请人ID，非空',
    created_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (applicant_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='请假申请表';

-- 创建报销申请表
CREATE TABLE IF NOT EXISTS reimbursement_request
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '报销申请ID，主键，自增',
    expense_type VARCHAR(50)    NOT NULL COMMENT '费用类型，非空',
    amount       DECIMAL(10, 2) NOT NULL COMMENT '金额，非空',
    expense_date DATE           NOT NULL COMMENT '费用日期，非空',
    description  VARCHAR(500) COMMENT '描述',
    status       VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态，默认为PENDING',
    comment      VARCHAR(500) COMMENT '评论',
    type         VARCHAR(50) COMMENT '类型，与expense_type相同',
    applicant_id BIGINT         NOT NULL COMMENT '申请人ID，非空',
    created_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (applicant_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='报销申请表';

-- 创建报销申请附件表
CREATE TABLE IF NOT EXISTS reimbursement_request_attachment
(
    reimbursement_request_id BIGINT       NOT NULL COMMENT '报销申请ID',
    attachment               VARCHAR(255) NOT NULL COMMENT '附件路径',
    PRIMARY KEY (reimbursement_request_id, attachment),
    FOREIGN KEY (reimbursement_request_id) REFERENCES reimbursement_request (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='报销申请附件表';

-- 创建预算表
CREATE TABLE IF NOT EXISTS budget
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预算ID，主键，自增',
    project_id       BIGINT         NOT NULL COMMENT '项目ID',
    name             VARCHAR(100)   NOT NULL COMMENT '预算名称',
    total_amount     DECIMAL(10, 2) NOT NULL COMMENT '总金额',
    used_amount      DECIMAL(10, 2) COMMENT '已使用金额',
    remaining_amount DECIMAL(10, 2) COMMENT '剩余金额',
    start_date       DATE           NOT NULL COMMENT '开始日期',
    end_date         DATE           NOT NULL COMMENT '结束日期',
    status           VARCHAR(20) COMMENT '状态',
    description      TEXT COMMENT '描述',
    created_by       BIGINT COMMENT '创建人ID',
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='预算表';

-- 创建审批流程表
CREATE TABLE IF NOT EXISTS approval_flow
(
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审批流程ID，主键，自增',
    approver_id              BIGINT NOT NULL COMMENT '审批人ID，非空',
    work_time_record_id      BIGINT COMMENT '工时记录ID',
    leave_request_id         BIGINT COMMENT '请假申请ID',
    business_trip_request_id BIGINT COMMENT '出差申请ID',
    reimbursement_request_id BIGINT COMMENT '报销申请ID',
    request_type             VARCHAR(50) COMMENT '请求类型(WORKTIME, LEAVE, BUSINESS_TRIP, REIMBURSEMENT)',
    status                   VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态，默认为PENDING',
    comment                  VARCHAR(500) COMMENT '评论',
    create_time              TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    update_time              TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (approver_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (work_time_record_id) REFERENCES work_time_record (id) ON DELETE CASCADE,
    FOREIGN KEY (leave_request_id) REFERENCES leave_request (id) ON DELETE CASCADE,
    FOREIGN KEY (business_trip_request_id) REFERENCES business_trip_request (id) ON DELETE CASCADE,
    FOREIGN KEY (reimbursement_request_id) REFERENCES reimbursement_request (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='审批流程表';

-- 删除审批记录表，使用approval_flow替代
DROP TABLE IF EXISTS approval_record;

-- 创建审计日志表
CREATE TABLE IF NOT EXISTS audit_log
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审计日志ID，主键，自增',
    user_id        BIGINT COMMENT '用户ID',
    action         VARCHAR(50) NOT NULL COMMENT '操作，非空',
    entity_type    VARCHAR(50) COMMENT '实体类型',
    entity_id      BIGINT COMMENT '实体ID',
    request_url    VARCHAR(200) COMMENT '请求URL',
    request_method VARCHAR(10) COMMENT '请求方法',
    request_params TEXT COMMENT '请求参数',
    ip_address     VARCHAR(50) COMMENT 'IP地址',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='审计日志表';

-- 创建系统参数表
CREATE TABLE IF NOT EXISTS system_config
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '系统参数ID，主键，自增',
    config_key   VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键，唯一且非空',
    config_value TEXT COMMENT '配置值',
    description  VARCHAR(200) COMMENT '描述',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='系统参数表';

-- 创建项目成员关联表
CREATE TABLE IF NOT EXISTS project_user
(
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id    BIGINT NOT NULL COMMENT '用户ID',
    PRIMARY KEY (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='项目成员关联表';

-- 创建预算项目表
CREATE TABLE IF NOT EXISTS budget_item
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预算项目ID，主键，自增',
    budget_id        BIGINT         NOT NULL COMMENT '预算ID',
    name             VARCHAR(100)   NOT NULL COMMENT '项目名称',
    category         VARCHAR(50)    NOT NULL COMMENT '类别',
    amount           DECIMAL(10, 2) NOT NULL COMMENT '金额',
    used_amount      DECIMAL(10, 2) COMMENT '已使用金额',
    remaining_amount DECIMAL(10, 2) COMMENT '剩余金额',
    status           VARCHAR(20) COMMENT '状态',
    description      TEXT COMMENT '描述',
    last_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    FOREIGN KEY (budget_id) REFERENCES budget (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='预算项目表';

-- 创建预算支出表
CREATE TABLE IF NOT EXISTS budget_expense
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预算支出ID，主键，自增',
    budget_id        BIGINT         NOT NULL COMMENT '预算ID',
    budget_item_id   BIGINT COMMENT '预算项目ID',
    amount           DECIMAL(10, 2) NOT NULL COMMENT '金额',
    expense_date     DATE           NOT NULL COMMENT '支出日期',
    expense_type     VARCHAR(50)    NOT NULL COMMENT '支出类型',
    reference_number VARCHAR(50) COMMENT '参考编号',
    status           VARCHAR(20) COMMENT '状态',
    description      TEXT COMMENT '描述',
    recorded_by      BIGINT COMMENT '记录人ID',
    record_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    FOREIGN KEY (budget_id) REFERENCES budget (id) ON DELETE CASCADE,
    FOREIGN KEY (budget_item_id) REFERENCES budget_item (id) ON DELETE SET NULL,
    FOREIGN KEY (recorded_by) REFERENCES user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='预算支出表';

-- 创建预算告警表
CREATE TABLE IF NOT EXISTS budget_alert
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预算告警ID，主键，自增',
    budget_id      BIGINT       NOT NULL COMMENT '预算ID',
    budget_item_id BIGINT COMMENT '预算项目ID',
    alert_type     VARCHAR(50)  NOT NULL COMMENT '告警类型',
    alert_level    VARCHAR(20)  NOT NULL COMMENT '告警级别',
    message        VARCHAR(500) NOT NULL COMMENT '告警消息',
    alert_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
    status         VARCHAR(20) COMMENT '状态',
    resolved_time  TIMESTAMP COMMENT '解决时间',
    resolved_by    BIGINT COMMENT '解决人ID',
    resolution     TEXT COMMENT '解决方案',
    FOREIGN KEY (budget_id) REFERENCES budget (id) ON DELETE CASCADE,
    FOREIGN KEY (budget_item_id) REFERENCES budget_item (id) ON DELETE SET NULL,
    FOREIGN KEY (resolved_by) REFERENCES user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='预算告警表';

-- 创建外包表
CREATE TABLE IF NOT EXISTS outsourcing
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '外包ID，主键，自增',
    name           VARCHAR(100) NOT NULL COMMENT '外包名称，非空',
    vendor_name    VARCHAR(100) NOT NULL COMMENT '供应商名称，非空',
    vendor_contact VARCHAR(100) COMMENT '供应商联系人',
    vendor_phone   VARCHAR(20) COMMENT '供应商电话',
    vendor_email   VARCHAR(100) COMMENT '供应商邮箱',
    project_id     BIGINT COMMENT '关联项目ID',
    description    TEXT COMMENT '描述',
    status         VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态，默认为ACTIVE',
    created_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='外包表';

-- 创建外包合同表
CREATE TABLE IF NOT EXISTS outsourcing_contract
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '外包合同ID，主键，自增',
    contract_number VARCHAR(50) NOT NULL COMMENT '合同编号，非空',
    outsourcing_id  BIGINT      NOT NULL COMMENT '外包ID，非空',
    sign_date       DATE        NOT NULL COMMENT '签订日期，非空',
    effective_date  DATE        NOT NULL COMMENT '生效日期，非空',
    expiration_date DATE        NOT NULL COMMENT '到期日期，非空',
    total_amount    DOUBLE      NOT NULL COMMENT '合同总金额，非空',
    paid_amount     DOUBLE COMMENT '已支付金额',
    terms           TEXT COMMENT '合同条款',
    status          VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态，默认为ACTIVE',
    attachment_path TEXT COMMENT '附件路径',
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (outsourcing_id) REFERENCES outsourcing (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='外包合同表';

-- 创建外包进度表
CREATE TABLE IF NOT EXISTS outsourcing_progress
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '外包进度ID，主键，自增',
    outsourcing_id        BIGINT       NOT NULL COMMENT '外包ID，非空',
    milestone             VARCHAR(100) NOT NULL COMMENT '里程碑，非空',
    planned_date          DATE         NOT NULL COMMENT '计划日期，非空',
    actual_date           DATE COMMENT '实际日期',
    completion_percentage INT         DEFAULT 0 COMMENT '完成百分比，默认为0',
    description           TEXT COMMENT '描述',
    status                VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态，默认为PENDING',
    created_at            TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at            TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (outsourcing_id) REFERENCES outsourcing (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='外包进度表';

-- 创建支付表
CREATE TABLE IF NOT EXISTS payment
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '支付ID，主键，自增',
    payment_number          VARCHAR(50)    NOT NULL COMMENT '支付编号，非空',
    payment_type            VARCHAR(50)    NOT NULL COMMENT '支付类型，非空',
    amount                  DECIMAL(10, 2) NOT NULL COMMENT '支付金额，非空',
    payment_date            DATE           NOT NULL COMMENT '支付日期，非空',
    payee                   VARCHAR(100)   NOT NULL COMMENT '收款人，非空',
    account_info            VARCHAR(200) COMMENT '账户信息',
    project_id              BIGINT COMMENT '项目ID',
    outsourcing_contract_id BIGINT COMMENT '外包合同ID',
    status                  VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态，默认为PENDING',
    description             TEXT COMMENT '描述',
    created_by              BIGINT COMMENT '创建人ID',
    approved_by             BIGINT COMMENT '审批人ID',
    created_at              TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at              TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE SET NULL,
    FOREIGN KEY (outsourcing_contract_id) REFERENCES outsourcing_contract (id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES user (id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='支付表';

-- 创建收款计划表
CREATE TABLE IF NOT EXISTS payment_collection_plan
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收款计划ID，主键，自增',
    plan_name        VARCHAR(100)   NOT NULL COMMENT '计划名称，非空',
    project_id       BIGINT         NOT NULL COMMENT '项目ID，非空',
    total_amount     DECIMAL(10, 2) NOT NULL COMMENT '总金额，非空',
    collected_amount DECIMAL(10, 2) DEFAULT 0 COMMENT '已收金额，默认为0',
    start_date       DATE           NOT NULL COMMENT '开始日期，非空',
    end_date         DATE           NOT NULL COMMENT '结束日期，非空',
    status           VARCHAR(20)    DEFAULT 'ACTIVE' COMMENT '状态，默认为ACTIVE',
    description      TEXT COMMENT '描述',
    created_by       BIGINT COMMENT '创建人ID',
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='收款计划表';

-- 创建收款表
CREATE TABLE IF NOT EXISTS payment_collection
(
    id                         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收款ID，主键，自增',
    collection_number          VARCHAR(50)    NOT NULL COMMENT '收款编号，非空',
    amount                     DECIMAL(10, 2) NOT NULL COMMENT '收款金额，非空',
    collection_date            DATE           NOT NULL COMMENT '收款日期，非空',
    payer                      VARCHAR(100)   NOT NULL COMMENT '付款人，非空',
    payment_method             VARCHAR(50) COMMENT '支付方式',
    project_id                 BIGINT COMMENT '项目ID',
    payment_collection_plan_id BIGINT COMMENT '收款计划ID',
    status                     VARCHAR(20) DEFAULT 'RECEIVED' COMMENT '状态，默认为RECEIVED',
    description                TEXT COMMENT '描述',
    created_by                 BIGINT COMMENT '创建人ID',
    created_at                 TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at                 TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE SET NULL,
    FOREIGN KEY (payment_collection_plan_id) REFERENCES payment_collection_plan (id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES user (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='收款表';

-- 创建收款计划项目表
CREATE TABLE IF NOT EXISTS payment_collection_plan_item
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收款计划项目ID，主键，自增',
    plan_id      BIGINT         NOT NULL COMMENT '收款计划ID，非空',
    item_name    VARCHAR(100)   NOT NULL COMMENT '项目名称，非空',
    amount       DECIMAL(10, 2) NOT NULL COMMENT '金额，非空',
    planned_date DATE           NOT NULL COMMENT '计划日期，非空',
    actual_date  DATE COMMENT '实际日期',
    status       VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态，默认为PENDING',
    description  TEXT COMMENT '描述',
    created_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (plan_id) REFERENCES payment_collection_plan (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='收款计划项目表';

-- 创建审批流程表
CREATE TABLE IF NOT EXISTS approval_flow
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审批流程ID，主键，自增',
    status              VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态，默认为PENDING',
    comment             VARCHAR(500) COMMENT '评论',
    approver_id         BIGINT NOT NULL COMMENT '审批人ID，非空',
    work_time_record_id BIGINT NOT NULL COMMENT '工时记录ID，非空',
    created_at          TIMESTAMP   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间',
    updated_at          TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，默认当前时间且自动更新',
    FOREIGN KEY (approver_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (work_time_record_id) REFERENCES work_time_record (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='审批流程表';
