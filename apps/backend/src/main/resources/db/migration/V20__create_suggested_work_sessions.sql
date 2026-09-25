CREATE TABLE suggested_work_sessions (
    id UUID PRIMARY KEY,
    workspace_member_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    title VARCHAR(255),
    description TEXT,
    project_id UUID,
    task_id UUID,
    confidence_score DOUBLE PRECISION,
    duration_minutes INTEGER,
    explanation TEXT,
    status VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);