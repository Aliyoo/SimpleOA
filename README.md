# 企业内部管理系统

一个基于Spring Boot和Vue.js的前后端分离的企业内部管理系统，旨在提供全面的办公自动化（OA）、项目管理和企业管理功能。

## ✨ 主要功能

本系统包含以下核心模块：

-   **OA管理**:
    -   工时管理
    -   考勤管理 (请假/休假)
    -   出差管理
    -   报销管理
-   **项目管理**:
    -   项目信息管理
    -   分包管理
    -   回款与付款管理
    -   预算与奖金管理
-   **企业管理**:
    -   通知公告
    -   员工手册
    -   用章管理
-   **系统管理**:
    -   用户、岗位与权限管理
    -   系统配置

关于详细的功能实现进度，请参阅 [`docs/project_status.md`](./docs/project_status.md)。

## 🛠️ 技术栈

| 领域 | 技术 |
| :--- | :--- |
| **前端** | Vue.js 3, Element Plus, Pinia, Vue Router, Axios |
| **后端** | Spring Boot 3, Spring Security, Spring Data JPA, MySQL 8.0 |
| **工具** | Maven, Node.js, Git |

## 🚀 快速启动

请遵循以下步骤在本地运行本项目。

### 环境要求

-   Java 17 或更高版本
-   Maven 3.x
-   Node.js 20.x 或更高版本
-   MySQL 8.0

### 1. 后端启动

```bash
# 克隆仓库
git clone <repository-url>
cd <repository-folder>

# 在 src/main/resources/application.yml 中配置数据库连接
# 在 MySQL 中创建数据库
# CREATE DATABASE simpleoa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 构建并运行 Spring Boot 应用
mvn clean install
mvn spring-boot:run
```

后端 API 将在 `http://localhost:8080` 上可用。

### 2. 前端启动

```bash
# 进入 web 目录
cd web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端应用将可以访问 `http://localhost:5173`。

## 📦 部署指南

### 前端部署

1.  构建生产环境资源包：
    ```bash
    cd web
    npm run build
    ```
2.  将 `web/dist` 目录下的内容部署到 Nginx 等静态文件服务器。
3.  配置服务器将 API 请求 (例如 `/api/*`) 代理到后端服务。

### 后端部署

1.  打包成可执行的 JAR 文件：
    ```bash
    mvn clean package
    ```
2.  将 `target/*.jar` 文件复制到你的服务器。
3.  运行应用：
    ```bash
    java -jar your-app-name.jar --spring.profiles.active=prod
    ```
    请确保提供了正确的生产环境配置文件。

## 📚 详细文档

更多详细信息，请参阅我们的文档：

-   **[项目进度报告](./docs/project_status.md)**: 功能实现的详细进度报告。

## 🤝 贡献指南

我们欢迎任何贡献！请遵循以下准则：

-   Commit 提交信息应遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范。
-   请确保你的代码遵循项目的格式化标准 (前端为 ESLint/Prettier, 后端为 Checkstyle/PMD)。

## 📄 许可证

本项目基于 [MIT 许可证](./LICENSE) 授权。
