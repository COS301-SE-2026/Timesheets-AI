-- Removing a user from a workspace uses a soft delete rather than physically deleting their workspace_members record. 
-- This preserves the member's identity for historical records while removing their access to the workspace.

-- When a workspace member is removed:
--   - workspace_members is marked inactive and removed_at is recorded.
--   - project_members is marked inactive so the user no longer has access to projects in the workspace.
--   - currently assigned tasks should be unassigned from the removed member, old assigned tasks remain pointing to them but they no longer have access to them
--   - historical records such as time entries, timesheets, timer sessions, leave requests, Git commits, AI insights, and audit logs are preserved.

-- If the user is later added back to the workspace, their existing workspace_members record is reactivated rather than creating a new membership.
-- Previous project memberships remain inactive until the user is explicitly added back to those projects.


-- Foreign key deletion behaviour:
-- The foreign key rules below apply to physical deletion. Normal workspace
-- removal is handled through soft deletion and does not trigger these rules.

--   RESTRICT
--      Used for historical records where the workspace member relationship must be preserved. 
--      These constraints prevent a workspace member from being physically deleted while protected historical records still reference them.
--
--   SET NULL
--     Used for records that should survive a physical deletion but do not need to retain the workspace member relationship afterwards. 
--     This includes task assignments, calendar events, and notifications.
--
--   CASCADE
--     Used for records that belong directly to the workspace membership and do not need to survive if that membership is physically deleted, such as
--     project membership records and integration tokens.
--
-- A normal "remove from workspace" operation should therefore UPDATE the
-- workspace_members record instead of DELETE it. Physical deletion should only
-- be used when permanently removing data and when all protected historical
-- relationships have been handled appropriately.


-- Workspace memberships use a soft delete so we can keep the user's previous activity when they leave the workspace.
-- is_active shows whether the user still has access to the workspace.
-- removed_at records when the membership was soft deleted.
ALTER TABLE workspace_members
ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE workspace_members
ADD COLUMN removed_at TIMESTAMP;


-- Project memberships use a soft delete so we can keep the user's previous project activity when they lose access to a project.
-- is_active shows whether the user still has access to the project.
-- removed_at records when the project membership was soft deleted.
ALTER TABLE project_members
ADD COLUMN removed_at TIMESTAMP;


-- Task assignments are kept when a workspace member is soft deleted so we can still see who was previously assigned to the task.
-- If the workspace member is physically deleted, the task is kept but the assignment is cleared.
ALTER TABLE tasks
DROP CONSTRAINT tasks_assigned_workspace_member_id_fkey;

ALTER TABLE tasks
ADD CONSTRAINT tasks_assigned_workspace_member_id_fkey
FOREIGN KEY (assigned_workspace_member_id)
REFERENCES workspace_members(id)
ON DELETE SET NULL;


-- Timesheets keep their workspace member reference when a member is soft deleted so we can still see who the timesheet belonged to.
-- Existing timesheets do not prevent a member from being soft deleted and losing access to the workspace.
-- If the workspace member is physically deleted, the deletion is blocked while timesheets still reference them so the timesheet history is preserved.
ALTER TABLE timesheets
DROP CONSTRAINT timesheets_workspace_member_id_fkey;

ALTER TABLE timesheets
ADD CONSTRAINT timesheets_workspace_member_id_fkey
FOREIGN KEY (workspace_member_id)
REFERENCES workspace_members(id)
ON DELETE RESTRICT;


-- Leave requests retain their workspace member reference when a member is removed from the workspace. 
-- Normal member removal uses soft deletion, so existing leave requests do not prevent a member from being removed.
-- Physical deletion of a workspace_members record is restricted while leave requests still reference it so the historical owner of each request can be preserved.
ALTER TABLE leave_requests
DROP CONSTRAINT leave_requests_workspace_member_id_fkey;

ALTER TABLE leave_requests
ADD CONSTRAINT leave_requests_workspace_member_id_fkey
FOREIGN KEY (workspace_member_id)
REFERENCES workspace_members(id)
ON DELETE RESTRICT;


-- User accounts use a soft delete so a user can leave the system while we keep the information needed for their previous activity.
-- deleted_at records when the user account was soft deleted.
ALTER TABLE users
ADD COLUMN deleted_at TIMESTAMP;