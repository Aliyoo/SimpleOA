# Gemini 项目配置

此文件为 Gemini AI 助手提供上下文，帮助其理解项目结构、约定和使用的技术。

## 项目概览

这是一个 SimpleOA（办公自动化）系统。它是一个全栈应用程序，后端使用 Java Spring Boot，前端使用 Vue.js 3。

---

## 技术栈详解

### 后端 (`src` 目录)

-   **语言:** Java 17
-   **框架:** Spring Boot 3.1.5
    -   **Web:** Spring Web (for RESTful APIs)
    -   **数据访问:** Spring Data JPA
    -   **安全:** Spring Security (集成 JWT for token-based authentication)
-   **数据库:** MySQL
-   **数据库迁移:** Flyway (`src/main/resources/db/migration`)
-   **构建工具:** Maven (`pom.xml`)
-   **核心依赖:** `mysql-connector-java`, `jjwt`, `lombok`, `poi-ooxml`

### 前端 (`web` 目录)

-   **框架:** Vue.js 3
-   **UI 框架:** Element Plus
-   **构建工具:** Vite (`vite.config.js`)
-   **包管理器:** npm (`package.json`)
-   **路由:** Vue Router (`web/src/router/index.js`)
-   **状态管理:** Pinia (`web/src/stores`)
-   **HTTP客户端:** Axios (`web/src/utils/axios.js`)

---

## 架构与约定

-   **前后端交互:**
    -   后端 API 运行于 `8080` 端口。
    -   前端 Vite 开发服务器通过代理将 `/api` 请求转发至 `http://localhost:8080`。
    -   认证机制为 JWT，由前端在请求头中携带。
-   **后端代码结构:**
    -   `com.example.simpleoa.controller`: API 控制器
    -   `com.example.simpleoa.service`: 业务逻辑
    -   `com.example.simpleoa.repository`: 数据访问 (Spring Data JPA)
    -   `com.example.simpleoa.model`/`entity`: JPA 实体
    -   `com.example.simpleoa.config`: Spring Boot 配置
-   **前端代码结构:**
    -   `web/src/views`: 页面组件
    -   `web/src/stores`: Pinia 状态管理
    -   `web/src/router`: 路由配置
    -   `web/src/utils`: 工具模块 (如 `axios.js`)

---

## 重要文件和目录

-   `pom.xml`: 定义后端所有依赖和构建配置。
-   `web/package.json`: 定义前端所有依赖和脚本命令 (`dev`, `build`, `test`)。
-   `src/main/resources/application.yml`: Spring Boot 核心配置文件。
-   `src/main/resources/db/migration`: Flyway 管理的数据库版本迁移脚本。
-   **根目录 `.sql` 文件**: 用于数据初始化 (`insert_*.sql`)、修复 (`fix_*.sql`) 和验证 (`verify_*.sql`) 的辅助脚本。
-   **根目录 `.md` 文件**: 包含需求、设计、报告等重要项目文档。
-   **根目录 `.sh` 文件**: 用于执行特定功能模块的自动化测试脚本 (例如 `test-reimbursement-approval.sh`)。
-   `postman-approval-api-test.json`: Postman 集合，用于 API 测试。

## 开发与测试

-   **后端启动**: 通过 Maven 运行 Spring Boot 应用。
-   **前端启动**: 在 `web` 目录下执行 `npm install` 和 `npm run dev`。
-   **测试**:
    -   使用根目录的 `.sh` 脚本对特定场景进行集成测试。
    -   使用 `postman-*.json` 文件导入 Postman 进行接口测试。
    -   前端单元测试可在 `web` 目录下执行 `npm run test`。

## 通用指令

-   请遵循现有的代码风格和约定。
-   进行后端开发时，请遵循 Controller -> Service -> Repository 的分层架构。
-   进行前端开发时，请善用 Pinia store 进行状态管理，并通过 `axios` 实例与后端交互。