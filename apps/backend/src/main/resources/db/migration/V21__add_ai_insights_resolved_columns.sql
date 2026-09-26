-- V21__add_ai_insights_resolved_columns.sql lets a burnout insight (or any insight) be marked resolved instead of
-- sitting in the list forever. resolved_by points at the workspace member who clicked resolve, same shape as workspace_id/workspace_member_id
-- already on this table.

ALTER TABLE ai_insights
    ADD COLUMN resolved BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE ai_insights
    ADD COLUMN resolved_at TIMESTAMP NULL;

ALTER TABLE ai_insights
    ADD COLUMN resolved_by_workspace_member_id UUID NULL;

ALTER TABLE ai_insights
    ADD CONSTRAINT fk_ai_insights_resolved_by
        FOREIGN KEY (resolved_by_workspace_member_id)
        REFERENCES workspace_members(id)
        ON DELETE SET NULL;

CREATE INDEX idx_ai_insights_resolved
    ON ai_insights (workspace_id, resolved)
    WHERE resolved = FALSE;