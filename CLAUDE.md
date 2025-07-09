# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Simple OA (Office Automation) system built with Spring Boot 3.1.5 and Vue.js 3. It provides comprehensive enterprise management functionality including OA management, project management, enterprise management, and system administration.

**Tech Stack:**
- Backend: Spring Boot 3.1.5, Spring Security, Spring Data JPA, MySQL 8.0, JWT
- Frontend: Vue.js 3, Element Plus, Pinia, Vite
- Build Tools: Maven (backend), npm/Vite (frontend)

## Common Development Commands

### Backend Commands
```bash
# Build and run backend
mvn clean install
mvn spring-boot:run

# Run tests
mvn test

# Package for production
mvn clean package
```

### Frontend Commands
```bash
# Navigate to frontend directory
cd web

# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Run linting
npm run lint
```

## Architecture Overview

### Backend Architecture
- **Package Structure**: Standard Spring Boot layered architecture under `com.example.simpleoa`
  - `controller/`: REST API endpoints
  - `service/` and `service/impl/`: Business logic layer
  - `repository/`: Data access layer using Spring Data JPA
  - `model/`: JPA entities
  - `config/`: Configuration classes including security and CORS
  - `security/`: JWT authentication components

### Frontend Architecture
- **Framework**: Vue.js 3 with Composition API
- **UI Library**: Element Plus with Chinese locale
- **State Management**: Pinia stores
- **Routing**: Vue Router with authentication guards
- **Build Tool**: Vite with proxy configuration for API calls

### Key Configuration Files
- `application.yml`: Spring Boot configuration (database, JWT, server settings)
- `web/vite.config.js`: Frontend build configuration with API proxy
- `web/package.json`: Frontend dependencies and scripts
- `pom.xml`: Backend dependencies and Maven configuration

### Security Configuration
- JWT-based authentication with configurable secret and validity
- CORS enabled for localhost:3000 and localhost:8989
- Protected API endpoints requiring authentication
- Public endpoints: `/api/auth/login`, `/api/auth/logout`

### Database Setup
- **Database**: MySQL 8.0
- **Migration Scripts**: Located in `src/main/resources/db/migration/`
- **Connection**: Configure in `application.yml` or use environment-specific profiles

## Development Environment Setup

1. **Prerequisites**: Java 17+, Node.js, MySQL 8.0
2. **Database Setup**: Create database `simpleoa_dev` and run migration scripts
3. **Backend**: Configure database connection in `application.yml`, then run `mvn spring-boot:run`
4. **Frontend**: Run `cd web && npm install && npm run dev`
5. **Access**: Frontend at http://localhost:3000, Backend API at http://localhost:8989

## Key Integration Points

- **API Proxy**: Frontend Vite dev server proxies `/api/*` requests to backend at port 8989
- **Authentication**: JWT tokens stored in frontend and sent via Authorization header
- **File Upload**: Configured for max 10MB files, stored in `uploads/` directory
- **CORS**: Configured to allow frontend-backend communication in development

## Testing

- **Backend Tests**: Located in `src/test/java/`, run with `mvn test`
- **Frontend Linting**: ESLint configured with Vue.js 3 recommended rules
- **Test Coverage**: Includes unit tests for models, repositories, and services

## Production Deployment

- **Backend**: Package with `mvn clean package`, deploy JAR file
- **Frontend**: Build with `npm run build`, serve static files via web server
- **Database**: Configure production database settings via environment variables
- **Security**: Update JWT secret and database credentials for production environment