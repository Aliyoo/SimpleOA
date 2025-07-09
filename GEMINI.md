# Gemini Project Configuration

This file provides context to the Gemini AI assistant to help it understand the project structure, conventions, and technologies used.

## Project Overview

This is a SimpleOA (Office Automation) system. It is a full-stack application with a Java backend and a Vue.js frontend.

## Backend (`src` directory)

-   **Language:** Java
-   **Framework:** Spring Boot
-   **Build Tool:** Maven (`pom.xml`)
-   **Database Migrations:** Flyway (`src/main/resources/db/migration`)
-   **Source Code:** `src/main/java`
-   **Resources:** `src/main/resources`

## Frontend (`web` directory)

-   **Framework:** Vue.js
-   **Build Tool:** Vite (`vite.config.js`)
-   **Package Manager:** Likely yarn or npm (`package.json`, `yarn.lock`)
-   **Source Code:** `web/src`
-   **UI Components:** `web/src/views` and `web/src/layouts`

## General Instructions

-   Please adhere to the existing code style and conventions.
-   Infer testing strategies from existing tests.
