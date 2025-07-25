# Gemini 项目配置

此文件为 Gemini AI 助手提供上下文，帮助其理解项目结构、约定和使用的技术。

## 项目概览

这是一个 SimpleOA（办公自动化）系统。它是一个全栈应用程序，后端使用 Java，前端使用 Vue.js。

## 后端 (`src` 目录)

-   **语言:** Java
-   **框架:** Spring Boot
-   **构建工具:** Maven (`pom.xml`)
-   **数据库迁移:** Flyway (`src/main/resources/db/migration`)
-   **源代码:** `src/main/java`
-   **资源:** `src/main/resources`

## 前端 (`web` 目录)

-   **框架:** Vue.js
-   **构建工具:** Vite (`vite.config.js`)
-   **包管理器:** 可能是 yarn 或 npm (`package.json`, `yarn.lock`)
-   **源代码:** `web/src`
-   **UI 组件:** `web/src/views` 和 `web/src/layouts`

## 通用指令

-   请遵循现有的代码风格和约定。
-   从现有测试中推断测试策略。

## 交互指南

-   **语言:** 请始终使用中文回复。