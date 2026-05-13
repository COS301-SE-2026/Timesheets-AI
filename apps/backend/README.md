# Backend Architecture Overview

## Introduction

The backend follows a **layer-first layered architecture** using **Spring Boot**.  
This architecture separates responsibilities into logical layers to improve:

- Maintainability
- Scalability
- Readability
- Separation of concerns
- Testability
- Clean dependency flow

The backend currently follows a **layer-first structure**, where components are grouped by technical responsibility (controllers, services, repositories, etc.).

As the system grows, the architecture may evolve into a **feature-first, layer-second structure** to improve scalability and modularity for larger development teams.

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

# Why This Architecture Is Used

This architecture ensures:

| Principle | Explanation |
|---|---|
| Separation of Concerns | Each layer has a single responsibility |
| Scalability | Features can grow without tightly coupling components |
| Maintainability | Easier debugging and future enhancement |
| Testability | Layers can be independently unit tested |
| Clean Architecture | Enforces proper dependency direction |
| Enterprise Readiness | Aligns with industry-standard Spring Boot practices |

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
| `controller/` | Handles HTTP requests and API endpoints | Separates API communication logic from business logic and acts as the entry point into the backend |
| `service/` | Contains business logic and workflows | Prevents controllers from becoming overloaded and centralizes system rules and operations |
| `repository/` | Handles database access and queries | Abstracts database operations and provides clean data persistence using JPA/Hibernate |
| `entity/` | Represents database tables as Java classes | Enables ORM mapping between Java objects and PostgreSQL tables |
| `dto/` | Transfers safe data between frontend and backend | Prevents exposing internal entities directly and controls API payload structure |
| `security/` | Manages authentication and authorization | Implements JWT security, access control and Spring Security configuration |
| `config/` | Contains application configuration classes | Centralizes framework and application-level configuration such as CORS and web settings |
| `resources/` | Stores configuration and SQL files | Contains application properties, environment settings and optional database seed data |
| `test/` | Contains unit and integration tests | Ensures system reliability and supports automated testing |
| `pom.xml` | Maven dependency and build configuration | Manages project dependencies, plugins and build lifecycle |
| `docker-compose.yml` | Docker container orchestration | Allows the backend and supporting services to run consistently across environments |

---

# Layer Descriptions

## Presentation Layer

Implemented using:
- Controllers
- REST APIs

Responsibilities:
- Receive frontend requests
- Validate incoming data
- Return API responses
- Delegate operations to the service layer

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
- Enforce system constraints
- Implement application logic

Examples:
- Task assignment rules
- Timesheet approval workflows
- Role-based restrictions

Examples:
- `TaskService`
- `TimesheetService`

---

## Persistence Layer

Implemented using:
- Repositories
- Entities
- JPA/Hibernate

Responsibilities:
- Interact with PostgreSQL
- Perform CRUD operations
- Map Java objects to database tables

Examples:
- `ProjectRepository`
- `TimeEntryRepository`

---

## Database Layer

Implemented using:
- PostgreSQL

Responsibilities:
- Persist application data
- Store projects, tasks, users, time entries and analytics information
- Support transactional consistency and relational integrity

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
- Role-based authorization

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
- Direct database access from controllers
- Business logic leaking into API layers

---

# Future Architectural Evolution

As the system grows, the backend may transition toward:

## Feature-First, Layer-Second Architecture

Example:

```text
modules/
├── projects/
├── tasks/
├── timesheets/
├── analytics/
└── integrations/
```

This approach improves:
- Scalability
- Team collaboration
- Feature modularity
- Large-project maintainability

---

# Technologies Used

| Area | Technology |
|---|---|
| Backend Framework | Spring Boot 3.5+ |
| Language | Java 17+ |
| Database | PostgreSQL |
| Security | Spring Security + JWT |
| ORM | JPA / Hibernate |
| API Style | RESTful APIs |
| Containerization | Docker |
| Build Tool | Maven |
| Version Control | Git & GitHub |

---

# Summary

This backend architecture is designed to provide:

- Modular system design
- Enterprise-ready layering
- Clean separation of responsibilities
- Maintainable and scalable backend development
- Secure API communication
- Reliable database interaction

The architecture aligns with standard enterprise Spring Boot layered architecture principles and provides a strong foundation for future system growth.