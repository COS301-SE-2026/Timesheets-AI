-- V14__seed_manager_insights_full.sql
-- Full seed for the Manager Insights view, covering every card and a
-- spread of edge cases: burnout risk tiers (high/medium/low/unknown),
-- delivery forecast states (on track/at risk/unknown), a 6-week team
-- productivity trend, and per-project member comparisons.
-- Additive only, no schema changes. Depends on V7 (workspace_id +
-- scope-owner check, which allows a TEAM row to still carry a
-- workspace_member_id alongside its workspace_id) and V11-V13.
--
-- Workspace: Momentum Engineering (00000000-0000-0000-0001-000000000010)
-- Managers: Amahle Dlamini (0004-000000000023), Joshua Botes (0008-000000000027)
--
-- People used below, all from V2/V3:
--   Thabang Siduke    (0001-000000000020) DEVELOPER, Mobile App + Backend API
--   Enzokuhle Khumalo (0002-000000000021) DEVELOPER, Mobile App
--   Lethabo Maseko    (0003-000000000022) DEVELOPER, Backend API
--   Karabo Mathebula  (0005-000000000024) DEVELOPER, Design System
--   Lubanzi Gcabashe  (0006-000000000025) DEVELOPER, DevOps Pipeline
--   Naledi Mphahlele  (0007-000000000026) DEVELOPER, DevOps Pipeline
--
-- Projects, all from V2:
--   Mobile App      (0001-000000000040), ACTIVE
--   Backend API     (0002-000000000041), ACTIVE
--   Design System   (0003-000000000042), ON_HOLD
--   DevOps Pipeline (0004-000000000043), ACTIVE


-- ============================================================
-- 0. BACKFILL: the two BURNOUT rows from V11 never had a score or a
-- member attached, so toRiskLevel(null) silently reads them both as
-- 'low' regardless of what the description says. Fixing that here
-- rather than editing V11 directly, since it's already been applied.
-- ============================================================

UPDATE ai_insights
SET workspace_member_id = '00000000-0000-0000-0005-000000000024', score = 82.00
WHERE id = '00000000-0000-0000-0005-000000000630'; -- Karabo, HIGH risk

UPDATE ai_insights
SET workspace_member_id = '00000000-0000-0000-0001-000000000020', score = 22.00
WHERE id = '00000000-0000-0000-0001-000000000631'; -- Thabang, LOW risk


-- ============================================================
-- 1. BURNOUT (scope=TEAM, workspace-owned, still attributable to a
-- specific member) - covers MEDIUM, a second HIGH on a different
-- project, a second LOW baseline, and an "insufficient data" edge
-- case with no member and no score at all.
-- ============================================================

INSERT INTO ai_insights (id, workspace_id, workspace_member_id, project_id, insight_type, scope, score, description, recommendation, created_at) VALUES
-- Enzokuhle, MEDIUM - ties to her Design Dashboard UI overrun from V12/V13
('00000000-0000-0000-0004-000000000900', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'BURNOUT', 'TEAM', 55.00,
 'Enzokuhle Khumalo logged a 9.5-hour day on Dashboard UI, and the task is already tracking over its original estimate.',
 'Keep an eye on next week''s hours before the task estimate is revised.',
 NOW()),

-- Lubanzi, HIGH - different project to Karabo, so the list isn't repetitive
('00000000-0000-0000-0004-000000000901', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0004-000000000043', 'BURNOUT', 'TEAM', 88.00,
 'Lubanzi Gcabashe has logged long days on DevOps Pipeline three weeks running, with a REJECTED timesheet earlier this month.',
 'Recommend a workload review before the next CI/CD milestone.',
 NOW()),

-- Naledi, LOW - calm baseline on the same project as Lubanzi, for contrast
('00000000-0000-0000-0004-000000000902', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0007-000000000026', '00000000-0000-0000-0004-000000000043', 'BURNOUT', 'TEAM', 15.00,
 'Naledi Mphahlele''s hours have stayed within her normal range for the past month.',
 NULL,
 NOW()),

-- Edge case: no member, no score - not enough activity logged yet to
-- assess anyone specifically. Tests the "unresolved" fallback path.
('00000000-0000-0000-0004-000000000903', '00000000-0000-0000-0001-000000000010', NULL, NULL, 'BURNOUT', 'TEAM', NULL,
 'Not enough activity has been logged yet this week to assess burnout risk for newer team members.',
 NULL,
 NOW());


-- ============================================================
-- 2. DELIVERY_FORECAST (scope=TEAM) - one per project, covering on
-- track, at risk, and an ON_HOLD project with no forecast possible.
-- score here doubles as forecast confidence (>=70 reads as on track).
-- ============================================================

INSERT INTO ai_insights (id, workspace_id, project_id, insight_type, scope, score, description, created_at) VALUES
-- Mobile App - on track
('00000000-0000-0000-0004-000000000910', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0001-000000000040', 'DELIVERY_FORECAST', 'TEAM', 85.00,
 'Mobile App tasks are tracking on schedule this sprint, with Login Screen and Dashboard UI both within their estimates.',
 NOW()),

-- Backend API - at risk
('00000000-0000-0000-0004-000000000911', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0002-000000000041', 'DELIVERY_FORECAST', 'TEAM', 35.00,
 'Implement JWT Authentication is behind its 10h estimate; at the current pace it is projected to slip by roughly 2 days.',
 NOW()),

-- Design System - unknown, project is ON_HOLD so no meaningful forecast
('00000000-0000-0000-0004-000000000912', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0003-000000000042', 'DELIVERY_FORECAST', 'TEAM', NULL,
 'Design System is currently on hold; no delivery forecast is available until work resumes.',
 NOW()),

-- DevOps Pipeline - on track
('00000000-0000-0000-0004-000000000913', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0004-000000000043', 'DELIVERY_FORECAST', 'TEAM', 91.00,
 'Setup GitHub Actions is ahead of schedule; AWS Infrastructure has not started but is not yet due.',
 NOW());


-- ============================================================
-- 3. PRODUCTIVITY, workspace-level (scope=TEAM, no member, no project)
-- 6 weeks of history for the team productivity trend chart.
-- ============================================================

INSERT INTO ai_insights (id, workspace_id, insight_type, scope, score, created_at) VALUES
('00000000-0000-0000-0004-000000000920', '00000000-0000-0000-0001-000000000010', 'PRODUCTIVITY', 'TEAM', 74.00, '2026-08-25 13:05:18.992465'),
('00000000-0000-0000-0004-000000000921', '00000000-0000-0000-0001-000000000010', 'PRODUCTIVITY', 'TEAM', 78.00, '2026-08-18 13:05:18.992465'),
('00000000-0000-0000-0004-000000000922', '00000000-0000-0000-0001-000000000010', 'PRODUCTIVITY', 'TEAM', 81.00, '2026-08-11 13:05:18.992465'),
('00000000-0000-0000-0004-000000000923', '00000000-0000-0000-0001-000000000010', 'PRODUCTIVITY', 'TEAM', 69.00, '2026-08-04 13:05:18.992465'),
('00000000-0000-0000-0004-000000000924', '00000000-0000-0000-0001-000000000010', 'PRODUCTIVITY', 'TEAM', 83.00, '2026-07-28 13:05:18.992465'),
('00000000-0000-0000-0004-000000000925', '00000000-0000-0000-0001-000000000010', 'PRODUCTIVITY', 'TEAM', 71.00, NOW());


-- ============================================================
-- 4. PRODUCTIVITY, per-project + per-member (scope=TEAM, but both
-- workspace_member_id AND project_id set - allowed since V7's check
-- only requires workspace_id for TEAM rows). Gives the manager's
-- "by project" view a real member comparison.
-- ============================================================

INSERT INTO ai_insights (id, workspace_id, workspace_member_id, project_id, insight_type, scope, score, description, created_at) VALUES
-- Mobile App: Thabang, Enzokuhle, Amahle (PM)
('00000000-0000-0000-0004-000000000930', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0001-000000000040', 'PRODUCTIVITY', 'TEAM', 86.00, 'Steady week on Login Screen and JWT Authentication.', NOW()),
('00000000-0000-0000-0004-000000000931', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'PRODUCTIVITY', 'TEAM', 74.00, 'Dashboard UI took longer than planned this week.', NOW()),
('00000000-0000-0000-0004-000000000932', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0004-000000000023', '00000000-0000-0000-0001-000000000040', 'PRODUCTIVITY', 'TEAM', 90.00, 'Reviews and planning kept pace with the sprint.', NOW()),

-- Backend API: Thabang, Lethabo, Lubanzi
('00000000-0000-0000-0004-000000000933', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0002-000000000041', 'PRODUCTIVITY', 'TEAM', 90.00, 'JWT Authentication work continued at a strong pace.', NOW()),
('00000000-0000-0000-0004-000000000934', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0003-000000000022', '00000000-0000-0000-0002-000000000041', 'PRODUCTIVITY', 'TEAM', 68.00, 'Create Timesheet API is still in early planning.', NOW()),
('00000000-0000-0000-0004-000000000935', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0002-000000000041', 'PRODUCTIVITY', 'TEAM', 52.00, 'MFA work slowed alongside the DevOps overtime this week.', NOW());