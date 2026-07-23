

-- Enable pgcrypto
CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- Drop existing tables if they exist (CAREFUL: This deletes data!)
-- DROP TABLE IF EXISTS audit_logs CASCADE;
-- DROP TABLE IF EXISTS notifications CASCADE;
-- DROP TABLE IF EXISTS ai_insights CASCADE;
-- DROP TABLE IF EXISTS git_commits CASCADE;
-- DROP TABLE IF EXISTS calendar_events CASCADE;
-- DROP TABLE IF EXISTS jira_tickets CASCADE;
-- DROP TABLE IF EXISTS user_availability CASCADE;
-- DROP TABLE IF EXISTS time_entries CASCADE;
-- DROP TABLE IF EXISTS timer_sessions CASCADE;
-- DROP TABLE IF EXISTS timesheets CASCADE;
-- DROP TABLE IF EXISTS tasks CASCADE;
-- DROP TABLE IF EXISTS project_members CASCADE;
-- DROP TABLE IF EXISTS projects CASCADE;
-- DROP TABLE IF EXISTS workspace_members CASCADE;
-- DROP TABLE IF EXISTS workspaces CASCADE;
-- DROP TABLE IF EXISTS users CASCADE;


CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    avatar_url TEXT,
    job_title VARCHAR(150),
    seniority_level VARCHAR(20) CHECK (seniority_level IN ('JUNIOR','SENIOR','LEAD')),
    employment_type VARCHAR(20) CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT')),
    email_verified BOOLEAN DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    theme VARCHAR(10) DEFAULT 'LIGHT' CHECK (theme IN ('LIGHT','DARK')),
    email_notifications BOOLEAN DEFAULT TRUE,
    jira_enabled BOOLEAN DEFAULT FALSE,
    calendar_enabled BOOLEAN DEFAULT FALSE,
    git_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE workspace_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL UNIQUE REFERENCES workspaces(id) ON DELETE CASCADE,
    reporting_period_type VARCHAR(20) NOT NULL DEFAULT 'WEEKLY' CHECK (reporting_period_type IN ('WEEKLY','MONTHLY','CUSTOM')),
    week_start_day VARCHAR(10) DEFAULT 'MONDAY' CHECK (week_start_day IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    custom_period_start_day INTEGER CHECK (custom_period_start_day BETWEEN 1 AND 31),
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE workspace_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','MANAGER','DEVELOPER')),
    joined_at TIMESTAMP DEFAULT now(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(workspace_id,user_id)
);

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','ON_HOLD', 'COMPLETED', 'ARCHIVED')),
    budget_hours NUMERIC(10,2),
    budget_cost NUMERIC(15,2),
    hourly_rate NUMERIC(10,2),
    start_date DATE,
    end_date DATE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_by_workspace_member_id UUID REFERENCES workspace_members(id) ON DELETE RESTRICT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE project_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    workspace_member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE CASCADE,
    is_project_manager BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE(project_id,workspace_member_id)
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE RESTRICT,
    jira_ticket_key VARCHAR(50),
    parent_task_id UUID REFERENCES tasks(id),
    title VARCHAR(300) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'TODO' CHECK(status IN ('TODO','IN_PROGRESS','DONE', 'BLOCKED')),
    priority VARCHAR(10) DEFAULT 'MEDIUM' CHECK(priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    estimated_hours NUMERIC(8,2),
    actual_hours NUMERIC(8,2),
    assigned_workspace_member_id UUID REFERENCES workspace_members(id),
    due_date DATE,
    completed_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE timesheets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED')),
    submitted_at TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by_workspace_member_id UUID REFERENCES workspace_members(id) ON DELETE RESTRICT,
    rejected_at TIMESTAMP,
    rejection_reason TEXT,
    is_locked BOOLEAN DEFAULT FALSE,
    locked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE timer_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE RESTRICT,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE RESTRICT,
    task_id UUID REFERENCES tasks(id) ON DELETE RESTRICT,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    paused_duration_seconds BIGINT DEFAULT 0,
    is_paused BOOLEAN DEFAULT FALSE,
    paused_at TIMESTAMP,
    is_running BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE time_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timesheet_id UUID REFERENCES timesheets(id) ON DELETE SET NULL,
    workspace_member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE RESTRICT,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE RESTRICT,
    task_id UUID REFERENCES tasks(id) ON DELETE RESTRICT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_seconds INTEGER,
    entry_type VARCHAR(10) DEFAULT 'MANUAL' CHECK(entry_type IN ('MANUAL','TIMER')),
    description TEXT,
    is_locked BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE user_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL CHECK(status IN ('AVAILABLE','UNAVAILABLE','ON_LEAVE','PARTIAL')),
    start_date DATE NOT NULL,
    end_date DATE,
    reason TEXT,
    created_at TIMESTAMP DEFAULT now(),
);

CREATE TABLE leave_requests(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE CASCADE,
    leave_type VARCHAR(30) NOT NULL CHECK(leave_type IN ('ANNUAL', 'SICK', 'MATERNITY', 'PATERNITY', 'FAMILY_RESPONSIBILITY', 'OTHER')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days NUMERIC (4,1) NOT NULL,
    reason TEXT,
    attachments JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    approved_by_workspace_member_id UUID REFERENCES workspace_members(id) ON DELETE SET NULL,
    approved_at TIMESTAMP, 
    rejection_reason TEXT, 
    availability_id UUID REFERENCES user_availability(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

COMMENT ON COLUMN leave_requests.attachments IS 'JSONB array of attachement objects:
{
    "files": [
        {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "name": "medical-certificate.pdf",
        "type": "application/pdf",
        "size": 102400,
        "url": "https://storage.example.com/uploads/medical-certificate.pdf",
        "uploadedAt": "2026-07-23T10:00:00"
        }
    ]
}';


CREATE TABLE jira_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES projects(id) ON DELETE RESTRICT,
    jira_ticket_key VARCHAR(50) NOT NULL,
    summary TEXT,
    jira_status VARCHAR(50),
    issue_type VARCHAR(30),
    estimated_hours NUMERIC(8,2),
    logged_hours NUMERIC(8,2),
    last_synced TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE calendar_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID REFERENCES workspace_members(id) ON DELETE SET NULL,
    event_title VARCHAR(255),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    external_event_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE git_commits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID REFERENCES workspace_members(id) ON DELETE RESTRICT,
    project_id UUID REFERENCES projects(id) ON DELETE RESTRICT,
    commit_hash VARCHAR(100) NOT NULL,
    repository_name VARCHAR(255),
    commit_message TEXT,
    commit_time TIMESTAMP,
    lines_added INTEGER,
    lines_removed INTEGER,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE ai_insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID REFERENCES workspace_members(id) ON DELETE RESTRICT,
    project_id UUID REFERENCES projects(id) ON DELETE RESTRICT,
    time_entry_id UUID REFERENCES time_entries(id) ON DELETE RESTRICT,
    insight_type VARCHAR(30) CHECK(insight_type IN ('PRODUCTIVITY','ANOMALY','BURNOUT','DELIVERY_FORECAST','TASK_SWITCHING')),
    score NUMERIC(5,2),
    confidence NUMERIC(5,2),
    description TEXT,
    recommendation TEXT,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID REFERENCES workspace_members(id) ON DELETE SET NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    entity_type VARCHAR(50),
    entity_id UUID,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID REFERENCES workspaces(id) ON DELETE RESTRICT,
    workspace_member_id UUID REFERENCES workspace_members(id) ON DELETE RESTRICT,
    project_id UUID REFERENCES projects(id) ON DELETE RESTRICT,
    entity_type VARCHAR(50),
    entity_id UUID,
    action VARCHAR(100) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE user_identity_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL CHECK (provider IN ('GOOGLE','MICROSOFT')),
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE(provider, provider_user_id)
);

CREATE TABLE user_mfa (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    secret_key TEXT NOT NULL,
    is_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE integration_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE CASCADE,
    provider VARCHAR(20) CHECK(provider IN ('JIRA','GOOGLE_CALENDAR','GIT')),
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now()
);