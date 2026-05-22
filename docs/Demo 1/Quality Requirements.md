<p align="center">
  <img src="../../assets/images/momently name.png" alt="Momently" width="260" />
</p>

<p align="center">Every moment counts</p>
<p align="center">Team Cybernauts · COS 301 · University of Pretoria · 2026</p>

<h1 align="center">Quality Requirements</h1>
This section specifies the non-functional requirements for the Momently system. Each requirement is uniquely identified, assigned to a quality attribute category, stated as a verifiable condition, and accompanied by a measurable metric or acceptance target. Requirements are prioritised as High, Medium, or Low.


## Performance (QR-PE)

| ID | Attribute | Requirement | Metric / Target | Priority |
|---|---|---|---|---|
| QR-PE-01 | Response Time | The system shall respond to all standard user interactions (page loads, time entry submissions, timesheet views) within 2 seconds under normal operating conditions. | 95th percentile response time <= 2s under load of 100 concurrent users | High |
| QR-PE-02 | Response Time | The system shall respond to report generation and AI-powered productivity insight requests within 5 seconds for datasets spanning up to 12 months. | Report generation <= 5s for up to 1 year of data per workspace | High |
| QR-PE-03 | Throughput | The system shall support a minimum of 500 simultaneous authenticated users performing time entry and timesheet operations without degradation in response time. | 500 concurrent users; no degradation beyond 10% of baseline response time | High |
| QR-PE-04 | Caching | Frequently accessed data (external API responses, user session data, productivity summaries) shall be served from the Redis cache, reducing database load by at least 40%. | Cache hit rate >= 70% for eligible endpoints; DB query reduction >= 40% | Medium |
| QR-PE-05 | AI Insight Latency | AI-generated insights (burnout risk, anomaly detection, productivity scores) shall be computed asynchronously and be available within 10 seconds of a triggering event. | AI inference latency <= 10s end-to-end for standard prediction workloads | Medium |

## Reliability (QR-RE)

| ID | Attribute | Requirement | Metric / Target | Priority |
|---|---|---|---|---|
| QR-RE-01 | Availability | The system shall maintain 99% uptime during business hours (08:00–18:00, Monday–Friday), excluding scheduled maintenance windows communicated in advance. | Uptime >= 99% (measured monthly); max unplanned downtime <= 7.2 hrs/month | High |
| QR-RE-02 | Fault Tolerance | The system shall gracefully handle failure of non-critical components (e.g. external integrations with Jira, GitHub, Calendar APIs) without disrupting core time-tracking functionality. | Core time entry and approval flows remain operational during external integration outages | High |
| QR-RE-03 | Disaster Recovery | In the event of a critical system failure, the system shall be fully restorable within two hours, with data loss not exceeding the last completed transaction. | RTO <= 2 hours; RPO = 0 (no committed transaction loss) | High |
| QR-RE-04 | Data Integrity | All time entry, timesheet, and audit log data shall be persisted with full transactional integrity; partial writes shall never be committed to the database. | Zero partial-write incidents; all DB operations wrapped in ACID transactions | High |
| QR-RE-05 | Error Handling | The system shall surface user-friendly error messages for all foreseeable failure states and log structured error details server-side for diagnostics. | All user-facing errors include a message, code, and suggested action; errors logged with timestamp, user ID, and stack trace | Medium |


## Scalability (QR-SC)

| ID | Attribute | Requirement | Metric / Target | Priority |
|---|---|---|---|---|
| QR-SC-01 | Horizontal Scaling | The system architecture shall support horizontal scaling of the Spring Boot backend and Python AI microservices via container orchestration, without requiring code changes. | New service instances deployable via Docker/AWS ECS with zero code changes | High |
| QR-SC-02 | Data Volume | The system shall maintain performance benchmarks (QR-PE-01, QR-PE-02) as the total number of time entries in the workspace grows to 1,000,000 records. | No performance regression at 1M time entry records vs 10K baseline | High |
| QR-SC-03 | User Growth | The system shall be designed to accommodate workspace growth from the initial team size to 1,000 registered users without architectural changes. | System handles 1,000 users with <= 15% increase in 95th-percentile response time | Medium |
| QR-SC-04 | AI Workload | AI/ML microservices shall be independently scalable from the core application, allowing inference workloads to be scaled up without affecting the main API response times. | AI services deployable and scalable independently; no AI scale-out affects core API SLA | Medium |


## Security (QR-SE)

| ID | Attribute | Requirement | Metric / Target | Priority |
|---|---|---|---|---|
| QR-SE-01 | Authentication | The system shall enforce multi-factor authentication (MFA) for all user logins and shall validate credentials against the organisation's company email domain. | MFA required for all logins; login with non-company email domain rejected | High |
| QR-SE-02 | Authorisation | The system shall enforce role-based access control (RBAC) at every API endpoint; no user shall access data or functionality outside their assigned workspace role. | 100% of API endpoints protected; unauthorised requests return HTTP 403 | High |
| QR-SE-03 | Session Management | User sessions shall be managed via stateless JWT tokens; tokens shall expire after 1 hour of inactivity and refresh tokens shall be invalidated on logout. | Token TTL = 1 hour; refresh tokens revoked on logout; no session stored server-side | High |
| QR-SE-04 | Data in Transit | All client–server communication shall be encrypted using TLS 1.2 or higher; unencrypted HTTP requests shall be rejected or redirected. | TLS 1.2+ enforced on all endpoints; plain HTTP connections blocked | High |
| QR-SE-05 | Data at Rest | Sensitive data (credentials, API keys, database passwords) shall never be hardcoded; secrets shall be managed via AWS Parameter Store and encrypted at rest. | Zero hardcoded secrets in codebase; all secrets retrieved from AWS Parameter Store at runtime | High |
| QR-SE-06 | Audit Logging | The system shall maintain an immutable audit log of all significant system events (time entry edits, approval decisions, role changes) including timestamp, user ID, and IP. | Audit log entries created for 100% of defined system events; logs are append-only and tamper-evident | High |
| QR-SE-07 | POPIA Compliance | The system shall handle all personal data in accordance with the Protection of Personal Information Act (POPIA); user data shall not be shared or exported without authorisation. | Data access and export operations require explicit authorisation; no PII exported without audit trail | High |


## Maintainability (QR-MA)

| ID | Attribute | Requirement | Metric / Target | Priority |
|---|---|---|---|---|
| QR-MA-01 | Code Quality | All source code shall adhere to defined coding standards; static analysis tools shall be integrated into the CI/CD pipeline and block merges on critical violations. | Zero critical static analysis violations on any merge to main branch | High |
| QR-MA-02 | Test Coverage | The backend shall maintain a minimum unit test coverage of 80%; integration tests shall cover all defined API endpoints and critical business logic paths. | Backend unit test coverage >= 80%; all REST endpoints covered by integration tests | High |
| QR-MA-03 | Documentation | All public API endpoints shall be documented via Swagger/OpenAPI; the system architecture, data models, and deployment procedures shall be documented in Markdown. | 100% of REST endpoints documented in Swagger; architecture docs maintained in repo | Medium |
| QR-MA-04 | Modifiability | The system shall use a layered or hexagonal architecture separating concerns (presentation, business logic, data access, AI/ML) so that any layer can be modified independently. | Changes to one layer require no changes in other layers as demonstrated in code reviews | High |
| QR-MA-05 | CI/CD Pipeline | A fully automated CI/CD pipeline shall build, test, and deploy the system; deployments to production shall require a passing test suite and code review approval. | Pipeline executes on every commit; production deployments blocked without passing tests and PR approval | High |
| QR-MA-06 | Environment Config | The system shall support deployment to development, staging, and production environments without code changes; all environment-specific values shall be externalised. | Deployable to any environment by changing config only; no environment-specific code | Medium |


## Usability (QR-US)

| ID | Attribute | Requirement | Metric / Target | Priority |
|---|---|---|---|---|
| QR-US-01 | Learnability | A new user with no prior training shall be able to complete a time entry, submit a timesheet, and view productivity insights within 10 minutes of first use. | User testing: >= 80% of participants complete the three tasks within 10 minutes unaided | Medium |
| QR-US-02 | Accessibility | The system UI shall comply with WCAG 2.1 Level AA accessibility guidelines, ensuring usability for users with visual or motor impairments. | WCAG 2.1 AA compliance verified via automated accessibility audit tools | Medium |
| QR-US-03 | Responsiveness | The system UI shall be fully functional and visually consistent on desktop (1920×1080), laptop (1366×768), and tablet (768px width) screen resolutions. | No layout breakage or loss of functionality on supported resolutions | Medium |
| QR-US-04 | Theme Personalisation | The system shall allow all users to switch between light and dark UI themes; the selected theme shall persist across sessions. | Theme preference persisted per user; applied on next login without additional action | Low |


## Interoperability (QR-IN)

| ID | Attribute | Requirement | Metric / Target | Priority |
|---|---|---|---|---|
| QR-IN-01 | External API Integration | The system shall integrate with Jira, GitHub, and calendar APIs via Spring WebClient; failures in external API calls shall not cause the core application to become unavailable. | External API failures handled with circuit-breaker pattern; core functions unaffected during external outage | High |
| QR-IN-02 | SSO Compatibility | The system shall be designed to support future integration with OAuth2 / OpenID Connect for Single Sign-On (SSO), a priority requirement identified by the client. | SSO integration achievable via configuration change only; no architectural refactoring required | High |
| QR-IN-03 | API Standards | All external-facing APIs shall conform to RESTful design principles and be versioned (e.g. /api/v1/); breaking changes shall never be introduced without a version increment. | API design reviewed against REST maturity model Level 2+; versioning enforced via URL prefix | Medium |


## Key Design Decisions

**Performance: Redis Caching and Asynchronous AI**
The 1-2 second response time requirement (QR-PE-01) is driven by Momentum Life's need for an efficient replacement to TMetric. Redis caching (QR-PE-04) will intercept high-frequency reads from external services (Jira, GitHub) and session data. AI insight workloads (QR-PE-05) are deliberately asynchronous so that inference latency does not block the core application.

**Reliability: ACID Transactions and RTO/RPO**
Operating within a financial services enterprise demands that time entry and audit data are always consistent. PostgreSQL's ACID guarantees (QR-RE-04) and a two-hour RTO (QR-RE-03) reflect the client's operational risk tolerance. Circuit-breaker patterns around external integrations (QR-RE-02) ensure the core timesheet workflow remains available even when Jira or GitHub APIs are unreachable.

**Security: POPIA and Stateless JWT**
Momentum Life is a financial services enterprise subject to South African POPIA legislation. QR-SE-07 formalises compliance obligations and will influence data retention, export controls, and consent workflows. Stateless JWT authentication (QR-SE-03) combined with Spring Security RBAC (QR-SE-02) satisfies the client's requirement for secure RESTful APIs while enabling future SSO integration via OAuth2 (QR-IN-02).

**Maintainability: Layered Architecture and CI/CD**
The hexagonal or layered architecture mandated by QR-MA-04 ensures that the Angular frontend, Spring Boot backend, and Python AI microservices can evolve independently. The 80% unit test coverage floor (QR-MA-02) combined with a fully automated GitHub Actions pipeline (QR-MA-05) underpins the team's Scrum strategy and ensures consistently deployable software throughout the project lifecycle.

**Scalability: Independent AI Microservices**
Separating AI/ML workloads into Python microservices (QR-SC-04) isolates compute-intensive inference from the core Spring Boot API, ensuring that scaling AI capacity does not introduce latency for standard time-tracking operations. The containerised deployment model (QR-SC-01) via Docker and AWS ECS enables elastic scaling without architectural changes as the user base grows.
