-- V17__optimize_notifications.sql

-- indexes so that as the users grow, the queries still stay optimized


-- TIMER SESSIONS

-- used by the scheduler when looking for active timers, that started before the 8-hour threshold
-- only running timers are included in this index, which keeps
-- the index smaller than indexing every timer session
CREATE INDEX IF NOT EXISTS idx_timer_sessions_running_started_at
ON timer_sessions (started_at)
WHERE is_running = true;


-- NOTIFICATIONS
-- speeds up:
-- GET /api/notifications
-- notifications are retrieved for one workspace member
-- and ordered by created_at
CREATE INDEX IF NOT EXISTS idx_notifications_member_created_at
ON notifications (workspace_member_id, created_at DESC);


-- speeds up:
-- GET /api/notifications/unread
-- GET /api/notifications/unread/count
-- only unread notifications are stored in this index
CREATE INDEX IF NOT EXISTS idx_notifications_member_unread_created_at
ON notifications (workspace_member_id, created_at DESC)
WHERE is_read = false;


-- prevents the same long-running timer warning from being created more than once for the same time
-- this is also protection against race conditions if two checks happen very close together
CREATE UNIQUE INDEX IF NOT EXISTS unique_long_running_timer_notification
ON notifications (workspace_member_id, entity_id)
WHERE type = 'TIMER_LONG_RUNNING'
  AND entity_type = 'TIMER'
  AND workspace_member_id IS NOT NULL
  AND entity_id IS NOT NULL;