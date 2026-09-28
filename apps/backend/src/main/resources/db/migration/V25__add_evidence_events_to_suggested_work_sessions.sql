ALTER TABLE suggested_work_sessions
ADD COLUMN IF NOT EXISTS evidence_events JSONB;