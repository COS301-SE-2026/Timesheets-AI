ALTER TABLE time_entries ADD COLUMN duration_seconds INTEGER;
UPDATE time_entries SET duration_seconds = duration_minutes * 60;
ALTER TABLE time_entries DROP COLUMN duration_minutes;
ALTER TABLE time_entries RENAME COLUMN duration_seconds TO duration_seconds;