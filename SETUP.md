<h1 align="center">Momently: Setup Guide</h1>

<p align="center">
  <em>Every moment counts.</em>
</p>

<p align="center">
  Created by <strong>Team Cybernauts</strong> &nbsp;·&nbsp; University of Pretoria &nbsp;·&nbsp; COS 301 Capstone 2026
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/Node.js-20_LTS-339933?style=for-the-badge&logo=nodedotjs&logoColor=white" alt="Node"/>
  <img src="https://img.shields.io/badge/Python-3.11+-3776AB?style=for-the-badge&logo=python&logoColor=white" alt="Python"/>
  <img src="https://img.shields.io/badge/Docker-24+-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven"/>
  <img src="https://img.shields.io/badge/Yarn-1.22+-2C8EBB?style=for-the-badge&logo=yarn&logoColor=white" alt="Yarn"/>
</p>

<h2> Please read once before running</h2>

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [First-time setup](#2-first-time-setup)
3. [Setup options](#3-setup-options)
4. [Command reference](#4-command-reference)
5. [Docker reference](#5-docker-reference)
6. [Environment variables](#6-environment-variables)
7. [Project structure](#7-project-structure)
8. [Health check](#8-health-check)
9. [Test users](#9-test-users)

## 1. Prerequisites

Install these before anything else.

| Tool | Version | Check | Install |
|------|---------|-------|---------|
| Java | 17+ | `java -version` | [adoptium.net](https://adoptium.net) |
| Maven | 3.9+ | `mvn -version` | [maven.apache.org](https://maven.apache.org) |
| Node.js | 20 LTS | `node -v` | [nodejs.org](https://nodejs.org) |
| Yarn | 1.22+ | `yarn -v` | `npm install -g yarn` |
| Angular CLI | 17+ | `ng version` | `npm install -g @angular/cli` |
| Python | 3.11+ | `python3 --version` | [python.org](https://python.org) |
| PostgreSQL | 15 | `psql --version` | [postgresql.org](https://postgresql.org) |
| Docker | 24+ | `docker -v` | [docker.com](https://docker.com) |

<p> Not everything is required for every setup option. See [Setup options](#3-setup-options) for what you actually need. </p>

## 2. First-time setup

### 2.1 Clone and install root dependencies

```bash
git clone https://github.com/COS301-SE-2026/Timesheets-AI.git
cd Timesheets-AI
yarn install
```

### 2.2 Copy environment variables

```bash
cp .env.example .env
```
<p> Defaults in `.env` work for local development without any changes. </p>

### 2.3 First run per service

**Backend**
```bash
cd apps/backend
mvn clean install -DskipTests
```

**Frontend**
```bash
cd apps/frontend
yarn install
```

**AI service**
```bash
cd ai-service
python3 -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

<p> If you are using Docker for the database, skip creating postgres manually. `docker compose up -d postgres` handles it. </p>

## 3. Setup options

### Option A: Docker

<p align="left">
  <img src="https://img.shields.io/badge/requires-Docker_24+-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Requires Docker"/>
</p>

Use this if you want everything running with one command and do not need to actively develop a specific service.

```bash
docker compose up -d
```

All five services start automatically: postgres, redis, backend, frontend and ai-service.

### Option B: Hybrid

<p align="left">
  <img src="https://img.shields.io/badge/requires-Docker_+_Java_17_+_Node_20-0F4C91?style=for-the-badge" alt="Requires Docker and Java"/>
</p>

Use this if you are actively developing the backend or frontend and want hot reload without running everything in Docker. This is the recommended option for day-to-day development.

```bash
# Start only the database and cache
docker compose up -d postgres redis

# Then run whichever service you are working on
cd apps/backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
# or
cd apps/frontend && yarn start
# or
cd ai-service && source venv/bin/activate && uvicorn main:app --reload --port 8000
```

### Option C: Fully local

<p align="left">
  <img src="https://img.shields.io/badge/requires-Java_17_+_Node_20_+_Python_3.11_+_PostgreSQL_+_Redis-E07830?style=for-the-badge" alt="Requires everything locally"/>
</p>

Use this if you cannot run Docker on your machine. Requires postgres and redis installed and running locally.

```bash
# Create the database manually (once only)
psql -U postgres << 'EOF'
CREATE USER momently WITH PASSWORD 'momently_dev_password';
CREATE DATABASE momently_dev OWNER momently;
GRANT ALL PRIVILEGES ON DATABASE momently_dev TO momently;
EOF

# Backend (Tab 1)
cd apps/backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend (Tab 2)
cd apps/frontend
yarn start

# AI service (Tab 3)
cd ai-service
source venv/bin/activate
uvicorn main:app --reload --port 8000
```

### Option D: GitHub Codespaces

<p align="left">
  <img src="https://img.shields.io/badge/requires-GitHub_account_only-181717?style=for-the-badge&logo=github&logoColor=white" alt="Requires GitHub"/>
</p>

Use this if you are on a low-spec machine, on Windows without WSL, or want to get started without installing anything locally.

1. Go to the repo on GitHub
2. Click the green **Code** button, then **Codespaces**, then **Create codespace on dev**
3. Wait approximately 3 minutes for the environment to set up automatically
4. Run Option A or B inside the Codespaces terminal

## 4. Command reference

### Backend

Run from `apps/backend/` or use `yarn backend:<cmd>` from the repo root.

| What | Command |
|------|---------|
| Run (dev, hot reload) | `mvn spring-boot:run -Dspring-boot.run.profiles=dev` |
| Build JAR | `mvn clean package -DskipTests` |
| Build and test | `mvn clean package` |
| Test | `mvn test` |
| Test (specific class) | `mvn test -Dtest=AuthServiceTest` |
| Coverage report | `mvn verify -P coverage` |
| Lint (Checkstyle) | `mvn checkstyle:check` |
| Clean | `mvn clean` |

Coverage report: `target/site/jacoco/index.html`

### Frontend

Run from `apps/frontend/` or use `yarn frontend:<cmd>` from the repo root.

| What | Command |
|------|---------|
| Run (dev server) | `yarn start` |
| Build (production) | `yarn build --configuration production` |
| Test | `yarn test` |
| Test (watch mode) | `yarn test --watch` |
| Coverage | `yarn test --coverage` |
| Lint | `yarn lint` |
| Lint and fix | `yarn lint --fix` |
| E2E (Cypress) | `yarn e2e` |
| E2E (headless) | `yarn e2e --headless` |

### AI Service

Run from `ai-service/` with your venv active, or use `yarn ai:<cmd>` from the repo root.

| What | Command |
|------|---------|
| Run (hot reload) | `uvicorn main:app --reload --port 8000` |
| Test | `pytest` |
| Test (verbose) | `pytest -v` |
| Coverage | `pytest --cov=. --cov-report=html` |
| Lint | `ruff check .` |
| Lint and fix | `ruff check . --fix` |
| Type check | `mypy .` |

Coverage report: `htmlcov/index.html`

### Turbo (from repo root)

| What | Command |
|------|---------|
| Dev all | `yarn dev` |
| Build all | `yarn build` |
| Test all | `yarn test` |
| Lint all | `yarn lint` |
| Coverage all | `yarn coverage` |

## 5. Docker reference

### Start and stop

```bash
# Start everything
docker compose up -d

# Start only the database and redis
docker compose up -d postgres redis

# Start a specific service
docker compose up -d backend

# Stop (keeps data)
docker compose down

# Stop and wipe all data
docker compose down -v
```

### Logs

```bash
# All services
docker compose logs -f

# Single service
docker compose logs -f backend
docker compose logs -f postgres
docker compose logs -f ai-service
```

### Rebuild after code changes

```bash
docker compose build backend
docker compose up -d backend
```

### Database shell inside Docker

```bash
docker exec -it momently-postgres psql -U momently -d momently_dev
```

## 6. Environment variables

All variables are in `.env.example`. Copy it to `.env` and the defaults work without changes.

| Variable | Default | Notes |
|----------|---------|-------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/momently_dev` | Backend datasource |
| `DB_USERNAME` | `momently` | Postgres user |
| `DB_PASSWORD` | set in `.env` | Postgres password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | set in `.env` | Must be 32 or more characters |
| `JWT_EXPIRATION_MS` | `86400000` | 24 hours |
| `SERVER_PORT` | `8080` | Backend port |
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` or `prod` |

### DBeaver connection

| Field | Value |
|-------|-------|
| Host | `localhost` |
| Port | `5432` |
| Database | `momently_dev` |
| Username | `momently` |
| Password | from your `.env` file |

## 7. Project structure

```
momently/
│
├── apps/
│   ├── backend/                  # Java 17 and Spring Boot 3.5
│   │   ├── src/main/java/timesheets/
│   │   │   ├── TimesheetsApplication.java
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── domain/
│   │   │   ├── dto/
│   │   │   ├── mapper/
│   │   │   ├── security/
│   │   │   ├── config/
│   │   │   ├── exception/
│   │   │   ├── enums/
│   │   │   └── util/
│   │   ├── src/main/resources/
│   │   │   ├── application.yml
│   │   │   └── db/migration/
│   │   ├── src/test/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── checkstyle.xml
│   │
│   └── frontend/                 # Angular 17 and Material UI
│       ├── src/
│       ├── package.json
│       └── Dockerfile
│
├── ai-service/                   # Python 3.11 and FastAPI
│   ├── main.py
│   ├── requirements.txt
│   ├── pyproject.toml
│   ├── tests/
│   └── Dockerfile
│
├── infrastructure/
├── docs/
├── docker-compose.yml
├── package.json
├── turbo.json
├── .env.example
└── SETUP.md
```

## 8. Health check

After starting, verify everything is running:

| Service | URL | Expected |
|---------|-----|----------|
| Backend health | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| Swagger UI | http://localhost:8080/swagger-ui.html | Swagger page loads |
| Frontend | http://localhost:4200 | Angular app loads |
| AI service health | http://localhost:8000/health | `{"status":"ok"}` |
| AI service docs | http://localhost:8000/docs | FastAPI Swagger loads |

## 9. Test users

After Flyway migrations run, these users are available:

| Name | Email | Password | Role |
|------|-------|----------|------|
| Alice Admin | alice@momentum.co.za | Password123! | ADMIN |
| Bob Developer | bob@momentum.co.za | Password123! | DEVELOPER |
| Carol Manager | carol@momentum.co.za | Password123! | MANAGER |

Get a JWT token:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@momentum.co.za","password":"Password123!"}' | jq .
```

<p align="center">
  <strong>Momently</strong> &nbsp;·&nbsp; <em>Every moment counts.</em><br/>
  <sub>Team Cybernauts &nbsp;·&nbsp; COS 301 &nbsp;·&nbsp; University of Pretoria &nbsp;·&nbsp; 2026</sub>
</p>
