# 项目简介
本项目是一个基于Spring Boot和Vue.js的前后端分离的企业内部管理系统，旨在提供全面的办公自动化（OA）、项目管理和企业管理功能。系统覆盖以下主要模块：

- **OA管理**：包括工时管理、考勤管理（请假/休假申请）、出差管理和报销管理。
- **项目管理**：包括项目信息管理、分包管理、回款管理、付款管理、预算管理和奖金管理。
- **企业管理**：包括通知公告管理、员工手册管理和用章管理。
- **系统管理**：包括用户管理、岗位管理、权限管理和系统配置。

## 报销管理系统详细说明

### 核心功能概述
报销管理系统是本项目的核心业务模块之一，实现了完整的报销申请、审批、预算校验和扣减流程。系统支持多级审批流程，与项目预算管理深度集成，确保财务合规性。

### 业务流程图
```
员工提交报销申请 → 预算校验 → 项目经理审批 → 财务经理审批 → 预算扣减 → 审批完成
     ↓                ↓           ↓            ↓            ↓         ↓
   草稿状态        预算验证    待项目经理审批  待财务审批   自动扣减   审批通过
     ↓                ↓           ↓            ↓            ↓         ↓
  可编辑修改      不足则拒绝    可批准/驳回   可批准/驳回  更新预算   流程结束
```

### 状态流转说明
- **DRAFT（草稿）**：初始状态，申请人可编辑修改
- **PENDING_MANAGER_APPROVAL（待项目经理审批）**：提交后等待项目经理审批
- **PENDING_FINANCE_APPROVAL（待财务审批）**：项目经理批准后等待财务审批
- **APPROVED（审批通过）**：审批完成，预算已扣减
- **REJECTED（已驳回）**：审批被拒绝，需要重新修改或撤回

### 数据模型说明

#### ReimbursementRequest（报销申请主表）
| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | Long | 主键ID | 自增 |
| applicant | User | 申请人 | 外键，非空 |
| project | Project | 关联项目 | 外键，可空 |
| title | String | 报销标题 | 非空，如"2025年6月差旅报销" |
| totalAmount | BigDecimal | 总金额 | 由明细计算得出 |
| status | ReimbursementStatus | 报销状态 | 枚举值 |
| comment | String | 审批意见 | 可空 |
| attachments | List\<String\> | 凭证文件路径列表 | 可空 |
| items | List\<ReimbursementItem\> | 费用明细 | 一对多关系 |
| createTime | LocalDateTime | 创建时间 | 自动生成 |
| updateTime | LocalDateTime | 更新时间 | 自动更新 |

#### ReimbursementItem（报销明细表）
| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | Long | 主键ID | 自增 |
| reimbursementRequest | ReimbursementRequest | 关联报销申请 | 外键，非空 |
| expenseDate | LocalDate | 费用发生日期 | 格式：yyyy-MM-dd |
| itemCategory | String | 费用类别 | 如"交通"、"餐饮"、"住宿" |
| description | String | 费用描述 | 详细说明 |
| amount | BigDecimal | 费用金额 | 非空 |
| budget | Budget | 关联预算 | 外键，可空 |
| budgetItem | BudgetItem | 关联预算明细 | 外键，可空 |

#### ReimbursementRequestDTO（报销申请传输对象）
| 字段名 | 类型 | 说明 |
|--------|------|---------|
| id | Long | 报销ID（用于更新） |
| title | String | 报销标题 |
| projectId | Long | 项目ID |
| items | List\<ReimbursementItemDTO\> | 费用明细列表 |
| attachments | List\<String\> | 附件路径列表 |

#### ReimbursementItemDTO（报销明细传输对象）
| 字段名 | 类型 | 说明 |
|--------|------|---------|
| id | Long | 明细ID（用于更新） |
| expenseDate | LocalDate | 费用发生日期 |
| itemCategory | String | 费用类别 |
| description | String | 费用描述 |
| amount | BigDecimal | 费用金额 |
| budgetId | Long | 关联预算ID |
| budgetItemId | Long | 关联预算明细ID |

### API 接口文档

#### 基础信息
- **基础URL**: `/api/oa/reimbursement`
- **认证方式**: JWT Token（Header: `Authorization: Bearer {token}`）
- **返回格式**: 统一使用 `ApiResponse<T>` 包装

#### 1. 创建报销申请
```http
POST /api/oa/reimbursement
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "2025年6月差旅报销",
  "projectId": 1,
  "items": [
    {
      "expenseDate": "2025-06-15",
      "itemCategory": "交通",
      "description": "北京到上海高铁票",
      "amount": 553.50,
      "budgetId": 1
    }
  ],
  "attachments": ["/uploads/receipts/receipt1.jpg"]
}
```

**返回示例**:
```json
{
  "success": true,
  "message": "报销申请已创建",
  "data": {
    "id": 1,
    "title": "2025年6月差旅报销",
    "totalAmount": 553.50,
    "status": "DRAFT",
    "createTime": "2025-06-15T10:30:00"
  }
}
```

#### 2. 更新报销申请
```http
PUT /api/oa/reimbursement/{id}
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "更新后的报销标题",
  "projectId": 1,
  "items": [...],
  "attachments": [...]
}
```

#### 3. 删除报销申请
```http
DELETE /api/oa/reimbursement/{id}
Authorization: Bearer {token}
```

#### 4. 获取报销申请详情
```http
GET /api/oa/reimbursement/{id}
Authorization: Bearer {token}
```

#### 5. 查询报销申请列表
```http
GET /api/oa/reimbursement?page=0&size=10&status=DRAFT&startDate=2025-06-01&endDate=2025-06-30&keyword=差旅
Authorization: Bearer {token}
```

**查询参数**:
- `page`: 页码（从0开始）
- `size`: 每页数量
- `status`: 报销状态（可选）
- `startDate`: 开始日期（可选）
- `endDate`: 结束日期（可选）
- `keyword`: 关键字搜索（可选）

#### 6. 提交审批
```http
POST /api/oa/reimbursement/{id}/submit
Authorization: Bearer {token}
```

**返回示例**:
```json
{
  "success": true,
  "message": "报销申请已提交审批",
  "data": {
    "id": 1,
    "status": "PENDING_MANAGER_APPROVAL"
  }
}
```

#### 7. 审批报销申请（已废弃）
```http
POST /api/oa/reimbursement/{id}/approval
Content-Type: application/json
Authorization: Bearer {token}

{
  "decision": "APPROVE", // 或 "REJECT"
  "comment": "审批意见"
}
```

**注意**: 此接口已废弃，审批功能已迁移至统一审批管理系统。

#### 8. 获取报销统计数据
```http
GET /api/oa/reimbursement/statistics?startDate=2025-06-01&endDate=2025-06-30
Authorization: Bearer {token}
```

**返回示例**:
```json
{
  "success": true,
  "data": {
    "summary": {
      "totalAmount": 5230.50,
      "totalCount": 8,
      "approvalRate": "87.5%",
      "avgAmount": 653.81,
      "mostUsedCategory": "交通"
    },
    "details": [
      {
        "category": "交通",
        "totalAmount": 2100.00,
        "count": 3,
        "avgAmount": 700.00
      }
    ]
  }
}
```

#### 9. 获取报销状态选项
```http
GET /api/oa/reimbursement/status-options
Authorization: Bearer {token}
```

#### 10. 检查预算可用性
```http
GET /api/oa/reimbursement/{id}/check-budget
Authorization: Bearer {token}
```

#### 11. 验证报销预算
```http
POST /api/oa/reimbursement/validate-budget
Content-Type: application/json
Authorization: Bearer {token}

{
  "projectId": 1,
  "items": [
    {
      "amount": 553.50,
      "budgetId": 1
    }
  ]
}
```

### 错误处理

系统使用统一的错误处理机制，所有错误都会返回统一格式：

```json
{
  "success": false,
  "message": "错误描述",
  "data": null
}
```

**常见错误码**:
- `400 Bad Request`: 参数错误或预算不足
- `401 Unauthorized`: 未登录或Token失效
- `403 Forbidden`: 没有权限执行该操作
- `404 Not Found`: 资源不存在
- `500 Internal Server Error`: 服务器内部错误

## 技术栈选型

### 前端技术
1. **框架选择**: Vue.js 3 + Element Plus
   - **理由**: Vue.js 3是最简单易上手的前端框架，组合式API使代码更简洁；Element Plus提供丰富的UI组件，维护成本低。
   - **版本**: Vue.js 3.3.x + Element Plus 2.3.x
2. **辅助工具**:
   - **Axios**: HTTP请求库，用于与后端API交互。
   - **Vue Router**: 路由管理，实现页面导航。
   - **Pinia**: 状态管理，比Vuex更简单轻量。

### 后端技术
1. **框架选择**: Spring Boot 3.x
   - **理由**: Java生态成熟稳定，Spring Boot简化配置，内置Tomcat服务器。
   - **关键依赖**:
     - **Spring Security**: 权限控制，实现用户认证和授权。
     - **Spring Data JPA**: 简化数据库操作，与Hibernate集成。
     - **Lombok**: 减少样板代码，提升代码简洁性。
2. **数据库**: MySQL 8.0
   - **理由**: 关系型数据库适合业务系统，免费且社区支持好。
   - **特性**: 支持JSON字段、窗口函数等现代特性。

## 功能实现对比与缺失分析

### 已实现主要模块
- **登录与权限管理模块**：已实现用户认证和基本权限控制。
- **部分基础数据管理页面**：如用户、角色、组织等管理功能已部分实现。
- **基础的前后端分离架构与接口联调**：前后端分离架构已建立，部分接口已完成联调。

### 存在的功能缺失
- **用章管理、分包管理等业务页面尚未开发**：这些模块的前端页面和后端逻辑尚未完全实现。
- **相关状态管理（如Pinia store）未完善**：前端状态管理尚未全面覆盖所有模块。
- **部分接口封装与统一错误处理缺失**：后端接口封装不够完善，统一错误处理机制尚未建立。
- **页面交互与数据校验逻辑有待补充**：前端页面交互和数据校验功能需要进一步完善。

### 后续完善建议
- 按照原始需求逐步补齐用章管理、分包管理等核心业务模块。
- 完善前端状态管理与接口封装，提升代码复用性和可维护性。
- 增加统一的异常处理与用户友好提示。
- 完善单元测试与集成测试，保障系统稳定性。

## 工程详细进度情况

### 后端实现进度
- **OA管理模块**：
  - **工时管理**：已实现完整的控制器功能，包括工时记录、批量提交、审批和统计报表等功能。
  - **考勤管理**：已实现请假申请、记录查询和审批功能。
  - **出差管理**：已实现出差申请、记录查询和审批功能。
  - **报销管理**：已实现报销申请、记录查询和审批功能。
- **项目管理模块**：
  - **项目信息管理**：已实现项目创建、更新、删除、查询及成员和任务管理功能。
  - **分包管理**：已实现分包商和合同管理功能，包括分配和进度跟踪。
  - **回款管理**：已实现回款记录和计划管理功能，包括统计和趋势分析。
  - **付款管理**：已实现付款记录和标记支付功能。
  - **预算管理**：已实现预算创建、更新、监控和预警功能，功能较为完善。
  - **奖金管理**：已实现绩效评估和奖金分配功能，功能较为完善。
- **企业管理模块**：
  - 未见专门的通知公告管理、员工手册管理和用章管理控制器，相关功能尚未实现。
- **系统管理模块**：
  - **用户管理**：已实现用户注册、更新、删除和角色分配功能。
  - **岗位管理**：已实现岗位创建、更新、删除和用户分配功能。
  - **权限管理**：已实现角色和权限的创建、更新和分配功能。
  - **系统配置**：已实现系统参数的查询和更新功能。

### 前端实现进度
- **OA管理模块**：
  - **工时管理**：已实现工时填写、记录查询和详情查看页面（TimeManagement.vue）。
  - **考勤管理**：已实现请假申请（LeaveApplication.vue）和请假管理页面（LeaveManagement.vue）。
  - **出差管理**：已实现出差管理页面（TravelManagement.vue）。
  - **报销管理**：已实现报销申请页面（Reimbursement.vue）。
- **项目管理模块**：
  - **项目信息管理**：已实现项目列表（Projects.vue）和项目表单页面（ProjectForm.vue）。
  - **分包管理**：已实现分包管理页面（OutsourcingManagement.vue）。
  - **回款管理**：未见专门的回款管理页面，但付款管理页面可能包含部分功能（PaymentManagement.vue）。
  - **付款管理**：已实现付款管理页面（PaymentManagement.vue）。
  - **预算管理**：已实现预算管理页面（BudgetManagement.vue）。
  - **奖金管理**：已实现绩效管理页面（PerformanceManagement.vue）。
- **企业管理模块**：
  - **通知公告管理**：已实现通知公告页面（Announcement.vue）和通知查看页面（Notification.vue）。
  - **员工手册管理**：未见专门的员工手册管理页面。
  - **用章管理**：未见专门的用章管理页面。
- **系统管理模块**：
  - **用户管理**：已实现用户管理页面（UserManagement.vue）。
  - **岗位管理**：未见专门的岗位管理页面，但可能包含在用户管理中。
  - **权限管理**：已实现权限管理页面（PermissionManagement.vue）和角色管理页面（RoleManagement.vue）。
  - **系统配置**：已实现系统配置页面（SystemConfig.vue）。

### 总体进度评估
- **已实现功能**：大部分核心模块的前端页面和后端控制器已实现，包括工时管理、考勤管理、出差管理、报销管理、项目信息管理、分包管理、回款管理、付款管理、预算管理、奖金管理、用户管理、权限管理和系统配置。
- **未实现功能**：企业管理模块中的通知公告管理、员工手册管理和用章管理功能尚未完全实现，相关前端页面和后端控制器缺失或不完整。
- **进度百分比**：综合评估，工程进度约为70%，核心OA和项目管理功能已基本实现，但企业管理模块和部分辅助功能仍需完善。

## 启动说明

### 前端启动
1. 进入前端目录：`cd web`
2. 安装依赖：`npm install`
3. 启动开发服务器：`npm run dev`
4. 访问前端页面：浏览器打开`http://localhost:5173`（端口可能根据配置有所不同）

### 后端启动
1. 确保已安装Java 17或更高版本
2. 进入后端目录：`cd .`
3. 构建项目：`mvn clean install`
4. 启动后端服务：`mvn spring-boot:run`
5. 访问API文档：浏览器打开`http://localhost:8080/swagger-ui.html`（如已配置Swagger）

## 部署指南

### 前端部署
1. 构建生产环境包：`npm run build`
2. 将构建输出的`dist`目录内容复制到Nginx或其他Web服务器的静态文件目录
3. 配置Nginx反向代理，将API请求转发到后端服务

### 后端部署
1. 构建可执行JAR包：`mvn clean package`
2. 将生成的JAR文件（如`target/simpleoa-0.0.1-SNAPSHOT.jar`）复制到服务器
3. 运行JAR文件：`java -jar simpleoa-0.0.1-SNAPSHOT.jar`
4. 配置环境变量或`application.yml`文件，确保数据库连接等配置正确
5. 建议使用Nginx进行反向代理和负载均衡

### 数据库部署
1. 安装MySQL 8.0或更高版本
2. 创建数据库：`CREATE DATABASE simpleoa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
3. 执行初始化SQL脚本：位于`src/main/resources/db/migration/`目录下
4. 配置Spring Boot应用连接到数据库

## 注意事项
- 所有环境配置必须通过外部化配置管理（如Spring Profile、.env文件），禁止硬编码。
- Git提交信息必须遵循Conventional Commits规范。
- 代码必须遵循各自语言/框架的通用格式化标准（如Java: Checkstyle/PMD, Vue/TS: ESLint/Prettier）。