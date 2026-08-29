-- V5__extend_ai_insights_scope_and_weekly_summary.sql
-- Adds scope and narrative to ai_insights, extends the insight_type CHECK
-- constraint to allow WEEKLY_SUMMARY. See dashboard-insights-plan.md.

ALTER TABLE ai_insights
    ADD COLUMN scope VARCHAR(10) NOT NULL DEFAULT 'USER';

ALTER TABLE ai_insights
    ADD CONSTRAINT chk_ai_insights_scope CHECK (scope IN ('USER', 'TEAM'));

ALTER TABLE ai_insights
    ADD COLUMN narrative TEXT NULL;

ALTER TABLE ai_insights
    DROP CONSTRAINT IF EXISTS ai_insights_insight_type_check;

ALTER TABLE ai_insights
    ADD CONSTRAINT ai_insights_insight_type_check
    CHECK (insight_type IN (
        'PRODUCTIVITY', 'ANOMALY', 'BURNOUT',
        'DELIVERY_FORECAST', 'TASK_SWITCHING', 'WEEKLY_SUMMARY'
    ));

CREATE INDEX idx_ai_insights_lookup
    ON ai_insights (workspace_member_id, insight_type, scope, project_id);