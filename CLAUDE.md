# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 在此代码仓库中工作时提供指导。

## 项目概述

这是一个基于 Spring Boot 3.1.5 和 Vue.js 3 构建的简单OA（办公自动化）系统。提供全面的企业管理功能，包括OA管理、项目管理、企业管理和系统管理。

**技术栈：**
- 后端：Spring Boot 3.1.5, Spring Security, Spring Data JPA, MySQL 8.0, JWT, Lombok
- 前端：Vue.js 3, Element Plus, Pinia, Vite, Axios, FullCalendar, ECharts  
- 构建工具：Maven（后端），npm/Vite（前端）

## 常用开发命令

### 后端命令
```bash
# 构建并运行后端
mvn clean install
mvn spring-boot:run

# 运行测试
mvn test

# 生产环境打包
mvn clean package
```

### 前端命令
```bash
# 进入前端目录
cd web

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 生产环境构建
npm run build

# 运行代码检查（ESLint with Vue.js 规则）
npm run lint
```

## 架构总览

### 后端架构
- **包结构**：`com.example.simpleoa` 下的标准Spring Boot分层架构
  - `controller/`：REST API 端点，启用了 @CrossOrigin
  - `service/` 和 `service/impl/`：业务逻辑层，包含服务接口
  - `repository/`：数据访问层，使用Spring Data JPA和自定义查询
  - `model/`：JPA实体，使用Lombok注解
  - `config/`：配置类（安全、CORS、密码编码）
  - `security/`：JWT认证（JwtTokenProvider、JwtAuthenticationFilter）
  - `common/`：共享工具类如ApiResponse包装器
  - `exception/`：全局异常处理，使用@ControllerAdvice

### 前端架构  
- **框架**：Vue.js 3 使用组合式API和 `<script setup>` 语法
- **UI库**：Element Plus 中文本土化（el-table、el-form等）
- **状态管理**：Pinia stores 用于认证和全局状态
- **路由**：Vue Router 带认证守卫和受保护路由
- **构建工具**：Vite 配置后端API代理和路径别名（@/ -> ./src）
- **图表**：ECharts 用于仪表板可视化
- **日历**：FullCalendar 用于假期/工作日管理

### 关键配置文件
- `application.yml`：后端配置（MySQL连接、JWT设置、文件上传、时区=Asia/Shanghai）
- `web/vite.config.js`：前端代理配置、别名和开发服务器设置
- `web/package.json`：依赖包含Vue 3、Element Plus、Pinia
- `pom.xml`：后端依赖包含Spring Boot 3.1.5、JWT（jjwt 0.11.5）、Apache POI

### 安全与数据库配置
- **JWT认证**：密钥和24小时令牌有效期在application.yml中配置
- **CORS**：开发环境启用所有源（`@CrossOrigin(origins = "*")`）
- **数据库**：MySQL 8.0，时区=Asia/Shanghai，连接端口3306
- **文件上传**：最大10MB文件大小，上传存储在 `uploads/` 目录
- **数据迁移**：Flyway/手动SQL脚本位于 `src/main/resources/db/migration/`

## 核心业务模块

### OA管理
- **工时管理**：WorkTimeController 支持批量操作和审批流程
- **请假管理**：请假申请、审批和日历集成
- **出差管理**：差旅申请和费用管理
- **报销管理**：多项费用报销和附件上传

### 项目管理  
- **项目管理**：完整的CRUD操作，支持成员分配和任务管理
- **分包管理**：分包商管理和合同跟踪
- **预算管理**：预算分配、监控和预警系统
- **付款/回款**：财务跟踪和计划管理
- **绩效管理**：员工评估和奖金计算

### 系统管理
- **用户/角色/权限**：基于角色的访问控制，菜单级权限
- **假期/工作日**：日历管理用于请假计算
- **系统配置**：动态配置参数

## 开发模式

### 后端模式
- **控制器层**：RESTful端点使用 `@CrossOrigin`，统一响应格式
- **服务层**：基于接口设计，使用 `@Transactional` 方法
- **仓储层**：Spring Data JPA 配合自定义 `@Query` 注解
- **错误处理**：全局异常处理器，结构化错误响应
- **安全**：JWT过滤器链和基于角色的授权

### 前端模式  
- **组件结构**：单文件组件使用组合式API
- **数据获取**：Axios 集中式API配置和错误处理
- **表单处理**：Element Plus 表单配合验证规则
- **状态管理**：Pinia stores 包含计算属性和动作
- **路由**：受保护路由配合认证守卫

## 数据库架构说明
- **迁移版本**：V1__init.sql、V2__menu_permission.sql 等
- **字符集**：utf8mb4 配合适当的中文排序规则
- **关联关系**：User、Role、Permission、Menu 实体间的JPA关联
- **审计字段**：大多数实体包含 CreatedAt、UpdatedAt 时间戳

## 开发环境设置

1. **前置要求**：Java 17+、Node.js 16+、MySQL 8.0
2. **数据库**：创建 `simple_oa` 数据库，按顺序运行迁移脚本
3. **后端**：配置 `application.yml` 数据库连接，运行 `mvn spring-boot:run`（端口8989）
4. **前端**：运行 `cd web && npm install && npm run dev`（端口3000，带API代理）
5. **访问**：前端 http://localhost:3000，后端API http://localhost:8989

## 关键集成点

- **API代理**：Vite开发服务器将 `/api/*` 代理到 `http://localhost:8989`
- **认证**：JWT令牌存储在localStorage，通过Authorization头发送
- **文件上传**：多部分表单数据到 `/api/upload` 端点
- **国际化**：Element Plus组件全面使用中文UI文本
- **时间处理**：配置Asia/Shanghai时区确保日期/时间操作一致