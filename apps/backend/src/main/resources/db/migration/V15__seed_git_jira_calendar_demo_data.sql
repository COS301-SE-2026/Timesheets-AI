-- V15__seed_git_jira_calendar_demo_data.sql
-- Seeds git_commits, jira_tickets, and calendar_events with realistic values
-- for the integration cards on the Insights page (git vs effort, jira vs
-- logged hours, calendar vs tracked). This is simulated third-party
-- integration data, not fabricated AI output, the ai_insights table itself
-- is untouched by this migration.
--
-- All timestamps are NOW()-relative (matching the V14 pattern) so this stays
-- valid whenever it actually gets applied, rather than pinned to a date that
-- ages out of the 7/14-day lookback windows used in dashboard.py.
--
-- People/projects reused from V2/V3/V14:
--   Amahle Dlamini    (0004-000000000023) MANAGER, PM on Mobile App
--   Thabang Siduke    (0001-000000000020) DEVELOPER, Mobile App + Backend API
--   Enzokuhle Khumalo (0002-000000000021) DEVELOPER, Mobile App
--   Lubanzi Gcabashe  (0006-000000000025) DEVELOPER, DevOps Pipeline
--
--   Mobile App      (0001-000000000040)
--   Backend API     (0002-000000000041)
--   DevOps Pipeline (0004-000000000043)
--
-- ============================================================
-- 1. GIT_COMMITS
-- Amahle had zero rows here, which is why "git commits vs effort" read 0
-- on the manager dashboard, get_github_activity() in github_activity.py
-- only looks at the requesting member's own commits.
-- ============================================================

INSERT INTO git_commits (id, workspace_member_id, project_id, commit_hash, repository_name, commit_message, commit_time, lines_added, lines_removed, created_at) VALUES
('00000000-0000-0000-0005-000000001000', '00000000-0000-0000-0004-000000000023', '00000000-0000-0000-0001-000000000040', 'a1c9f3e', 'Timesheets-AI', 'review: approve login screen PR, minor copy tweaks', NOW() - INTERVAL '1 day', 12, 4, NOW()),
('00000000-0000-0000-0005-000000001001', '00000000-0000-0000-0004-000000000023', '00000000-0000-0000-0001-000000000040', 'b28d0aa', 'Timesheets-AI', 'chore: update sprint planning notes for mobile app', NOW() - INTERVAL '2 days', 30, 2, NOW()),
('00000000-0000-0000-0005-000000001002', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0001-000000000040', 'c391b7d', 'Timesheets-AI', 'feat(login): wire up google oauth redirect', NOW() - INTERVAL '1 day', 145, 18, NOW()),
('00000000-0000-0000-0005-000000001003', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0002-000000000041', 'd48e2c1', 'Timesheets-AI', 'fix(auth): correct jwt expiry check off-by-one', NOW() - INTERVAL '3 days', 22, 9, NOW()),
('00000000-0000-0000-0005-000000001004', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'e57f1d0', 'Timesheets-AI', 'feat(dashboard): dashboard ui chart skeleton states', NOW() - INTERVAL '2 days', 210, 30, NOW()),
('00000000-0000-0000-0005-000000001005', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'f603a9c', 'Timesheets-AI', 'refactor(dashboard): extract chart config to constants', NOW() - INTERVAL '4 days', 60, 55, NOW()),
('00000000-0000-0000-0005-000000001006', '00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0004-000000000043', 'a712e4b', 'Timesheets-AI', 'fix(ci): pin node version in github actions matrix', NOW() - INTERVAL '1 day', 8, 8, NOW()),
('00000000-0000-0000-0005-000000001007', '00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0004-000000000043', 'b823f5c', 'Timesheets-AI', 'feat(ci): add staging deploy workflow', NOW() - INTERVAL '5 days', 180, 4, NOW());


-- ============================================================
-- 2. JIRA_TICKETS
-- estimated_hours/logged_hours here are intentionally close to (but not
-- identical to) the actual_hours on the matching tasks from V2/V3, since
-- a real jira sync would rarely match Momently's internal tracking exactly.
-- ============================================================

INSERT INTO jira_tickets (id, project_id, jira_ticket_key, summary, jira_status, issue_type, estimated_hours, logged_hours, last_synced, created_at) VALUES
('00000000-0000-0000-0006-000000001010', '00000000-0000-0000-0001-000000000040', 'MOM-101', 'Login screen UI', 'DONE', 'STORY', 8.0, 8.5, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0006-000000001011', '00000000-0000-0000-0001-000000000040', 'MOM-102', 'Dashboard UI widgets', 'IN_PROGRESS', 'STORY', 12.0, 14.5, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0006-000000001012', '00000000-0000-0000-0002-000000000041', 'MOM-108', 'Implement JWT Authentication', 'IN_PROGRESS', 'STORY', 10.0, 12.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0006-000000001013', '00000000-0000-0000-0004-000000000043', 'MOM-115', 'Setup GitHub Actions pipeline', 'DONE', 'TASK', 6.0, 5.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0006-000000001014', '00000000-0000-0000-0004-000000000043', 'MOM-116', 'AWS infrastructure provisioning', 'TODO', 'TASK', 16.0, 0.0, NOW() - INTERVAL '6 hours', NOW());


-- ============================================================
-- 3. CALENDAR_EVENTS
-- A mix of planning/standup meetings (calendar time that won't show up as
-- tracked time_entries) so calendarVsTracked has a genuine unmatched gap
-- to display instead of a suspiciously perfect 1:1 match.
-- ============================================================

INSERT INTO calendar_events (id, workspace_member_id, event_title, start_time, end_time, external_event_id, created_at) VALUES
('00000000-0000-0000-0007-000000001020', '00000000-0000-0000-0004-000000000023', 'Sprint planning, Mobile App', NOW() - INTERVAL '2 days' + TIME '09:00', NOW() - INTERVAL '2 days' + TIME '10:00', 'gcal-evt-2001', NOW()),
('00000000-0000-0000-0007-000000001021', '00000000-0000-0000-0004-000000000023', '1:1 with Enzokuhle', NOW() - INTERVAL '1 day' + TIME '14:00', NOW() - INTERVAL '1 day' + TIME '14:30', 'gcal-evt-2002', NOW()),
('00000000-0000-0000-0007-000000001022', '00000000-0000-0000-0001-000000000020', 'Daily standup', NOW() - INTERVAL '1 day' + TIME '09:00', NOW() - INTERVAL '1 day' + TIME '09:15', 'gcal-evt-2003', NOW()),
('00000000-0000-0000-0007-000000001023', '00000000-0000-0000-0002-000000000021', 'Design review, Dashboard UI', NOW() - INTERVAL '3 days' + TIME '11:00', NOW() - INTERVAL '3 days' + TIME '12:00', 'gcal-evt-2004', NOW()),
('00000000-0000-0000-0007-000000001024', '00000000-0000-0000-0006-000000000025', 'CI/CD incident retro', NOW() - INTERVAL '4 days' + TIME '15:00', NOW() - INTERVAL '4 days' + TIME '16:00', 'gcal-evt-2005', NOW());