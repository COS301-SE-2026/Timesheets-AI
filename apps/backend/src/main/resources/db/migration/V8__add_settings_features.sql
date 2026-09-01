-- V8: Add Settings Features
-- Description: Adds support for notification settings, 
-- account deletion, and user preferences
-- Author: Nyasha
-- Date: 2026-09-01

CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- 1. USER PREFERENCES
-- adding notification frequency columns
ALTER TABLE user_preferences 
ADD COLUMN notification_frequency VARCHAR(20) DEFAULT 'FREQUENT' 
CHECK (notification_frequency IN ('DND', 'INFREQUENT', 'FREQUENT'));

ALTER TABLE user_preferences 
ADD COLUMN dnd_start_time TIME DEFAULT '22:00:00',
ADD COLUMN dnd_end_time TIME DEFAULT '07:00:00',
ADD COLUMN dnd_enabled BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN user_preferences.notification_frequency IS 'Notification frequency preference: DND (Do Not Disturb), INFREQUENT, or FREQUENT';
COMMENT ON COLUMN user_preferences.dnd_start_time IS 'Start time for Do Not Disturb mode (e.g., 22:00:00)';
COMMENT ON COLUMN user_preferences.dnd_end_time IS 'End time for Do Not Disturb mode (e.g., 07:00:00)';
COMMENT ON COLUMN user_preferences.dnd_enabled IS 'Whether Do Not Disturb mode is enabled';

CREATE INDEX idx_user_preferences_notification_frequency ON user_preferences(notification_frequency);



-- 2. ACCOUNT DELETION - Add to Users Table
-- deletion tracking
ALTER TABLE users 
ADD COLUMN deletion_requested_at TIMESTAMP,
ADD COLUMN deletion_reason TEXT,
ADD COLUMN deletion_processed_at TIMESTAMP;

COMMENT ON COLUMN users.deletion_requested_at IS 'Timestamp when user requested account deletion';
COMMENT ON COLUMN users.deletion_reason IS 'Reason provided by user for account deletion';
COMMENT ON COLUMN users.deletion_processed_at IS 'Timestamp when deletion request was processed (approved or rejected)';

CREATE INDEX idx_users_deletion_requested_at ON users(deletion_requested_at) 
WHERE deletion_requested_at IS NOT NULL;

CREATE INDEX idx_users_deletion_processed_at ON users(deletion_processed_at)
WHERE deletion_processed_at IS NOT NULL;



-- 3. SESSION MANAGEMENT for the rememberMe part of things
--table for user_sessions
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    refresh_token VARCHAR(255) NOT NULL UNIQUE,
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),
    device_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_session_token ON user_sessions(session_token);
CREATE INDEX idx_user_sessions_refresh_token ON user_sessions(refresh_token);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);

COMMENT ON TABLE user_sessions IS 'User session management for authentication';
COMMENT ON COLUMN user_sessions.session_token IS 'JWT session token';
COMMENT ON COLUMN user_sessions.refresh_token IS 'Refresh token for session renewal';
COMMENT ON COLUMN user_sessions.device_name IS 'Device name from user agent';




-- 4. USER ACTIVITY LOG
-- creating a table for logging activity
CREATE TABLE user_activity_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL CHECK (
        activity_type IN (
            'LOGIN',
            'LOGOUT',
            'PASSWORD_CHANGE',
            'PROFILE_UPDATE',
            'SETTINGS_UPDATE',
            'DELETION_REQUESTED',
            'DELETION_CANCELLED',
            'MFA_ENABLED',
            'MFA_DISABLED',
            'INTEGRATION_CONNECTED',
            'INTEGRATION_DISCONNECTED'
        )
    ),
    activity_details JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_user_activity_log_user_id ON user_activity_log(user_id);
CREATE INDEX idx_user_activity_log_created_at ON user_activity_log(created_at DESC);
CREATE INDEX idx_user_activity_log_activity_type ON user_activity_log(activity_type);

COMMENT ON TABLE user_activity_log IS 'Audit log for user activities';
COMMENT ON COLUMN user_activity_log.activity_details IS 'JSONB details about the activity (e.g., changes made)';
COMMENT ON COLUMN user_activity_log.activity_type IS 'Type of activity performed by user';



-- 5. UPDATE EXISTING USERS WITH DEFAULT VALUES
-- updating the user preferences fot the existing users
UPDATE user_preferences 
SET 
    notification_frequency = 'FREQUENT',
    dnd_start_time = '22:00:00',
    dnd_end_time = '07:00:00',
    dnd_enabled = FALSE
WHERE notification_frequency IS NULL;



-- 6. ADMIN HELPERS
-- having a view be created for admins to see pending deletions
CREATE OR REPLACE VIEW pending_deletion_requests AS
SELECT 
    u.id,
    u.email,
    u.first_name,
    u.last_name,
    u.deletion_requested_at,
    u.deletion_reason,
    u.created_at as user_created_at,
    EXTRACT(DAY FROM (NOW() - u.deletion_requested_at))::INTEGER as days_pending,
    COUNT(DISTINCT wm.workspace_id) as workspace_count
FROM users u
LEFT JOIN workspace_members wm ON u.id = wm.user_id
WHERE u.deletion_requested_at IS NOT NULL
AND u.status != 'SUSPENDED'
GROUP BY u.id, u.email, u.first_name, u.last_name, u.deletion_requested_at, u.deletion_reason, u.created_at
ORDER BY u.deletion_requested_at ASC;

-- Create simplified view for quick admin checks
CREATE OR REPLACE VIEW pending_deletion_summary AS
SELECT 
    id,
    email,
    first_name,
    last_name,
    deletion_requested_at,
    deletion_reason,
    EXTRACT(DAY FROM (NOW() - deletion_requested_at))::INTEGER as days_pending
FROM users
WHERE deletion_requested_at IS NOT NULL
AND status != 'SUSPENDED'
ORDER BY deletion_requested_at ASC;
