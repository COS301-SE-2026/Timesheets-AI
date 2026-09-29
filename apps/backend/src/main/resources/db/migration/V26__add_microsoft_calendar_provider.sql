ALTER TABLE integration_tokens
    DROP CONSTRAINT IF EXISTS integration_tokens_provider_check;

ALTER TABLE integration_tokens
    ADD CONSTRAINT integration_tokens_provider_check
    CHECK (
        provider IN ('JIRA', 'GOOGLE_CALENDAR', 'GITHUB', 'MICROSOFT_CALENDAR')
    );