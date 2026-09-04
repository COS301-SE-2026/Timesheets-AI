-- V12__seed_enzokuhle_khumalo_ai_insights.sql
-- Demo seed data for Enzokuhle Khumalo (workspace_member_id 0002-000000000021)
-- for the current work week (Mon 2026-08-31 - Sun 2026-09-06), so every
-- AI Insights card renders with real data for this person on the frontend.
-- Additive only, per team convention (no schema changes, no edits to V1-V9).
-- Depends on V5 (scope/narrative columns), V7 (workspace_id + scope-owner check).
--
-- Enzokuhle Khumalo (0002-000000000021) DEVELOPER, works on Mobile App
-- Project: Mobile App (0001-000000000040) -> Design Dashboard UI (0002-000000000071, est. 6h, TODO)
-- Workspace: Momentum Engineering (00000000-0000-0000-0001-000000000010)

-- ============================================================
-- 1. TIMESHEET
-- ============================================================

INSERT INTO timesheets (
    id, workspace_member_id, period_start, period_end, status,
    submitted_at, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0002-000000000700',
    '00000000-0000-0000-0002-000000000021',
    '2026-08-31', '2026-09-06',
    'SUBMITTED',
    '2026-09-04 10:00:00',
    NOW(), NOW()
);

-- ============================================================
-- 2. TIME ENTRIES
-- ============================================================

-- Enzokuhle's week: Mobile App (Design Dashboard UI), 13h total,
-- including one long Tuesday session that feeds the ANOMALY insight below
INSERT INTO time_entries (
    id, timesheet_id, workspace_member_id, project_id, task_id,
    start_time, end_time, duration_seconds, entry_type, description, created_at, updated_at
) VALUES
('00000000-0000-0000-0002-000000000710', '00000000-0000-0000-0002-000000000700', '00000000-0000-0000-0002-000000000021',
 '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000000071',
 '2026-08-31 09:00:00', '2026-08-31 12:00:00', 10800, 'MANUAL', 'Dashboard UI wireframes', NOW(), NOW()),

('00000000-0000-0000-0002-000000000711', '00000000-0000-0000-0002-000000000700', '00000000-0000-0000-0002-000000000021',
 '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000000071',
 '2026-09-01 09:00:00', '2026-09-01 18:30:00', 34200, 'MANUAL', 'Dashboard widget layout rework after stakeholder feedback', NOW(), NOW()),

('00000000-0000-0000-0002-000000000712', '00000000-0000-0000-0002-000000000700', '00000000-0000-0000-0002-000000000021',
 '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000000071',
 '2026-09-02 10:00:00', '2026-09-02 12:00:00', 7200, 'TIMER', 'Dashboard chart components', NOW(), NOW()),

('00000000-0000-0000-0002-000000000713', '00000000-0000-0000-0002-000000000700', '00000000-0000-0000-0002-000000000021',
 '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000000071',
 '2026-09-03 09:00:00', '2026-09-03 11:00:00', 7200, 'MANUAL', 'Dashboard UI review fixes', NOW(), NOW()),

('00000000-0000-0000-0002-000000000714', '00000000-0000-0000-0002-000000000700', '00000000-0000-0000-0002-000000000021',
 '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000000071',
 '2026-09-04 09:00:00', '2026-09-04 10:00:00', 3600, 'MANUAL', 'Dashboard UI polish ahead of submission', NOW(), NOW());

-- ============================================================
-- 3. AI INSIGHTS
-- Covers every insight_type, scope=USER, so every card on Enzokuhle's
-- Insights page has something real to render.
-- ============================================================

-- --- WEEKLY_SUMMARY -----------------------------------------

INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, narrative, created_at) VALUES
('00000000-0000-0000-0002-000000000720', '00000000-0000-0000-0002-000000000021', NULL, 'WEEKLY_SUMMARY', 'USER',
 'You logged 13.0h this week on Dashboard UI, including one extended Tuesday session after stakeholder feedback. Your timesheet is submitted and awaiting review.',
 '2026-09-04 10:15:00');

-- --- PRODUCTIVITY ---------------------------------------------

INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, score, created_at) VALUES
('00000000-0000-0000-0002-000000000721', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 74.00, NOW()),
('00000000-0000-0000-0002-000000000722', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'PRODUCTIVITY', 'USER', 74.00, NOW());

-- --- TASK_SWITCHING ---------------------------------------------

INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, score, description, created_at) VALUES
('00000000-0000-0000-0002-000000000723', '00000000-0000-0000-0002-000000000021', NULL, 'TASK_SWITCHING', 'USER', 1.20,
 'Averaged 1.2 task switches per day this week, staying focused on a single task (Design Dashboard UI).', NOW());

-- --- DELIVERY_FORECAST (tied to the real in-progress task) ------

-- "Design Dashboard UI" (Mobile App, TODO, est. 6h, 13h already logged this week — over estimate)
INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, description, created_at) VALUES
('00000000-0000-0000-0002-000000000724', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'DELIVERY_FORECAST', 'USER',
 'At current pace (13.0h logged this week against a 6h estimate), "Design Dashboard UI" is projected to run over its original estimate; consider re-scoping or re-estimating the task.',
 NOW());

-- --- ANOMALY (tied to the real Tuesday long-session entry) ------

INSERT INTO ai_insights (id, workspace_member_id, project_id, time_entry_id, insight_type, scope, confidence, description, created_at) VALUES
('00000000-0000-0000-0002-000000000725', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000000711', 'ANOMALY', 'USER', 88.00,
 'Entry duration of 9.5h on Tuesday is unusually long compared to this person''s typical entries.',
 NOW());