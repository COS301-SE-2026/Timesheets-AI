CREATE TABLE project_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    workspace_member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_messages_project_created_at
ON project_messages (project_id, created_at DESC);

CREATE TABLE project_message_reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES project_messages(id) ON DELETE CASCADE,
    workspace_member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE CASCADE,
    reaction_type VARCHAR(20) NOT NULL
        CHECK (reaction_type IN ('LIKE','DISLIKE','CHECK','CROSS','IMPORTANT')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (message_id, workspace_member_id, reaction_type)
);

CREATE INDEX idx_project_message_reactions_message
ON project_message_reactions (message_id);