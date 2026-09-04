-- V13__seed_enzokuhle_productivity_trend.sql
-- Adds 5 weeks of historical PRODUCTIVITY history for Enzokuhle Khumalo
-- (workspace_member_id 0002-000000000021), on top of the current week
-- already seeded in V12, so the "Productivity trend, last 6 weeks" chart
-- on the Developer Insights page has real points to render.
-- Additive only, no schema changes. Depends on V12.
--
-- Same weekly cadence used for Thabang in V6 (Mondays, going back).

INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, score, created_at) VALUES
('00000000-0000-0000-0002-000000000730', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 79.00, '2026-08-25 13:05:18.992465'),
('00000000-0000-0000-0002-000000000731', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 82.00, '2026-08-18 13:05:18.992465'),
('00000000-0000-0000-0002-000000000732', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 88.00, '2026-08-11 13:05:18.992465'),
('00000000-0000-0000-0002-000000000733', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 85.00, '2026-08-04 13:05:18.992465'),
('00000000-0000-0000-0002-000000000734', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 90.00, '2026-07-28 13:05:18.992465');

-- Per-project history too, so "By project" has more than a single point
-- for Mobile App once byProject is wired up.
INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, score, created_at) VALUES
('00000000-0000-0000-0002-000000000735', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'PRODUCTIVITY', 'USER', 79.00, '2026-08-25 13:05:18.992465'),
('00000000-0000-0000-0002-000000000736', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'PRODUCTIVITY', 'USER', 88.00, '2026-08-11 13:05:18.992465');