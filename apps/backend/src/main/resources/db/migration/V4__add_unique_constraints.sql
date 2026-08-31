-- V4__add_unique_constraints.sql
-- adding these constraints to prevent race conditions and data inconsistencies


-- timer sessions
--this should prevent multiple active timers per user, across all workspaces
CREATE UNIQUE INDEX IF NOT EXISTS unique_active_timer_per_user 
ON timer_sessions (workspace_member_id) 
WHERE is_running = true;


-- timesheets
-- preventing multiple draft timesheets for the same period
CREATE UNIQUE INDEX IF NOT EXISTS unique_draft_timesheet_per_member_period 
ON timesheets (workspace_member_id, period_start, period_end) 
WHERE status = 'DRAFT';


-- time entries
-- preventing overlapping time entries
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE time_entries 
ADD CONSTRAINT no_overlapping_time_entries 
EXCLUDE USING gist (
    workspace_member_id WITH =,
    tsrange(start_time, COALESCE(end_time, start_time), '[)') WITH &&
) WHERE (is_deleted = false);


-- leave requests 
-- prevent overlapping leave requests
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE leave_requests 
ADD CONSTRAINT no_overlapping_leave_requests 
EXCLUDE USING gist ( 
    workspace_member_id WITH =,
    daterange(start_date, end_date, '[]') WITH &&
) WHERE (status IN ('PENDING', 'APPROVED'));


-- user availability
-- prevent overlapping availability
ALTER TABLE user_availability 
ADD CONSTRAINT no_overlapping_availability 
EXCLUDE USING gist (
    workspace_member_id WITH =,
    daterange(start_date, COALESCE(end_date, start_date), '[]') WITH &&
);


--projects
-- Prevent duplicate project names per workspace
CREATE UNIQUE INDEX IF NOT EXISTS unique_project_name_per_workspace 
ON projects (workspace_id, name) 
WHERE is_deleted = false;


-- tasks
-- prevent duplicate Jira tickets per project
CREATE UNIQUE INDEX IF NOT EXISTS unique_jira_ticket_per_project 
ON tasks (project_id, jira_ticket_key) 
WHERE jira_ticket_key IS NOT NULL AND is_deleted = false;


-- integration tokens
-- Prevent duplicate integration tokens per user per provider
CREATE UNIQUE INDEX IF NOT EXISTS unique_integration_token_per_user_provider 
ON integration_tokens (workspace_member_id, provider);



-- the btree stuff explained: 

-- btree_gist: Allows GiST indexes to work with normal data types (UUID, dates, etc.)
-- GIST: an index type to check if things overlap
-- B-tree: the normal index type to see if things are equal


-- since GiST does not know how to handle UUIDs by default so btree_gist teaches it how to handles UUIDs and other data types
