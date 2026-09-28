-- V22__seed_enzokuhle_full_developer_insights_demo.sql
-- Full demo seed so every card on the Developer Insights page (github
-- activity, jira tickets, time allocation, estimate vs actual, burnout,
-- task switching, time split by task, productivity trend, and my projects)
-- has real, connected data for a full year, in one demo login.
--
-- Redo of the original V22: that version (a) created a brand-new user
-- ("Cosmas Ndlovu") instead of reusing an existing one, and (b) had a real
-- bug - its BURNOUT insert set scope='TEAM' but never set workspace_id,
-- which fails V7's chk_ai_insights_scope_owner CHECK constraint
-- ((scope='USER' AND workspace_member_id IS NOT NULL) OR (scope='TEAM' AND
-- workspace_id IS NOT NULL)) and would have aborted the whole migration.
--
-- This version instead seeds a full year of activity for Enzokuhle Khumalo
-- (workspace_member_id 0002-000000000021), who already exists as of V2 -
-- no new user/workspace_member is created here - and fixes the CHECK
-- constraint bug directly (see section 8).
--
-- Additive only, no schema changes. Depends on V2 (Enzokuhle + Mobile App +
-- Client Portal Redesign + Legacy Reporting Migration), V7 (workspace_id +
-- scope-owner check on ai_insights), V12/V13 (her existing week-of-2026-08-31
-- seed + 5-week productivity history).
--
-- NOTE on categories: same heuristic caveat as the original V22 - see
-- app/services/dashboard_aggregates.py's categorize_task_title(), which only
-- recognises "Bug Fixes", "Code Review", "Feature Dev", "Meetings", falling
-- back to "Other". Task/entry titles below are picked so they land in those
-- real buckets.
--
-- NOTE on the year-long backfill and overlaps: time_entries has a
-- no_overlapping_time_entries EXCLUDE constraint per workspace_member_id
-- (V4), so a generated entry can never overlap ANY existing entry for this
-- person, regardless of project. Enzokuhle already has entries in the weeks
-- of 2026-07-13, 2026-07-20 and 2026-08-31 (V2/V3/V12). The weekly
-- generator below (section 3) explicitly skips those three weeks (by
-- offset from the current week) rather than risk colliding with them;
-- those three weeks already have their own real data from V2/V3/V12/V13, so
-- the full year is still covered, just stitched together from both sources.
--
-- All new rows are dated relative to date_trunc('week', NOW()), same
-- convention as V22/V12/V13, so this stays valid whenever it's applied -
-- run it today and "this week" in the seed lines up with today's actual week.
--
-- Workspace: Momentum Engineering (00000000-0000-0000-0001-000000000010)
-- Person:    Enzokuhle Khumalo   (00000000-0000-0000-0002-000000000021)
-- Projects:  Mobile App (00000000-0000-0000-0001-000000000040) - primary
--            Client Portal Redesign (00000000-0000-0000-0001-000000000200)
--            Legacy Reporting Migration (00000000-0000-0000-0002-000000000201)

-- ============================================================
-- 1. CURRENT WEEK TASKS on Mobile App - the "live sprint" tasks that
-- estimate_vs_actual, time_split_by_task, and the jira board below all key
-- off. A mix of DONE/IN_PROGRESS/TODO so the board doesn't look finished.
-- ============================================================

INSERT INTO tasks (id, project_id, title, status, estimated_hours, actual_hours, assigned_workspace_member_id, due_date, completed_at, created_at, updated_at) VALUES
-- Feature Dev
('00000000-0000-0000-0002-000000002080', '00000000-0000-0000-0001-000000000040', 'Implement login flow', 'DONE', 12.0, 11.5, '00000000-0000-0000-0002-000000000021', (date_trunc('week', NOW()) + INTERVAL '3 days')::date, date_trunc('week', NOW()) + INTERVAL '3 days 16:00', NOW(), NOW()),
-- Bug Fixes
('00000000-0000-0000-0002-000000002081', '00000000-0000-0000-0001-000000000040', 'Fix authentication bug', 'DONE', 9.0, 8.5, '00000000-0000-0000-0002-000000000021', (date_trunc('week', NOW()) + INTERVAL '2 days')::date, date_trunc('week', NOW()) + INTERVAL '2 days 17:00', NOW(), NOW()),
-- Code Review
('00000000-0000-0000-0002-000000002082', '00000000-0000-0000-0001-000000000040', 'Review API integration PR', 'IN_PROGRESS', 5.0, 5.0, '00000000-0000-0000-0002-000000000021', (date_trunc('week', NOW()) + INTERVAL '4 days')::date, NULL, NOW(), NOW()),
-- Other
('00000000-0000-0000-0002-000000002083', '00000000-0000-0000-0001-000000000040', 'UI polish for dashboard', 'IN_PROGRESS', 6.0, 6.0, '00000000-0000-0000-0002-000000000021', (date_trunc('week', NOW()) + INTERVAL '5 days')::date, NULL, NOW(), NOW()),
-- Other
('00000000-0000-0000-0002-000000002084', '00000000-0000-0000-0001-000000000040', 'Refactor time tracking service', 'TODO', 4.0, 4.0, '00000000-0000-0000-0002-000000000021', (date_trunc('week', NOW()) + INTERVAL '6 days')::date, NULL, NOW(), NOW());

-- ============================================================
-- 2. CURRENT WEEK TIMESHEET + TIME ENTRIES on Mobile App - ~37.5h,
-- including one deliberately long Tuesday (9.5h, single block, no
-- switching) that feeds the BURNOUT and ANOMALY insights in section 8,
-- plus a Saturday catch-up so the week isn't purely Mon-Fri.
-- ============================================================

INSERT INTO timesheets (id, workspace_member_id, period_start, period_end, status, submitted_at, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0002-000000002700',
    '00000000-0000-0000-0002-000000000021',
    date_trunc('week', NOW())::date,
    (date_trunc('week', NOW()) + INTERVAL '6 days')::date,
    'SUBMITTED',
    date_trunc('week', NOW()) + INTERVAL '4 days 17:00',
    NOW(), NOW()
);

INSERT INTO time_entries (id, timesheet_id, workspace_member_id, project_id, task_id, start_time, end_time, duration_seconds, entry_type, description, created_at, updated_at) VALUES
-- Monday: login (4h) -> standup (0.5h) -> bugfix (3.5h)  [2 switches]
('00000000-0000-0000-0002-000000002710', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000002080', date_trunc('week', NOW()) + TIME '09:00', date_trunc('week', NOW()) + TIME '13:00', 14400, 'MANUAL', 'Login flow implementation', NOW(), NOW()),
('00000000-0000-0000-0002-000000002711', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', NULL, date_trunc('week', NOW()) + TIME '13:30', date_trunc('week', NOW()) + TIME '14:00', 1800, 'MANUAL', 'Daily standup sync', NOW(), NOW()),
('00000000-0000-0000-0002-000000002712', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000002081', date_trunc('week', NOW()) + TIME '14:00', date_trunc('week', NOW()) + TIME '17:30', 12600, 'MANUAL', 'Fix authentication bug', NOW(), NOW()),

-- Tuesday: single long block, UI polish after stakeholder feedback (9.5h, no switching)
('00000000-0000-0000-0002-000000002713', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000002083', date_trunc('week', NOW()) + INTERVAL '1 day' + TIME '09:00', date_trunc('week', NOW()) + INTERVAL '1 day' + TIME '18:30', 34200, 'MANUAL', 'UI polish for dashboard, extended session after stakeholder feedback', NOW(), NOW()),

-- Wednesday: refactor (4h) -> bugfix (3h) -> standup (1.5h)  [2 switches]
('00000000-0000-0000-0002-000000002714', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000002084', date_trunc('week', NOW()) + INTERVAL '2 days' + TIME '09:00', date_trunc('week', NOW()) + INTERVAL '2 days' + TIME '13:00', 14400, 'MANUAL', 'Refactor time tracking service', NOW(), NOW()),
('00000000-0000-0000-0002-000000002715', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000002081', date_trunc('week', NOW()) + INTERVAL '2 days' + TIME '13:00', date_trunc('week', NOW()) + INTERVAL '2 days' + TIME '16:00', 10800, 'MANUAL', 'Fix authentication bug, edge cases', NOW(), NOW()),
('00000000-0000-0000-0002-000000002716', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', NULL, date_trunc('week', NOW()) + INTERVAL '2 days' + TIME '16:00', date_trunc('week', NOW()) + INTERVAL '2 days' + TIME '17:30', 5400, 'MANUAL', 'Daily standup sync', NOW(), NOW()),

-- Thursday: review (3h) -> login (1.5h)  [1 switch]
('00000000-0000-0000-0002-000000002717', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000002082', date_trunc('week', NOW()) + INTERVAL '3 days' + TIME '09:00', date_trunc('week', NOW()) + INTERVAL '3 days' + TIME '12:00', 10800, 'MANUAL', 'Review API integration PR', NOW(), NOW()),
('00000000-0000-0000-0002-000000002718', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000002080', date_trunc('week', NOW()) + INTERVAL '3 days' + TIME '12:30', date_trunc('week', NOW()) + INTERVAL '3 days' + TIME '14:00', 5400, 'MANUAL', 'Login flow, wrap-up + tests', NOW(), NOW()),

-- Friday: ui polish (3h) -> standup (1h)  [1 switch]
('00000000-0000-0000-0002-000000002719', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000002083', date_trunc('week', NOW()) + INTERVAL '4 days' + TIME '09:00', date_trunc('week', NOW()) + INTERVAL '4 days' + TIME '12:00', 10800, 'MANUAL', 'UI polish for dashboard', NOW(), NOW()),
('00000000-0000-0000-0002-000000002720', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', NULL, date_trunc('week', NOW()) + INTERVAL '4 days' + TIME '12:00', date_trunc('week', NOW()) + INTERVAL '4 days' + TIME '13:00', 3600, 'MANUAL', 'Daily standup sync', NOW(), NOW()),

-- Saturday: a bit of catch-up, no switching
('00000000-0000-0000-0002-000000002721', '00000000-0000-0000-0002-000000002700', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000002080', date_trunc('week', NOW()) + INTERVAL '5 days' + TIME '10:00', date_trunc('week', NOW()) + INTERVAL '5 days' + TIME '13:00', 10800, 'MANUAL', 'Login flow, weekend catch-up', NOW(), NOW());

-- ============================================================
-- 3. A FULL YEAR OF HISTORY on Mobile App - 48 more weeks of the same
-- 5-task/week rhythm (weeks 1-51 back from this week), so the productivity
-- trend, my-projects hours, and time-allocation buckets all have a real
-- year behind them instead of just one week.
--
-- Weeks 3, 9 and 10 back are skipped deliberately: those are the weeks of
-- 2026-08-31, 2026-07-20 and 2026-07-13, which already have Enzokuhle's own
-- time entries from V12 and V2/V3. Generating entries for those weeks here
-- risks colliding with V4's no_overlapping_time_entries EXCLUDE constraint
-- on real timestamps that already exist - simplest and safest is to just
-- let the existing seeds keep covering those three weeks.
-- ============================================================

-- 3a. Tasks: same 5-category rhythm every week, all DONE (this is history).
INSERT INTO tasks (id, project_id, title, status, estimated_hours, actual_hours, assigned_workspace_member_id, due_date, completed_at, created_at, updated_at)
SELECT
    gen_random_uuid(),
    '00000000-0000-0000-0001-000000000040',
    tmpl.title,
    'DONE',
    tmpl.est_hours,
    tmpl.act_hours,
    '00000000-0000-0000-0002-000000000021',
    (date_trunc('week', NOW()) - (wk.n || ' weeks')::interval + (tmpl.day_offset || ' days')::interval)::date,
    date_trunc('week', NOW()) - (wk.n || ' weeks')::interval + (tmpl.day_offset || ' days')::interval + TIME '17:00',
    NOW(), NOW()
FROM generate_series(1, 51) AS wk(n)
CROSS JOIN (VALUES
    (0, 'Implement login flow', 12.0, 11.5),
    (1, 'Fix authentication bug', 9.0, 8.5),
    (2, 'Review API integration PR', 5.0, 5.0),
    (3, 'UI polish for dashboard', 6.0, 6.0),
    (4, 'Refactor time tracking service', 4.0, 4.0)
) AS tmpl(day_offset, title, est_hours, act_hours)
WHERE wk.n NOT IN (3, 9, 10);

-- 3b. Timesheets: one SUBMITTED timesheet per historical week.
INSERT INTO timesheets (id, workspace_member_id, period_start, period_end, status, submitted_at, created_at, updated_at)
SELECT
    gen_random_uuid(),
    '00000000-0000-0000-0002-000000000021',
    (date_trunc('week', NOW()) - (wk.n || ' weeks')::interval)::date,
    (date_trunc('week', NOW()) - (wk.n || ' weeks')::interval + INTERVAL '6 days')::date,
    'SUBMITTED',
    date_trunc('week', NOW()) - (wk.n || ' weeks')::interval + INTERVAL '4 days 17:00',
    NOW(), NOW()
FROM generate_series(1, 51) AS wk(n)
WHERE wk.n NOT IN (3, 9, 10);

-- 3c. Time entries: the same Mon-Fri rhythm as the current week (minus the
-- Tuesday anomaly and Saturday catch-up, which are this-week-only), joined
-- back to the matching timesheet (by period_start) and task (by due_date).
INSERT INTO time_entries (id, timesheet_id, workspace_member_id, project_id, task_id, start_time, end_time, duration_seconds, entry_type, description, created_at, updated_at)
SELECT
    gen_random_uuid(),
    ts.id,
    '00000000-0000-0000-0002-000000000021',
    '00000000-0000-0000-0001-000000000040',
    t.id,
    (date_trunc('week', NOW()) - (wk.n || ' weeks')::interval + (tmpl.entry_day || ' days')::interval)::date + tmpl.start_t,
    (date_trunc('week', NOW()) - (wk.n || ' weeks')::interval + (tmpl.entry_day || ' days')::interval)::date + tmpl.end_t,
    tmpl.dur_seconds,
    'MANUAL',
    tmpl.description,
    NOW(), NOW()
FROM generate_series(1, 51) AS wk(n)
CROSS JOIN (VALUES
    (0, 0::int,     TIME '09:00', TIME '13:00', 14400, 'Login flow implementation'),
    (0, NULL::int,  TIME '13:30', TIME '14:00', 1800,  'Daily standup sync'),
    (0, 1,          TIME '14:00', TIME '17:30', 12600, 'Fix authentication bug'),
    (1, 0,          TIME '09:00', TIME '12:00', 10800, 'Login flow, OAuth redirect'),
    (1, 2,          TIME '12:00', TIME '14:00', 7200,  'Review API integration PR'),
    (1, 3,          TIME '14:00', TIME '17:00', 10800, 'UI polish for dashboard'),
    (2, 4,          TIME '09:00', TIME '13:00', 14400, 'Refactor time tracking service'),
    (2, 1,          TIME '13:00', TIME '16:00', 10800, 'Fix authentication bug, edge cases'),
    (2, NULL,       TIME '16:00', TIME '17:30', 5400,  'Daily standup sync'),
    (3, 2,          TIME '09:00', TIME '12:00', 10800, 'Review API integration PR'),
    (3, 0,          TIME '12:30', TIME '14:00', 5400,  'Login flow, wrap-up + tests'),
    (4, 3,          TIME '09:00', TIME '12:00', 10800, 'UI polish for dashboard'),
    (4, NULL,       TIME '12:00', TIME '13:00', 3600,  'Daily standup sync')
) AS tmpl(entry_day, link_task_day, start_t, end_t, dur_seconds, description)
JOIN timesheets ts
    ON ts.workspace_member_id = '00000000-0000-0000-0002-000000000021'
   AND ts.period_start = (date_trunc('week', NOW()) - (wk.n || ' weeks')::interval)::date
LEFT JOIN tasks t
    ON tmpl.link_task_day IS NOT NULL
   AND t.project_id = '00000000-0000-0000-0001-000000000040'
   AND t.assigned_workspace_member_id = '00000000-0000-0000-0002-000000000021'
   AND t.due_date = (date_trunc('week', NOW()) - (wk.n || ' weeks')::interval + (tmpl.link_task_day || ' days')::interval)::date
WHERE wk.n NOT IN (3, 9, 10);

-- ============================================================
-- 4. HISTORICAL TIME ENTRIES on her other two projects (Client Portal
-- Redesign, Legacy Reporting Migration), spread roughly monthly across the
-- year on Saturdays (so they can never overlap the Mon-Fri rhythm above),
-- purely so "My Projects" shows non-zero all-time hours on more than one
-- project, matching the wireframe's bars.
-- ============================================================

INSERT INTO time_entries (id, workspace_member_id, project_id, start_time, end_time, duration_seconds, entry_type, description, created_at, updated_at)
SELECT
    gen_random_uuid(),
    '00000000-0000-0000-0002-000000000021',
    CASE WHEN wk.n % 2 = 0 THEN '00000000-0000-0000-0001-000000000200'::uuid ELSE '00000000-0000-0000-0002-000000000201'::uuid END,
    (date_trunc('week', NOW()) - (wk.n || ' weeks')::interval + INTERVAL '5 days')::date + TIME '10:00',
    (date_trunc('week', NOW()) - (wk.n || ' weeks')::interval + INTERVAL '5 days')::date + TIME '13:00',
    10800,
    'MANUAL',
    CASE WHEN wk.n % 2 = 0 THEN 'Client Portal Redesign, portal layout work' ELSE 'Legacy Reporting Migration, report audit work' END,
    NOW(), NOW()
FROM generate_series(4, 51, 4) AS wk(n)
WHERE wk.n NOT IN (9, 10);

-- ============================================================
-- 5. GIT COMMITS - a full year of weekday commit activity on Mobile App
-- (2/day, Mon-Fri, every week including this one), so GitHub Activity has
-- a real trend behind "this week"'s numbers instead of a single spike.
-- ============================================================

INSERT INTO git_commits (id, workspace_member_id, project_id, commit_hash, repository_name, commit_message, commit_time, lines_added, lines_removed, created_at)
SELECT
    gen_random_uuid(),
    '00000000-0000-0000-0002-000000000021',
    '00000000-0000-0000-0001-000000000040',
    substr(md5(random()::text || wk.n || tmpl.entry_day || tmpl.slot), 1, 7),
    'Timesheets-AI',
    tmpl.message,
    (date_trunc('week', NOW()) - (wk.n || ' weeks')::interval + (tmpl.entry_day || ' days')::interval)::date + tmpl.commit_t,
    5 + floor(random() * 150)::int,
    2 + floor(random() * 40)::int,
    NOW()
FROM generate_series(0, 51) AS wk(n)
CROSS JOIN (VALUES
    (0, 'am', TIME '10:15', 'feat(auth): scaffold login flow'),
    (0, 'pm', TIME '15:50', 'fix(auth): correct token refresh bug'),
    (1, 'am', TIME '11:00', 'style(dashboard): polish widget spacing'),
    (1, 'pm', TIME '16:50', 'test(dashboard): widget snapshot tests'),
    (2, 'am', TIME '10:05', 'refactor(timesheets): extract shared time utils'),
    (2, 'pm', TIME '14:30', 'fix(auth): handle expired session edge case'),
    (3, 'am', TIME '10:10', 'review: approve API integration PR feedback'),
    (3, 'pm', TIME '13:35', 'feat(auth): finish login flow wrap-up'),
    (4, 'am', TIME '10:20', 'style(dashboard): responsive tweaks'),
    (4, 'pm', TIME '12:40', 'chore: update sprint notes')
) AS tmpl(entry_day, slot, commit_t, message);

-- ============================================================
-- 6. JIRA - 24 tickets assigned to Enzokuhle (10 To Do, 8 In Progress,
-- 4 Done, 2 Blocked), each backed by a lightweight shadow task carrying the
-- matching jira_ticket_key, matching the join getJiraTicketsBreakdown()
-- expects (task.assignedWorkspaceMemberId -> task.jiraTicketKey ->
-- jira_tickets.jiraTicketKey -> jira_tickets.jiraStatus).
-- ============================================================

INSERT INTO tasks (id, project_id, jira_ticket_key, title, status, assigned_workspace_member_id, created_at, updated_at) VALUES
('00000000-0000-0000-0002-000000002900', '00000000-0000-0000-0001-000000000040', 'MOM-300', 'Design new nav bar', 'TODO', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002901', '00000000-0000-0000-0001-000000000040', 'MOM-301', 'Add dark mode toggle', 'TODO', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002902', '00000000-0000-0000-0001-000000000040', 'MOM-302', 'Responsive layout for settings page', 'TODO', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002903', '00000000-0000-0000-0001-000000000040', 'MOM-303', 'Onboarding tour copy pass', 'TODO', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002904', '00000000-0000-0000-0001-000000000040', 'MOM-304', 'Empty states for reports tab', 'TODO', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002905', '00000000-0000-0000-0001-000000000040', 'MOM-305', 'Notification bell dropdown', 'TODO', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002906', '00000000-0000-0000-0001-000000000040', 'MOM-306', 'Accessibility pass on forms', 'TODO', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002907', '00000000-0000-0000-0001-000000000040', 'MOM-307', 'Redesign the projects table', 'TODO', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002908', '00000000-0000-0000-0001-000000000040', 'MOM-308', 'Sticky table headers', 'TODO', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002909', '00000000-0000-0000-0001-000000000040', 'MOM-309', 'Skeleton loaders for dashboard cards', 'TODO', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),

('00000000-0000-0000-0002-000000002910', '00000000-0000-0000-0001-000000000040', 'MOM-310', 'Implement login flow', 'IN_PROGRESS', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002911', '00000000-0000-0000-0001-000000000040', 'MOM-311', 'Fix authentication bug', 'IN_PROGRESS', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002912', '00000000-0000-0000-0001-000000000040', 'MOM-312', 'Review API integration PR', 'IN_PROGRESS', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002913', '00000000-0000-0000-0001-000000000040', 'MOM-313', 'UI polish for dashboard', 'IN_PROGRESS', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002914', '00000000-0000-0000-0001-000000000040', 'MOM-314', 'Refactor time tracking service', 'IN_PROGRESS', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002915', '00000000-0000-0000-0001-000000000040', 'MOM-315', 'Chart config extraction', 'IN_PROGRESS', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002916', '00000000-0000-0000-0001-000000000040', 'MOM-316', 'Password reset email template', 'IN_PROGRESS', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002917', '00000000-0000-0000-0001-000000000040', 'MOM-317', 'Session expiry handling', 'IN_PROGRESS', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),

('00000000-0000-0000-0002-000000002918', '00000000-0000-0000-0001-000000000040', 'MOM-318', 'Login screen UI', 'DONE', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002919', '00000000-0000-0000-0001-000000000040', 'MOM-319', 'Sprint retro follow-ups', 'DONE', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002920', '00000000-0000-0000-0001-000000000040', 'MOM-320', 'Dashboard widget skeleton states', 'DONE', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002921', '00000000-0000-0000-0001-000000000040', 'MOM-321', 'Google OAuth redirect', 'DONE', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),

('00000000-0000-0000-0002-000000002922', '00000000-0000-0000-0001-000000000040', 'MOM-322', 'AWS infra provisioning for staging', 'BLOCKED', '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0002-000000002923', '00000000-0000-0000-0001-000000000040', 'MOM-323', 'Third-party design token import', 'BLOCKED', '00000000-0000-0000-0002-000000000021', NOW(), NOW());

INSERT INTO jira_tickets (id, project_id, jira_ticket_key, summary, jira_status, issue_type, estimated_hours, logged_hours, last_synced, created_at) VALUES
('00000000-0000-0000-0002-000000003200', '00000000-0000-0000-0001-000000000040', 'MOM-300', 'Design new nav bar', 'To Do', 'STORY', 6.0, 0.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003201', '00000000-0000-0000-0001-000000000040', 'MOM-301', 'Add dark mode toggle', 'To Do', 'STORY', 8.0, 0.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003202', '00000000-0000-0000-0001-000000000040', 'MOM-302', 'Responsive layout for settings page', 'To Do', 'STORY', 5.0, 0.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003203', '00000000-0000-0000-0001-000000000040', 'MOM-303', 'Onboarding tour copy pass', 'To Do', 'TASK', 3.0, 0.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003204', '00000000-0000-0000-0001-000000000040', 'MOM-304', 'Empty states for reports tab', 'To Do', 'TASK', 4.0, 0.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003205', '00000000-0000-0000-0001-000000000040', 'MOM-305', 'Notification bell dropdown', 'To Do', 'STORY', 5.0, 0.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003206', '00000000-0000-0000-0001-000000000040', 'MOM-306', 'Accessibility pass on forms', 'To Do', 'TASK', 6.0, 0.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003207', '00000000-0000-0000-0001-000000000040', 'MOM-307', 'Redesign the projects table', 'To Do', 'STORY', 7.0, 0.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003208', '00000000-0000-0000-0001-000000000040', 'MOM-308', 'Sticky table headers', 'To Do', 'TASK', 2.0, 0.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003209', '00000000-0000-0000-0001-000000000040', 'MOM-309', 'Skeleton loaders for dashboard cards', 'To Do', 'TASK', 3.0, 0.0, NOW() - INTERVAL '6 hours', NOW()),

('00000000-0000-0000-0002-000000003210', '00000000-0000-0000-0001-000000000040', 'MOM-310', 'Implement login flow', 'In Progress', 'STORY', 12.0, 11.5, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003211', '00000000-0000-0000-0001-000000000040', 'MOM-311', 'Fix authentication bug', 'In Progress', 'BUG', 9.0, 8.5, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003212', '00000000-0000-0000-0001-000000000040', 'MOM-312', 'Review API integration PR', 'In Progress', 'TASK', 5.0, 5.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003213', '00000000-0000-0000-0001-000000000040', 'MOM-313', 'UI polish for dashboard', 'In Progress', 'STORY', 6.0, 6.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003214', '00000000-0000-0000-0001-000000000040', 'MOM-314', 'Refactor time tracking service', 'In Progress', 'TASK', 4.0, 4.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003215', '00000000-0000-0000-0001-000000000040', 'MOM-315', 'Chart config extraction', 'In Progress', 'TASK', 3.0, 2.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003216', '00000000-0000-0000-0001-000000000040', 'MOM-316', 'Password reset email template', 'In Progress', 'TASK', 2.0, 1.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003217', '00000000-0000-0000-0001-000000000040', 'MOM-317', 'Session expiry handling', 'In Progress', 'BUG', 3.0, 1.5, NOW() - INTERVAL '6 hours', NOW()),

('00000000-0000-0000-0002-000000003218', '00000000-0000-0000-0001-000000000040', 'MOM-318', 'Login screen UI', 'Done', 'STORY', 8.0, 8.5, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003219', '00000000-0000-0000-0001-000000000040', 'MOM-319', 'Sprint retro follow-ups', 'Done', 'TASK', 2.0, 2.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003220', '00000000-0000-0000-0001-000000000040', 'MOM-320', 'Dashboard widget skeleton states', 'Done', 'STORY', 5.0, 5.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003221', '00000000-0000-0000-0001-000000000040', 'MOM-321', 'Google OAuth redirect', 'Done', 'STORY', 6.0, 6.5, NOW() - INTERVAL '6 hours', NOW()),

('00000000-0000-0000-0002-000000003222', '00000000-0000-0000-0001-000000000040', 'MOM-322', 'AWS infra provisioning for staging', 'Blocked', 'TASK', 16.0, 2.0, NOW() - INTERVAL '6 hours', NOW()),
('00000000-0000-0000-0002-000000003223', '00000000-0000-0000-0001-000000000040', 'MOM-323', 'Third-party design token import', 'Blocked', 'TASK', 4.0, 0.5, NOW() - INTERVAL '6 hours', NOW());

-- ============================================================
-- 7. AI INSIGHTS
--
-- 7a. PRODUCTIVITY - explicit last 6 weeks (92 this week, +12 vs last
-- week, matching the wireframe's "92 / 100" / "12% vs last week" text),
-- then a full year of history behind that (weeks 6-51, skipping 9 and 10
-- where V2/V3 don't seed a score anyway - no collision risk there since
-- ai_insights has no uniqueness constraint, but keeping the pattern
-- consistent with section 3's exclusions).
-- ============================================================

INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, score, created_at) VALUES
('00000000-0000-0000-0002-000000003300', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 68.00, NOW() - INTERVAL '5 weeks'),
('00000000-0000-0000-0002-000000003301', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 72.00, NOW() - INTERVAL '4 weeks'),
('00000000-0000-0000-0002-000000003302', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 75.00, NOW() - INTERVAL '3 weeks'),
('00000000-0000-0000-0002-000000003303', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 78.00, NOW() - INTERVAL '2 weeks'),
('00000000-0000-0000-0002-000000003304', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 80.00, NOW() - INTERVAL '1 week'),
('00000000-0000-0000-0002-000000003305', '00000000-0000-0000-0002-000000000021', NULL, 'PRODUCTIVITY', 'USER', 92.00, NOW()),
-- by-project (Mobile App), matching the overall trend for this week and last
('00000000-0000-0000-0002-000000003306', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'PRODUCTIVITY', 'USER', 92.00, NOW()),
('00000000-0000-0000-0002-000000003307', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'PRODUCTIVITY', 'USER', 80.00, NOW() - INTERVAL '1 week');

INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, score, created_at)
SELECT
    gen_random_uuid(),
    '00000000-0000-0000-0002-000000000021',
    NULL,
    'PRODUCTIVITY',
    'USER',
    ROUND((60 + random() * 30)::numeric, 2),
    NOW() - (wk.n || ' weeks')::interval
FROM generate_series(6, 51) AS wk(n)
WHERE wk.n NOT IN (9, 10);

-- ============================================================
-- 7b. TASK_SWITCHING - this week only.
-- ============================================================

INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, score, description, created_at) VALUES
('00000000-0000-0000-0002-000000003310', '00000000-0000-0000-0002-000000000021', NULL, 'TASK_SWITCHING', 'USER', 1.20,
 'Averaged 1.2 task switches per day this week across login, review, bug-fix and UI-polish work on Mobile App.',
 NOW());

-- ============================================================
-- 7c. BURNOUT - this is the fix for the original V22 bug: workspace_id is
-- now set, satisfying chk_ai_insights_scope_owner for scope='TEAM'.
-- ============================================================

INSERT INTO ai_insights (id, workspace_id, workspace_member_id, project_id, insight_type, scope, score, description, recommendation, created_at) VALUES
('00000000-0000-0000-0002-000000003320', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', 'BURNOUT', 'TEAM', 55.00,
 'At least one 9.5h+ day logged this period (Tuesday ran long on UI polish).',
 'Keep an eye on next week''s hours before the task estimate is revised.',
 NOW());

-- ============================================================
-- 7d. WEEKLY_SUMMARY - narrative for the current week.
-- ============================================================

INSERT INTO ai_insights (id, workspace_member_id, project_id, insight_type, scope, narrative, created_at) VALUES
('00000000-0000-0000-0002-000000003330', '00000000-0000-0000-0002-000000000021', NULL, 'WEEKLY_SUMMARY', 'USER',
 'You logged 37.5h this week on Mobile App, including one extended Tuesday session after stakeholder feedback. Your timesheet is submitted and awaiting review.',
 NOW());

-- ============================================================
-- 7e. ANOMALY - tied to the real Tuesday long-session entry from section 2.
-- ============================================================

INSERT INTO ai_insights (id, workspace_member_id, project_id, time_entry_id, insight_type, scope, confidence, description, created_at) VALUES
('00000000-0000-0000-0002-000000003340', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000002713', 'ANOMALY', 'USER', 88.00,
 'Entry duration of 9.5h on Tuesday is unusually long compared to this person''s typical entries.',
 NOW());