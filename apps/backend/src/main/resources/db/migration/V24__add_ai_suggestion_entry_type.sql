ALTER TABLE time_entries
ALTER COLUMN entry_type TYPE VARCHAR(20);

ALTER TABLE time_entries
DROP CONSTRAINT time_entries_entry_type_check;

ALTER TABLE time_entries
ADD CONSTRAINT time_entries_entry_type_check
CHECK (entry_type IN ('MANUAL', 'TIMER', 'AI_SUGGESTION'));
