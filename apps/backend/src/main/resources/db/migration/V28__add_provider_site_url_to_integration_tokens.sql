-- stores the browser URL for an external integration
-- Jira uses this for links such as https://example.atlassian.net/browse/SCRUM-18

ALTER TABLE integration_tokens
ADD COLUMN provider_site_url VARCHAR(255);