-- V9__extend_git_commits_for_github.sql
-- git_commits already exists from V1 with the base columns (commit_hash, repository_name, commit_message, commit_time, lines_added, lines_removed).
-- this just adds what github oauth sync needs on top. additive only, same convention as V7, not touching V1-V7
-- Author: Zamokuhle Zwane

ALTER TABLE git_commits ADD COLUMN repository_url VARCHAR(500);
ALTER TABLE git_commits ADD COLUMN author_name VARCHAR(255);
ALTER TABLE git_commits ADD COLUMN author_email VARCHAR(255);
ALTER TABLE git_commits ADD COLUMN github_author_login VARCHAR(255);
ALTER TABLE git_commits ADD COLUMN changed_files INTEGER;
ALTER TABLE git_commits ADD COLUMN updated_at TIMESTAMP DEFAULT NOW();

CREATE UNIQUE INDEX ux_git_commits_member_hash
    ON git_commits (workspace_member_id, commit_hash);

CREATE INDEX idx_git_commits_member_time
    ON git_commits (workspace_member_id, commit_time);

CREATE INDEX idx_git_commits_member_repo
    ON git_commits (workspace_member_id, repository_name);

-- provider rename, V1 has: provider VARCHAR(20) CHECK(provider IN ('JIRA','GOOGLE_CALENDAR','GIT'))
UPDATE integration_tokens SET provider = 'GITHUB' WHERE provider = 'GIT';

ALTER TABLE integration_tokens
    DROP CONSTRAINT IF EXISTS integration_tokens_provider_check;

ALTER TABLE integration_tokens
    ADD CONSTRAINT integration_tokens_provider_check
    CHECK (provider IN ('JIRA', 'GOOGLE_CALENDAR', 'GITHUB'));