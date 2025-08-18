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
    -   **监控:** Spring Boot Actuator
-   **数据库:** MySQL
-   **数据库迁移:** Flyway (`src/main/resources/db/migration`)
-   **构建工具:** Maven (`pom.xml`)
-   **核心依赖:**
    -   `mysql-connector-java`: MySQL 驱动
    -   `jjwt`: JSON Web Token 支持
    -   `lombok`: 减少样板代码
    -   `poi`: 处理 Excel 文件

### 前端 (`web` 目录)

-   **框架:** Vue.js 3
-   **UI 框架:** Element Plus
-   **构建工具:** Vite (`vite.config.js`)
-   **包管理器:** npm (`package.json`)
-   **路由:** Vue Router (`web/src/router/index.js`)
-   **状态管理:** Pinia (`web/src/stores`)
-   **HTTP客户端:** Axios (`web/src/utils/axios.js`)
-   **代码规范:** ESLint 和 Prettier
-   **测试:** Vitest

---

## 架构与约定

### 前后端交互

-   后端在 `8080` 端口上提供 RESTful API。
-   前端开发服务器通过 Vite 代理将 `/api` 前缀的请求转发到后端的 `http://localhost:8080`。
-   API 请求路径重写：前端的 `/api/users` 会被转发到后端的 `/users`。
-   认证机制：使用 JWT。前端在登录后保存 token，并在后续请求的 Header 中携带。

### 后端代码结构

-   遵循标准的 Spring Boot 项目结构。
-   预计包含以下包：
    -   `com.example.simpleoa.controller`: API 入口，处理 HTTP 请求。
    -   `com.example.simpleoa.service`: 业务逻辑层。
    -   `com.example.simpleoa.repository`: 数据访问层，使用 Spring Data JPA。
    -   `com.example.simpleoa.model` 或 `com.example.simpleoa.entity`: JPA 实体类。
    -   `com.example.simpleoa.config`: 应用配置，如安全配置。
    -   `com.example.simpleoa.security`: JWT 相关工具和过滤器。

### 前端代码结构

-   `web/src/main.js`: 应用入口。
-   `web/src/App.vue`: 根组件。
-   `web/src/router/index.js`: 定义所有页面路由和导航守卫。导航守卫会检查用户权限。
-   `web/src/stores`: Pinia 状态管理模块，例如 `user.js` 用于管理用户登录状态、信息和权限。
-   `web/src/views`: 页面级组件，每个 `.vue` 文件对应一个页面。
-   `web/src/layouts`: 布局组件，如 `MainLayout.vue` 包含通用的头部和侧边栏。
-   `web/src/utils`: 通用工具模块，如封装的 `axios.js`。
-   `web/src/components`: 可复用的基础组件（如果存在）。

## 通用指令

-   请遵循现有的代码风格和约定。
-   从现有测试中推断测试策略。
-   进行后端开发时，请遵循 Controller -> Service -> Repository 的分层架构。
-   进行前端开发时，请善用 Pinia store 进行状态管理，并通过 `axios` 实例与后端交互。

## 交互指南

-   **语言:** 请始终使用中文回复。
