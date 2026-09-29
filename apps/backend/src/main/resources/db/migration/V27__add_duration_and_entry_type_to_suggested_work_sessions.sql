ALTER TABLE suggested_work_sessions
ADD COLUMN duration_seconds INTEGER;

ALTER TABLE suggested_work_sessions
ADD COLUMN entry_type VARCHAR(20);