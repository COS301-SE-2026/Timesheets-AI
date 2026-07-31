-- V3: Multi-project assignments, a rejected timesheet, more projects (varied
-- statuses) and more tasks for realistic test data.
-- Additive only, per team convention (no schema changes, no edits to V1/V2).

-- ============================================================
-- 1. MULTI-PROJECT ASSIGNMENTS
-- Give a handful of members (dev, manager, admin) a second project
-- so "assigned to multiple projects" scenarios actually exist to test.
-- ============================================================
INSERT INTO project_members (id, project_id, workspace_member_id, is_project_manager, is_active, created_at, updated_at) VALUES
-- Thabang Siduke (DEVELOPER, already on Mobile App + Backend API from V2) -> also Design System
('00000000-0000-0000-0001-000000000151', '00000000-0000-0000-0003-000000000042', '00000000-0000-0000-0001-000000000020', false, true, NOW(), NOW()),

-- Karabo Mathebula (DEVELOPER, already on Design System) -> also Mobile App
('00000000-0000-0000-0005-000000000152', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0005-000000000024', false, true, NOW(), NOW()),

-- Amahle Dlamini (MANAGER, PM on Mobile App) -> also Backend API (not PM there)
('00000000-0000-0000-0004-000000000153', '00000000-0000-0000-0002-000000000041', '00000000-0000-0000-0004-000000000023', false, true, NOW(), NOW()),

-- Joshua Botes (MANAGER, PM on Backend API) -> also DevOps Pipeline (not PM there)
('00000000-0000-0000-0008-000000000154', '00000000-0000-0000-0004-000000000043', '00000000-0000-0000-0008-000000000027', false, true, NOW(), NOW()),

-- Faith Solomons (ADMIN, PM on DevOps Pipeline) -> also Mobile App and Backend API
('00000000-0000-0000-0009-000000000155', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0009-000000000028', false, true, NOW(), NOW()),
('00000000-0000-0000-0009-000000000156', '00000000-0000-0000-0002-000000000041', '00000000-0000-0000-0009-000000000028', false, true, NOW(), NOW()),

-- Lubanzi Gcabashe (DEVELOPER, already on DevOps Pipeline) -> also Design System
('00000000-0000-0000-0006-000000000157', '00000000-0000-0000-0003-000000000042', '00000000-0000-0000-0006-000000000025', false, true, NOW(), NOW());


-- ============================================================
-- 2. A REJECTED TIMESHEET (with reason)
-- Everything in V2 is DRAFT already, so nothing to change there.
-- This adds one REJECTED example for Lubanzi Gcabashe so approval-flow
-- and rejection-reason UI/logic have something real to render.
-- ============================================================
INSERT INTO timesheets (
    id,
    workspace_member_id,
    period_start,
    period_end,
    status,
    submitted_at,
    rejected_at,
    rejection_reason,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0006-000000000160',
    '00000000-0000-0000-0006-000000000025',
    '2026-07-06',
    '2026-07-12',
    'REJECTED',
    '2026-07-13 09:15:00',
    '2026-07-14 11:40:00',
    'Logged hours exceed task estimate without an explanation - please break down the overtime by task and resubmit.',
    NOW(),
    NOW()
);

-- A couple of time entries against that rejected timesheet, so it isn't empty
INSERT INTO time_entries (id, timesheet_id, workspace_member_id, project_id, task_id, start_time, end_time, duration_seconds, entry_type, description, created_at, updated_at) VALUES
('00000000-0000-0000-0006-000000000161', '00000000-0000-0000-0006-000000000160', '00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0004-000000000043', '00000000-0000-0000-0009-000000000078', '2026-07-07 08:00:00', '2026-07-07 18:00:00', 36000, 'MANUAL', 'GitHub Actions pipeline debugging - longer than expected', NOW(), NOW()),
('00000000-0000-0000-0006-000000000162', '00000000-0000-0000-0006-000000000160', '00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0004-000000000043', '00000000-0000-0000-0009-000000000078', '2026-07-08 08:00:00', '2026-07-08 19:00:00', 39600, 'MANUAL', 'Continued CI/CD pipeline work', NOW(), NOW());


-- Additive only. Builds on V1 (schema), V2 (base seed), V3 (multi-project + rejected timesheet).

-- ============================================================
-- 3. MORE PROJECTS (covering all status values for UI/filter testing)
-- ============================================================
INSERT INTO projects (id, workspace_id, name, description, status, budget_hours, hourly_rate, budget_cost, start_date, end_date, created_by_workspace_member_id, created_at, updated_at) VALUES
-- ACTIVE
('00000000-0000-0000-0001-000000000200', '00000000-0000-0000-0001-000000000010', 'Client Portal Redesign', 'Revamp of the external client-facing portal', 'ACTIVE', 400.00, 80.00, 32000.00, '2026-06-01', NULL, '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
-- ON_HOLD
('00000000-0000-0000-0002-000000000201', '00000000-0000-0000-0001-000000000010', 'Legacy Reporting Migration', 'Migrate legacy Crystal Reports off to the new reporting stack', 'ON_HOLD', 250.00, 70.00, 17500.00, '2026-05-01', NULL, '00000000-0000-0000-0004-000000000023', NOW(), NOW()),
-- ARCHIVED
('00000000-0000-0000-0003-000000000202', '00000000-0000-0000-0001-000000000010', 'Q1 Marketing Site', 'Landing pages and campaign site for Q1 push', 'ARCHIVED', 120.00, 60.00, 7200.00, '2026-01-05', '2026-03-28', '00000000-0000-0000-0003-000000000022', NOW(), NOW()),
-- COMPLETED
('00000000-0000-0000-0004-000000000203', '00000000-0000-0000-0001-000000000010', 'Internal Tooling', 'Internal admin dashboard for support staff', 'COMPLETED', 180.00, 75.00, 13500.00, '2026-02-01', '2026-05-15', '00000000-0000-0000-0009-000000000028', NOW(), NOW());


-- ============================================================
-- 4. PROJECT MEMBERS
-- Enzokuhle demos with multiple projects across different statuses:
-- Mobile App (ACTIVE, existing from V2) + Client Portal Redesign (ACTIVE, PM here)
-- + Legacy Reporting Migration (ON_HOLD).
-- Amahle (workspace role = MANAGER) is added here as a plain contributor
-- (is_project_manager = false) on Legacy Reporting Migration, i.e. acting
-- as a "developer" on that project despite her workspace-level role.
-- ============================================================
INSERT INTO project_members (id, project_id, workspace_member_id, is_project_manager, is_active, created_at, updated_at) VALUES
-- Client Portal Redesign
('00000000-0000-0000-0001-000000000210', '00000000-0000-0000-0001-000000000200', '00000000-0000-0000-0002-000000000021', true, true, NOW(), NOW()),  -- Enzokuhle is PM
('00000000-0000-0000-0002-000000000211', '00000000-0000-0000-0001-000000000200', '00000000-0000-0000-0003-000000000022', false, true, NOW(), NOW()), -- Lethabo
('00000000-0000-0000-0003-000000000212', '00000000-0000-0000-0001-000000000200', '00000000-0000-0000-0007-000000000026', false, true, NOW(), NOW()), -- Naledi

-- Legacy Reporting Migration
('00000000-0000-0000-0004-000000000213', '00000000-0000-0000-0002-000000000201', '00000000-0000-0000-0002-000000000021', false, true, NOW(), NOW()), -- Enzokuhle (2nd project)
('00000000-0000-0000-0005-000000000214', '00000000-0000-0000-0002-000000000201', '00000000-0000-0000-0004-000000000023', false, true, NOW(), NOW()), -- Amahle (MANAGER role, acting as contributor here, not PM)
('00000000-0000-0000-0006-000000000215', '00000000-0000-0000-0002-000000000201', '00000000-0000-0000-0008-000000000027', true, true, NOW(), NOW()),  -- Joshua is PM here

-- Q1 Marketing Site (archived)
('00000000-0000-0000-0007-000000000216', '00000000-0000-0000-0003-000000000202', '00000000-0000-0000-0003-000000000022', true, false, NOW(), NOW()), -- Lethabo was PM, now inactive (archived project)
('00000000-0000-0000-0008-000000000217', '00000000-0000-0000-0003-000000000202', '00000000-0000-0000-0011-000000000030', false, false, NOW(), NOW()),-- Thato, inactive

-- Internal Tooling (completed)
('00000000-0000-0000-0009-000000000218', '00000000-0000-0000-0004-000000000203', '00000000-0000-0000-0009-000000000028', true, true, NOW(), NOW()),  -- Faith is PM
('00000000-0000-0000-0010-000000000219', '00000000-0000-0000-0004-000000000203', '00000000-0000-0000-0010-000000000029', false, true, NOW(), NOW()); -- Isabella


-- ============================================================
-- 5. TASKS for the new projects
-- ============================================================
INSERT INTO tasks (id, project_id, title, description, status, priority, estimated_hours, actual_hours, assigned_workspace_member_id, due_date, completed_at, created_at, updated_at) VALUES
-- Client Portal Redesign
('00000000-0000-0000-0001-000000000220', '00000000-0000-0000-0001-000000000200', 'Design New Portal Layout', 'Wireframes and high-fidelity mockups for the redesigned portal', 'IN_PROGRESS', 'HIGH', 12.0, 6.5, '00000000-0000-0000-0002-000000000021', '2026-08-10', NULL, NOW(), NOW()),
('00000000-0000-0000-0002-000000000221', '00000000-0000-0000-0001-000000000200', 'Migrate Client Auth Flow', 'Port existing client login/session handling to new portal', 'TODO', 'CRITICAL', 10.0, NULL, '00000000-0000-0000-0003-000000000022', '2026-08-20', NULL, NOW(), NOW()),
('00000000-0000-0000-0003-000000000222', '00000000-0000-0000-0001-000000000200', 'Build Document Upload Widget', 'Client-facing document upload with progress and validation', 'TODO', 'MEDIUM', 8.0, NULL, '00000000-0000-0000-0007-000000000026', '2026-08-25', NULL, NOW(), NOW()),

-- Legacy Reporting Migration
('00000000-0000-0000-0004-000000000223', '00000000-0000-0000-0002-000000000201', 'Audit Existing Crystal Reports', 'Catalogue all legacy reports and their data sources', 'IN_PROGRESS', 'MEDIUM', 6.0, 3.0, '00000000-0000-0000-0004-000000000023', '2026-08-05', NULL, NOW(), NOW()),
('00000000-0000-0000-0005-000000000224', '00000000-0000-0000-0002-000000000201', 'Design Replacement Report Schema', 'Data model for the new reporting stack', 'TODO', 'HIGH', 10.0, NULL, '00000000-0000-0000-0008-000000000027', '2026-08-30', NULL, NOW(), NOW()),
('00000000-0000-0000-0006-000000000225', '00000000-0000-0000-0002-000000000201', 'Stakeholder Sign-off on Scope', 'Confirm which reports are still in active use before migrating', 'BLOCKED', 'MEDIUM', 3.0, NULL, '00000000-0000-0000-0002-000000000021', NULL, NULL, NOW(), NOW()),

-- Q1 Marketing Site (archived project, tasks all DONE)
('00000000-0000-0000-0007-000000000226', '00000000-0000-0000-0003-000000000202', 'Build Landing Page Templates', 'Reusable templates for the Q1 campaign pages', 'DONE', 'MEDIUM', 8.0, 7.5, '00000000-0000-0000-0003-000000000022', '2026-02-20', '2026-02-19 16:00:00', NOW(), NOW()),
('00000000-0000-0000-0008-000000000227', '00000000-0000-0000-0003-000000000202', 'Set Up Campaign Analytics', 'Wire up tracking and conversion goals', 'DONE', 'LOW', 4.0, 4.0, '00000000-0000-0000-0011-000000000030', '2026-03-01', '2026-02-28 12:00:00', NOW(), NOW()),

-- Internal Tooling (completed project, tasks all DONE)
('00000000-0000-0000-0009-000000000228', '00000000-0000-0000-0004-000000000203', 'Build Support Ticket Viewer', 'Read-only view of support tickets for internal staff', 'DONE', 'HIGH', 10.0, 9.0, '00000000-0000-0000-0009-000000000028', '2026-04-15', '2026-04-14 10:00:00', NOW(), NOW()),
('00000000-0000-0000-0010-000000000229', '00000000-0000-0000-0004-000000000203', 'Add Role-Based Access Controls', 'Restrict tooling actions by workspace role', 'DONE', 'HIGH', 8.0, 8.5, '00000000-0000-0000-0010-000000000029', '2026-05-01', '2026-04-30 15:30:00', NOW(), NOW());