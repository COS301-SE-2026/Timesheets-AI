CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(100)  NOT NULL,
    last_name       VARCHAR(100)  NOT NULL,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    avatar_url      TEXT,
    job_title       VARCHAR(150),
    seniority_level VARCHAR(20)   CHECK (seniority_level IN
                    ('INTERN','JUNIOR','INTERMEDIATE','SENIOR','LEAD')),
    employment_type VARCHAR(20)   CHECK (employment_type IN
                    ('FULL_TIME','PART_TIME','CONTRACT','FREELANCE')),
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED')),
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE workspaces (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150)  NOT NULL,
    owner_user_id   UUID          NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE workspace_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE workspace_role_permissions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_role_id UUID NOT NULL REFERENCES workspace_roles(id),
    permission_id     UUID NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE workspace_members (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id      UUID      NOT NULL REFERENCES workspaces(id),
    user_id           UUID      NOT NULL REFERENCES users(id),
    workspace_role_id UUID      NOT NULL REFERENCES workspace_roles(id),
    is_locked         BOOLEAN   NOT NULL DEFAULT false,
    locked_at         TIMESTAMP,
    locked_by         UUID,
    joined_at         TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, user_id)
);

CREATE TABLE projects (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id                    UUID          NOT NULL REFERENCES workspaces(id),
    client_id                       UUID,
    business_owner_id               UUID,
    name                            VARCHAR(200)  NOT NULL,
    description                     TEXT,
    status                          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                                    CHECK (status IN ('ACTIVE','ARCHIVED','ON_HOLD')),
    visibility                      VARCHAR(10)   NOT NULL DEFAULT 'PRIVATE'
                                    CHECK (visibility IN ('PUBLIC','PRIVATE')),
    budget_hours                    NUMERIC(10,2),
    budget_cost                     NUMERIC(15,2),
    hourly_rate                     NUMERIC(10,2),
    start_date                      DATE,
    end_date                        DATE,
    created_by_workspace_member_id  UUID          REFERENCES workspace_members(id),
    archived_at                     TIMESTAMP,
    created_at                      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE project_members (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id   UUID          NOT NULL REFERENCES workspace_members(id),
    project_id            UUID          NOT NULL REFERENCES projects(id),
    project_role_id       UUID,
    allocation_percentage NUMERIC(5,2),
    joined_at             TIMESTAMP,
    is_active             BOOLEAN       NOT NULL DEFAULT true,
    created_at            TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE project_managers (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id            UUID      NOT NULL REFERENCES projects(id),
    workspace_member_id   UUID      NOT NULL REFERENCES workspace_members(id),
    assigned_at           TIMESTAMP,
    created_at            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE task_statuses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE tasks (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id                      UUID         NOT NULL REFERENCES projects(id),
    jira_ticket_id                  VARCHAR(50),
    parent_task_id                  UUID         REFERENCES tasks(id),
    title                           VARCHAR(300) NOT NULL,
    description                     TEXT,
    visibility                      VARCHAR(10)  NOT NULL DEFAULT 'PUBLIC'
                                    CHECK (visibility IN ('PUBLIC','PRIVATE')),
    status_id                       UUID         NOT NULL REFERENCES task_statuses(id),
    priority                        VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM'
                                    CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    story_points                    INTEGER,
    estimated_hours                 NUMERIC(8,2),
    actual_hours                    NUMERIC(8,2),
    assigned_workspace_member_id    UUID         REFERENCES workspace_members(id),
    created_by_workspace_member_id  UUID         REFERENCES workspace_members(id),
    due_date                        DATE,
    completed_at                    TIMESTAMP,
    deleted_at                      TIMESTAMP,
    deleted_by_workspace_member_id  UUID         REFERENCES workspace_members(id),
    is_deleted                      BOOLEAN      NOT NULL DEFAULT false,
    created_at                      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE timer_sessions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id     UUID      NOT NULL REFERENCES workspace_members(id),
    project_id              UUID      NOT NULL REFERENCES projects(id),
    task_id                 UUID      REFERENCES tasks(id),
    started_at              TIMESTAMP NOT NULL,
    ended_at                TIMESTAMP,
    paused_duration_seconds BIGINT    NOT NULL DEFAULT 0,
    source                  VARCHAR(20) DEFAULT 'WEB'
                            CHECK (source IN ('WEB','MOBILE','API')),
    is_running              BOOLEAN   NOT NULL DEFAULT true,
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE time_entries (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id             UUID      NOT NULL REFERENCES workspace_members(id),
    project_id                      UUID      NOT NULL REFERENCES projects(id),
    task_id                         UUID      REFERENCES tasks(id),
    start_time                      TIMESTAMP NOT NULL,
    end_time                        TIMESTAMP,
    duration_minutes                INTEGER,
    entry_type                      VARCHAR(10) NOT NULL DEFAULT 'MANUAL'
                                    CHECK (entry_type IN ('MANUAL','TIMER')),
    description                     TEXT,
    is_locked                       BOOLEAN   NOT NULL DEFAULT false,
    locked_at                       TIMESTAMP,
    edited_at                       TIMESTAMP,
    edited_by_workspace_member_id   UUID      REFERENCES workspace_members(id),
    status                          VARCHAR(15) NOT NULL DEFAULT 'DRAFT'
                                    CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED')),
    submitted_at                    TIMESTAMP,
    reviewed_by_workspace_member_id UUID      REFERENCES workspace_members(id),
    reviewed_at                     TIMESTAMP,
    rejection_reason                TEXT,
    created_at                      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE team_relationships (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    manager_workspace_member_id     UUID NOT NULL REFERENCES workspace_members(id),
    developer_workspace_member_id   UUID NOT NULL REFERENCES workspace_members(id),
    created_at                      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE user_availability (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID        NOT NULL REFERENCES workspace_members(id),
    status              VARCHAR(20) NOT NULL
                        CHECK (status IN ('AVAILABLE','UNAVAILABLE','ON_LEAVE','PARTIAL')),
    start_date          DATE,
    end_date            DATE,
    notes               TEXT,
    created_at          TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID        NOT NULL REFERENCES workspace_members(id),
    type                VARCHAR(50) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    message             TEXT,
    entity_type         VARCHAR(50),
    entity_id           UUID,
    action_url          VARCHAR(500),
    is_read             BOOLEAN     NOT NULL DEFAULT false,
    metadata            JSONB,
    created_at          TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id        UUID,
    workspace_member_id UUID,
    project_id          UUID,
    entity_type         VARCHAR(50),
    entity_id           UUID,
    action              VARCHAR(100) NOT NULL,
    old_values          JSONB,
    new_values          JSONB,
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(500),
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);