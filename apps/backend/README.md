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

# Backend File Structure

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/timesheets/
│   │   │   ├── TimesheetsApplication.java
│   │   │   │
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   ├── security/
│   │   │   └── config/
│   │   │
│   │   └── resources/
│   │
│   └── test/
│
├── pom.xml
└── docker-compose.yml
```

---

# Folder Responsibilities

| Folder/File | Responsibility | Why It Is Needed |
|---|---|---|
| `controller/` | Handles REST API requests and responses | Separates HTTP communication from business logic and acts as the API entry point |
| `service/` | Contains business rules and workflows | Centralises application logic and prevents controllers from becoming overloaded |
| `repository/` | Handles database interaction | Provides abstraction over PostgreSQL queries using Spring Data JPA |
| `entity/` | Represents database tables as Java classes | Enables ORM mapping between Java objects and relational tables |
| `dto/` | Transfers structured data between layers | Prevents exposing internal entities directly through APIs |
| `security/` | Manages authentication and authorisation | Implements JWT authentication and Spring Security configuration |
| `config/` | Contains application configuration classes | Centralises framework and environment configuration |
| `resources/` | Stores application configuration and SQL files | Contains properties, environment settings and optional seed data |
| `test/` | Contains automated tests | Supports unit testing and integration testing |
| `pom.xml` | Maven dependency and build configuration | Manages dependencies, plugins and project builds |
| `docker-compose.yml` | Container orchestration | Enables consistent local development and deployment environments |

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

## Supporting Backend Technologies

| Area | Technology |
|---|---|
| Backend Framework | Spring Boot 3.5+ |
| Language | Java 17+ |
| Security | Spring Security + JWT |
| Database | PostgreSQL 15 |
| ORM | Hibernate + JPA |
| API Style | RESTful APIs |
| Containerisation | Docker |
| Build Tool | Maven |
| Version Control | Git & GitHub |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| Documentation | Swagger / OpenAPI |
| Monitoring | Spring Boot Actuator |
| Environment Configuration | Spring Profiles |
| Integrations | Spring WebClient |

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