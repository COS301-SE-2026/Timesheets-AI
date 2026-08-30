-- V6__seed_ai_insights_weekly_summary.sql
-- Seed data covering every insight type x scope x overall/by-project
-- combination, so the /api/insights proxy has something real to read
-- against for every card on the Dashboard/Insights pages before the
-- FastAPI jobs have actually run against live data.
-- Additive only, per team convention (no schema changes, no edits to
-- V1-V5). Depends on V5 (scope, narrative columns) already having run.

-- People used below, all from V2/V3:
-- Thabang Siduke      (0001-000000000020) DEVELOPER, Mobile App + Backend API + Design System
-- Karabo Mathebula     (0005-000000000024) DEVELOPER, Design System + Mobile App
-- Amahle Dlamini       (0004-000000000023) MANAGER, PM on Mobile App
-- Joshua Botes         (0008-000000000027) MANAGER, PM on Backend API
-- Lubanzi Gcabashe     (0006-000000000025) DEVELOPER, DevOps Pipeline, has the V3 REJECTED timesheet
-- Faith Solomons       (0009-000000000028) ADMIN, PM on DevOps Pipeline
-- Enzokuhle            (0002-000000000021) MANAGER-ish role, PM on Client Portal Redesign

-- Projects used below, all from V2/V3:
-- Mobile App                (0001-000000000040)
-- Backend API                (0002-000000000041)
-- Design System               (0003-000000000042)
-- DevOps Pipeline              (0004-000000000043)
-- Client Portal Redesign        (0001-000000000200)



-- 1. WEEKLY_SUMMARY
-- scope=USER, overall and by-project, plus scope=TEAM for a manager


-- developer, overall, cross-project week
INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, narrative, created_at) VALUES
('00000000-0000-0000-0001-000000000300', '00000000-0000-0000-0001-000000000020', NULL, 'WEEKLY_SUMMARY', 'USER',
 'You logged 9.5h across 2 projects this week, mostly on the JWT authentication task for Backend API. Productivity held steady compared to last week, no anomalies flagged.',
 NOW());

-- same developer, by project, Backend API only
INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, narrative, created_at) VALUES
('00000000-0000-0000-0001-000000000301', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0002-000000000041', 'WEEKLY_SUMMARY', 'USER',
 'On Backend API this week: 5.5h logged, mainly JWT authentication. This is your highest-effort project this week.',
 NOW());

-- a second developer, overall, different week shape (lighter week)
INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, narrative, created_at) VALUES
('00000000-0000-0000-0001-000000000302', '00000000-0000-0000-0005-000000000024', NULL, 'WEEKLY_SUMMARY', 'USER',
 'You logged 4.0h this week on Design System, lighter than your usual pace. No flagged entries.',
 NOW());

-- manager, team-wide narrative
INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, narrative, created_at) VALUES
('00000000-0000-0000-0001-000000000303', '00000000-0000-0000-0004-000000000023', NULL, 'WEEKLY_SUMMARY', 'TEAM',
 'The team logged 34.5h this week across 4 active projects. Backend API saw the most activity. No burnout risks flagged, one timesheet still outstanding.',
 NOW());



-- 2. PRODUCTIVITY
-- scope=USER, overall and by-project, across three different developers
-- so the manager comparison card has more than one point of data


INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, score, created_at) VALUES
-- Thabang, overall and by-project
('00000000-0000-0000-0001-000000000310', '00000000-0000-0000-0001-000000000020', NULL, 'PRODUCTIVITY', 'USER', 87.00, NOW()),
('00000000-0000-0000-0001-000000000311', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0002-000000000041', 'PRODUCTIVITY', 'USER', 91.00, NOW()),

-- Karabo, overall only, lower score so the low-score recommendation path has data to show
('00000000-0000-0000-0001-000000000312', '00000000-0000-0000-0005-000000000024', NULL, 'PRODUCTIVITY', 'USER', 54.00, NOW()),

-- Lubanzi, overall, high score, so the high-score recommendation path has data too
('00000000-0000-0000-0001-000000000313', '00000000-0000-0000-0006-000000000025', NULL, 'PRODUCTIVITY', 'USER', 162.00, NOW());

-- a 6-week trend for Thabang, overall scope, so the productivity trend
-- chart (period=6w) has more than one point to actually draw a line
INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, score, created_at) VALUES
('00000000-0000-0000-0001-000000000314', '00000000-0000-0000-0001-000000000020', NULL, 'PRODUCTIVITY', 'USER', 78.00, NOW() - INTERVAL '5 weeks'),
('00000000-0000-0000-0001-000000000315', '00000000-0000-0000-0001-000000000020', NULL, 'PRODUCTIVITY', 'USER', 81.00, NOW() - INTERVAL '4 weeks'),
('00000000-0000-0000-0001-000000000316', '00000000-0000-0000-0001-000000000020', NULL, 'PRODUCTIVITY', 'USER', 79.00, NOW() - INTERVAL '3 weeks'),
('00000000-0000-0000-0001-000000000317', '00000000-0000-0000-0001-000000000020', NULL, 'PRODUCTIVITY', 'USER', 85.00, NOW() - INTERVAL '2 weeks'),
('00000000-0000-0000-0001-000000000318', '00000000-0000-0000-0001-000000000020', NULL, 'PRODUCTIVITY', 'USER', 83.00, NOW() - INTERVAL '1 week');



-- 3. TASK_SWITCHING
-- scope=USER, overall and by-project


INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, score, description, created_at) VALUES
('00000000-0000-0000-0001-000000000320', '00000000-0000-0000-0001-000000000020', NULL, 'TASK_SWITCHING', 'USER', 3.20,
 'Averaged 3.2 task switches per day over the period.', NOW()),
('00000000-0000-0000-0001-000000000321', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0002-000000000041', 'TASK_SWITCHING', 'USER', 1.50,
 'Averaged 1.5 task switches per day over the period, on Backend API.', NOW()),

-- a high-switching example, since this is the kind of thing worth a card looking noticeably different from the norm
('00000000-0000-0000-0001-000000000322', '00000000-0000-0000-0005-000000000024', NULL, 'TASK_SWITCHING', 'USER', 8.70,
 'Averaged 8.7 task switches per day over the period, notably higher than usual.', NOW());



-- 4. BURNOUT
-- always scope=TEAM per the plan doc, manager-visible only, regardless of who it's about
-- covering HIGH, MEDIUM, and LOW so the manager list isn't all one risk level


INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, description, recommendation, created_at) VALUES
-- Lubanzi, HIGH risk, matches the V3 REJECTED timesheet's overtime reason so this reads as a believable, connected story
('00000000-0000-0000-0001-000000000330', '00000000-0000-0000-0006-000000000025', NULL, 'BURNOUT', 'TEAM',
 'Sustained long days logged this week including two 10+ hour sessions on DevOps Pipeline.',
 'Check in before assigning further overtime on this project.',
 NOW()),

-- Thabang, MEDIUM risk, weekend logging pattern
('00000000-0000-0000-0001-000000000331', '00000000-0000-0000-0001-000000000020', NULL, 'BURNOUT', 'TEAM',
 'Weekend logging detected in 2 of the last 4 weeks.',
 NULL,
 NOW()),

-- Karabo, LOW risk, so the manager list has a calm baseline row too, not just flagged people
('00000000-0000-0000-0001-000000000332', '00000000-0000-0000-0005-000000000024', NULL, 'BURNOUT', 'TEAM',
 'No sustained long-hour days detected this period.',
 NULL,
 NOW());



-- 5. DELIVERY_FORECAST
-- scope=USER, tied to real in-progress tasks from V3 (Client Portal
-- Redesign and Legacy Reporting Migration both have live IN_PROGRESS
-- tasks with due dates and partial actual_hours, good forecast candidates)


INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, description, created_at) VALUES
-- Enzokuhle, "Design New Portal Layout" task (V3, IN_PROGRESS, due 2026-08-10, 6.5 of 12 estimated hours logged)
('00000000-0000-0000-0001-000000000340', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000200', 'DELIVERY_FORECAST', 'USER',
 'At current pace (0.9h/day), this task is projected to finish around 2026-08-14, 4 days after the planned date of 2026-08-10.',
 NOW()),

-- Amahle, "Audit Existing Crystal Reports" task (V3, IN_PROGRESS, due 2026-08-05, 3.0 of 6.0 estimated hours logged)
('00000000-0000-0000-0001-000000000341', '00000000-0000-0000-0004-000000000023', '00000000-0000-0000-0002-000000000201', 'DELIVERY_FORECAST', 'USER',
 'At current pace (1.1h/day), this task is projected to finish around 2026-08-04, ahead of the planned date of 2026-08-05.',
 NOW());



-- 6. ANOMALY
-- scope=USER, tied to the real time_entries from V3's rejected-timesheet
-- overtime block, this is the most natural existing example of an entry
-- that would actually get flagged


INSERT INTO ai_insights (id, workspace_member_id, project_id, time_entry_id, insight_type, scope, confidence, description, created_at) VALUES
('00000000-0000-0000-0001-000000000350', '00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0004-000000000043', '00000000-0000-0000-0006-000000000162', 'ANOMALY', 'USER', 88.00,
 'Entry duration of 11.0h is unusually long compared to this person''s typical entries.',
 NOW()),

-- a second, lower-confidence flag on the other overtime entry from the same block
('00000000-0000-0000-0001-000000000351', '00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0004-000000000043', '00000000-0000-0000-0006-000000000161', 'ANOMALY', 'USER', 72.00,
 'Entry duration of 10.0h is longer than this person''s typical entries.',
 NOW());