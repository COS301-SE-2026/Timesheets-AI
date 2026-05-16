# Backend Architecture

<p align="center">
  <strong>Momently Backend</strong><br/>
  Layered Spring Boot architecture for scalable enterprise development
</p>

---

## Overview

The backend for **Momently** follows a **layer-first layered architecture** using **Java 17+** and **Spring Boot 3.5+**.

The purpose of this architecture is to provide:

- Clear separation of concerns
- Maintainable backend development
- Scalable feature growth
- Cleaner dependency management
- Easier testing and debugging
- Enterprise-ready structure

The backend currently uses a **layer-first structure**, where files are grouped according to technical responsibility.

As the project grows, the system may evolve toward a **feature-first, layer-second architecture** for improved modularity and large-team scalability.

---


# Backend File Structure

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/timesheets/
│   │   │   ├── TimesheetsApplication.java
│   │   │   │
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── domain/
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── mapper/
│   │   │   ├── security/
│   │   │   ├── config/
│   │   │   └── enums/
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/migration/
│   │
│   └── test/
│       └── java/timesheets/
│           ├── service/
│           └── controller/
│
├── pom.xml
└── docker-compose.yml
```

---



# Folder Responsibilities

| Folder/File | Responsibility | Why It Is Needed |
|---|---|---|
| `controller/` | Handles REST API requests and responses | Acts as the entry point of the API and keeps HTTP handling separate from business logic |
| `service/` | Contains business rules and application workflows | Keeps core application logic organised and prevents controllers from becoming overloaded |
| `repository/` | Handles database interaction | Provides a clean abstraction for querying and updating PostgreSQL using Spring Data JPA |
| `domain/` | Represents database tables as Java classes | Allows Hibernate/JPA to map relational database tables to Java objects |
| `dto/` | Transfers structured data between layers | Helps control what data is exposed through the API and avoids exposing internal entities directly |
| `mapper/` | Converts between entities and DTOs | Centralises translation between different object types and reduces repetitive conversion code |
| `security/` | Manages security, authentication and authorisation | Handles JWT authentication, access control and Spring Security configuration |
| `config/` | Contains application configuration classes | Keeps framework setup and environment configuration organised in one place |
| `enums/` | Stores fixed sets of constants | Provides type-safe values for statuses, roles, priorities and other predefined options |
| `resources/` | Stores application configuration and SQL files | Contains properties files, environment settings and Flyway database migrations |
| `test/` | Contains automated tests | Supports unit and integration testing to improve reliability and maintainability |
| `pom.xml` | Maven dependency and build configuration | Manages project dependencies, plugins and build settings |
| `docker-compose.yml` | Container orchestration configuration | Creates consistent local development and deployment environments using Docker containers |

---

# Architectural Flow

```text
Presentation Layer
        ↓
Business Layer
        ↓
Persistence Layer
        ↓
Database
```

---

# Architectural Principles

| Principle | Explanation |
|---|---|
| Separation of Concerns | Each layer handles a specific responsibility |
| Scalability | Features can expand independently |
| Maintainability | Easier debugging and long-term enhancement |
| Testability | Layers can be unit and integration tested independently |
| Clean Dependency Flow | Prevents tightly coupled components |
| Enterprise Readiness | Aligns with standard Spring Boot architecture practices |

---

# Layer Descriptions

## Presentation Layer

Implemented using:

- Spring MVC Controllers
- RESTful APIs

Responsibilities:

- Receive frontend requests
- Validate incoming payloads
- Return API responses
- Delegate logic to services

Examples:

- `ProjectController`
- `TaskController`
- `AuthController`

---

## Business Layer

Implemented using:

- Services

Responsibilities:

- Handle business rules
- Coordinate workflows
- Enforce permissions
- Implement application behaviour

Examples:

- Timesheet approval workflows
- Role-based task restrictions
- Productivity calculations

Examples:

- `TaskService`
- `TimesheetService`
- `AuthService`

---

## Persistence Layer

Implemented using:

- Spring Data JPA
- Hibernate
- Repository classes

Responsibilities:

- Perform CRUD operations
- Execute database queries
- Map Java entities to PostgreSQL tables

Examples:

- `ProjectRepository`
- `TaskRepository`
- `TimeEntryRepository`

---

## Database Layer

Implemented using:

- PostgreSQL

Responsibilities:

- Persist application data
- Maintain relational integrity
- Support transactional consistency
- Store users, projects, tasks, time entries and analytics data

---

# Security Architecture

The backend uses:

- Spring Security
- JWT Authentication
- Stateless authentication flow

Security responsibilities include:

- User authentication
- Token validation
- Endpoint protection
- Role-based authorisation
- Secure API access

Key files:

- `JwtAuthFilter`
- `JwtUtil`
- `SecurityConfig`

---

# Dependency Flow

The backend follows strict dependency direction:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

This prevents:

- Tight coupling
- Business logic inside controllers
- Direct database access from APIs
- Poor architectural separation

---

# Technologies Used

## Core Backend Stack

<p align="left">
  <img src="https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5+-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security"/>
</p>

<p align="left">
  <img src="https://img.shields.io/badge/Hibernate-JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white" alt="Hibernate"/>
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven"/>
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/REST-API-0F4C91?style=for-the-badge" alt="REST API"/>
</p>

---

## Backend Testing Stack

<p align="left">
  <img src="https://img.shields.io/badge/JUnit_5-Testing-25A162?style=for-the-badge&logo=java&logoColor=white" alt="JUnit"/>
  <img src="https://img.shields.io/badge/Mockito-Mocking-25A162?style=for-the-badge&logo=java&logoColor=white" alt="Mockito"/>
  <img src="https://img.shields.io/badge/Spring_Boot_Test-Integration-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot Test"/>
</p>

---


# Future Architectural Evolution

As the backend grows, the project may evolve toward:

## Feature-First, Layer-Second Architecture

Example:

```text
modules/
├── auth/
├── projects/
├── tasks/
├── timesheets/
├── analytics/
└── integrations/
```

This improves:

- Feature modularity
- Team scalability
- Domain ownership
- Maintainability for large systems

---

# Summary

This backend architecture provides:

- Modular backend development
- Clean layered separation
- Enterprise-ready Spring Boot structure
- Scalable API development
- Secure authentication and authorisation
- Reliable PostgreSQL persistence
- Maintainable long-term system growth

The architecture aligns with enterprise Spring Boot layered architecture principles and provides a strong foundation for future system expansion.