-- V20__extend_ai_insights_project_forecast.sql

ALTER TABLE ai_insights
    ADD COLUMN data JSONB NULL;

ALTER TABLE ai_insights
    ADD COLUMN last_synced_at TIMESTAMP NULL;

ALTER TABLE ai_insights
    DROP CONSTRAINT IF EXISTS ai_insights_insight_type_check;

-- adding the project forecast constraint
ALTER TABLE ai_insights
    ADD CONSTRAINT ai_insights_insight_type_check
    CHECK ( insight_type IN (
            'PRODUCTIVITY',
            'ANOMALY',
            'BURNOUT',
            'DELIVERY_FORECAST',
            'TASK_SWITCHING',
            'WEEKLY_SUMMARY',
            'PROJECT_FORECAST'
        )
    );


ALTER TABLE ai_insights
    DROP CONSTRAINT IF EXISTS chk_ai_insights_scope;

ALTER TABLE ai_insights
    ADD CONSTRAINT chk_ai_insights_scope
    CHECK (scope IN ( 'USER', 'TEAM', 'PROJECT'));


ALTER TABLE ai_insights
    DROP CONSTRAINT IF EXISTS chk_ai_insights_scope_owner;

ALTER TABLE ai_insights
    ADD CONSTRAINT chk_ai_insights_scope_owner
    CHECK (
        (scope = 'USER' AND workspace_member_id IS NOT NULL)
        OR
        (scope = 'TEAM' AND workspace_id IS NOT NULL)
        OR
        ( scope = 'PROJECT' AND workspace_id IS NOT NULL AND project_id IS NOT NULL)
    );


-- index to make sure that there are no duplicate forecasts
CREATE UNIQUE INDEX unique_project_forecast_per_project
    ON ai_insights (project_id)
    WHERE scope = 'PROJECT'
    AND insight_type = 'PROJECT_FORECAST';


-- forecast lookup index
CREATE INDEX idx_ai_insights_project_forecast_lookup
    ON ai_insights (workspace_id, project_id, last_synced_at)
    WHERE scope = 'PROJECT'
    AND insight_type = 'PROJECT_FORECAST';