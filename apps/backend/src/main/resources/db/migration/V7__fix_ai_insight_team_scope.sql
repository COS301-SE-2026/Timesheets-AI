-- V7__fix_ai_insight_team_scope.sql
-- team insights belong to a workspace, not to one workspace member. keep member ownership for user-scoped insights.

ALTER TABLE ai_insights
    ADD COLUMN workspace_id UUID;

ALTER TABLE ai_insights
    ADD CONSTRAINT fk_ai_insights_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id)
        ON DELETE RESTRICT;

-- backfill existing rows before tightening the constraint.
UPDATE ai_insights ai
SET workspace_id = wm.workspace_id
FROM workspace_members wm
WHERE ai.workspace_member_id = wm.id
  AND ai.workspace_id IS NULL;

-- team insights should not pretend to belong to one person.
UPDATE ai_insights
SET workspace_member_id = NULL
WHERE scope = 'TEAM';

ALTER TABLE ai_insights
    ADD CONSTRAINT chk_ai_insights_scope_owner
    CHECK (
        (scope = 'USER' AND workspace_member_id IS NOT NULL)
        OR
        (scope = 'TEAM' AND workspace_id IS NOT NULL)
    );

CREATE INDEX idx_ai_insights_workspace_lookup
    ON ai_insights (workspace_id, insight_type, scope, project_id);

CREATE INDEX idx_ai_insights_member_lookup
    ON ai_insights (workspace_member_id, insight_type, scope, project_id);