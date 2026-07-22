

-- Enable UUID generation if not already enabled
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- User 1: Thabang Siduke- he is MFA enabled so use User 2 if you need JWT token
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0001-000000000001',
    'Thabang',
    'Siduke',
    'thabang.siduke@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',  -- password: "momentlyPass300$"
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- User 2: Enzokuhle Khumalo
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0002-000000000002',
    'Enzokuhle',
    'Khumalo',
    'enzokuhle.khumalo@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- User 3: Lethabo Maseko 
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0003-000000000003',
    'Lethabo',
    'Maseko',
    'lethabo.maseko@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- User 4: Amahle Dlamini
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0004-000000000004',
    'Amahle',
    'Dlamini',
    'amahle.dlamini@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- User 5: Karabo Mathebula
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0005-000000000005',
    'Karabo',
    'Mathebula',
    'karabo.mathebula@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- User 6: Lubanzi Gcabashe
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0006-000000000006',
    'Lubanzi',
    'Gcabashe',
    'lubanzi.gcabashe@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- User 7: Naledi Mphahlele
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0007-000000000007',
    'Naledi',
    'Mphahlele',
    'naledi.mphahlele@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- User 8: Joshua Botes
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0008-000000000008',
    'Joshua',
    'Botes',
    'joshua.botes@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- User 9: Faith Solomons
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0009-000000000009',
    'Faith',
    'Solomons',
    'faith.solomons@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- User 10: Isabella Cassiem
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0010-000000000010',
    'Isabella',
    'Cassiem',
    'isabella.cassiem@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- User 11: Thato Moosa
INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    password_hash,
    email_verified,
    status,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0011-000000000011',
    'Thato',
    'Moosa',
    'thato.moosa@momentum.co.za',
    '$2a$12$l.I8BliSILNVE7tbTm937eT7OMBdU9mv5.pXpmzBm3JcJNA/5mA1i',
    true,
    'ACTIVE',
    NOW(),
    NOW()
);

-- 2. WORKSPACE MEMBERS (All users in Workspace 1)
INSERT INTO workspaces (
    id,
    name,
    owner_user_id,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-0000-0001-000000000010',
    'Momentum Engineering',
    '00000000-0000-0000-0001-000000000001',  -- Thabang is owner
    NOW(),
    NOW()
);

INSERT INTO workspace_members (id, workspace_id, user_id, role, joined_at, created_at, updated_at) VALUES
-- Thabang Siduke (DEVELOPER)
('00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0001-000000000001', 'DEVELOPER', NOW(), NOW(), NOW()),
-- Enzokuhle Khumalo (DEVELOPER)
('00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0002-000000000002', 'DEVELOPER', NOW(), NOW(), NOW()),
-- Lethabo Maseko (DEVELOPER)
('00000000-0000-0000-0003-000000000022', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0003-000000000003', 'DEVELOPER', NOW(), NOW(), NOW()),
-- Amahle Dlamini (MANAGER)
('00000000-0000-0000-0004-000000000023', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0004-000000000004', 'MANAGER', NOW(), NOW(), NOW()),
-- Karabo Mathebula (DEVELOPER)
('00000000-0000-0000-0005-000000000024', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0005-000000000005', 'DEVELOPER', NOW(), NOW(), NOW()),
-- Lubanzi Gcabashe (DEVELOPER)
('00000000-0000-0000-0006-000000000025', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0006-000000000006', 'DEVELOPER', NOW(), NOW(), NOW()),
-- Naledi Mphahlele (DEVELOPER)
('00000000-0000-0000-0007-000000000026', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0007-000000000007', 'DEVELOPER', NOW(), NOW(), NOW()),
-- Joshua Botes (MANAGER)
('00000000-0000-0000-0008-000000000027', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0008-000000000008', 'MANAGER', NOW(), NOW(), NOW()),
-- Faith Solomons (ADMIN)
('00000000-0000-0000-0009-000000000028', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0009-000000000009', 'ADMIN', NOW(), NOW(), NOW()),
-- Isabella Cassiem (DEVELOPER)
('00000000-0000-0000-0010-000000000029', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0010-000000000010', 'DEVELOPER', NOW(), NOW(), NOW()),
-- Thato Moosa (DEVELOPER)
('00000000-0000-0000-0011-000000000030', '00000000-0000-0000-0001-000000000010', '00000000-0000-0000-0011-000000000011', 'DEVELOPER', NOW(), NOW(), NOW());



-- 3. PROJECTS
INSERT INTO projects (id, workspace_id, name, description, status, budget_hours, hourly_rate, created_by_workspace_member_id, created_at, updated_at) VALUES
('00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0001-000000000010', 'Mobile App Development', 'Building the React Native mobile application', 'ACTIVE', 500.00, 75.00, '00000000-0000-0000-0003-000000000022', NOW(), NOW()),
('00000000-0000-0000-0002-000000000041', '00000000-0000-0000-0001-000000000010', 'Backend API', 'Spring Boot REST API development', 'ACTIVE', 300.00, 90.00, '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0003-000000000042', '00000000-0000-0000-0001-000000000010', 'Design System', 'Building the company design system', 'ON_HOLD', 150.00, 65.00, '00000000-0000-0000-0001-000000000020', NOW(), NOW()),
('00000000-0000-0000-0004-000000000043', '00000000-0000-0000-0001-000000000010', 'DevOps Pipeline', 'CI/CD pipeline setup and maintenance', 'ACTIVE', 200.00, 85.00, '00000000-0000-0000-0006-000000000025', NOW(), NOW());



-- 4. PROJECT MEMBERS
INSERT INTO project_members (id, project_id, workspace_member_id, is_project_manager, is_active, created_at, updated_at) VALUES
-- Mobile App project members
('00000000-0000-0000-0001-000000000050', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0001-000000000020', false, true, NOW(), NOW()),
('00000000-0000-0000-0002-000000000051', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000000021', false, true, NOW(), NOW()),
('00000000-0000-0000-0003-000000000052', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0004-000000000023', true, true, NOW(), NOW()),  -- Amahle is PM

-- Backend API project members
('00000000-0000-0000-0004-000000000053', '00000000-0000-0000-0002-000000000041', '00000000-0000-0000-0001-000000000020', false, true, NOW(), NOW()),
('00000000-0000-0000-0005-000000000054', '00000000-0000-0000-0002-000000000041', '00000000-0000-0000-0003-000000000022', false, true, NOW(), NOW()),
('00000000-0000-0000-0006-000000000055', '00000000-0000-0000-0002-000000000041', '00000000-0000-0000-0006-000000000025', false, true, NOW(), NOW()),
('00000000-0000-0000-0007-000000000056', '00000000-0000-0000-0002-000000000041', '00000000-0000-0000-0008-000000000027', true, true, NOW(), NOW()),  -- Joshua is PM

-- Design System project members
('00000000-0000-0000-0008-000000000057', '00000000-0000-0000-0003-000000000042', '00000000-0000-0000-0005-000000000024', false, true, NOW(), NOW()),
('00000000-0000-0000-0009-000000000058', '00000000-0000-0000-0003-000000000042', '00000000-0000-0000-0011-000000000030', false, true, NOW(), NOW()),

-- DevOps project members
('00000000-0000-0000-0010-000000000059', '00000000-0000-0000-0004-000000000043', '00000000-0000-0000-0007-000000000026', false, true, NOW(), NOW()),
('00000000-0000-0000-0011-000000000060', '00000000-0000-0000-0004-000000000043', '00000000-0000-0000-0010-000000000029', false, true, NOW(), NOW()),
('00000000-0000-0000-0012-000000000061', '00000000-0000-0000-0004-000000000043', '00000000-0000-0000-0009-000000000028', true, true, NOW(), NOW());  -- Faith is PM



-- 5. TASKS
INSERT INTO tasks (id, project_id, title, description, status, priority, estimated_hours, assigned_workspace_member_id, created_at, updated_at) VALUES
-- Mobile App tasks
('00000000-0000-0000-0001-000000000070', '00000000-0000-0000-0001-000000000040', 'Implement Login Screen', 'Design and implement the login screen with Google SSO', 'IN_PROGRESS', 'HIGH', 8.0, '00000000-0000-0000-0001-000000000020', NOW(), NOW()),
('00000000-0000-0000-0002-000000000071', '00000000-0000-0000-0001-000000000040', 'Design Dashboard UI', 'Create wireframes and UI for the main dashboard', 'TODO', 'MEDIUM', 6.0, '00000000-0000-0000-0002-000000000021', NOW(), NOW()),
('00000000-0000-0000-0003-000000000072', '00000000-0000-0000-0001-000000000040', 'Implement Navigation', 'Implement the bottom tab navigation', 'DONE', 'MEDIUM', 4.0, '00000000-0000-0000-0001-000000000020', NOW(), NOW()),

-- Backend API tasks
('00000000-0000-0000-0004-000000000073', '00000000-0000-0000-0002-000000000041', 'Implement JWT Authentication', 'Add JWT token generation and validation', 'IN_PROGRESS', 'CRITICAL', 10.0, '00000000-0000-0000-0001-000000000020', NOW(), NOW()),
('00000000-0000-0000-0005-000000000074', '00000000-0000-0000-0002-000000000041', 'Create Timesheet API', 'Build timesheet CRUD and approval endpoints', 'TODO', 'HIGH', 8.0, '00000000-0000-0000-0003-000000000022', NOW(), NOW()),
('00000000-0000-0000-0006-000000000075', '00000000-0000-0000-0002-000000000041', 'Implement MFA', 'Add multi-factor authentication support', 'TODO', 'HIGH', 6.0, '00000000-0000-0000-0006-000000000025', NOW(), NOW()),

-- Design System tasks
('00000000-0000-0000-0007-000000000076', '00000000-0000-0000-0003-000000000042', 'Create Color System', 'Define and document the color palette', 'TODO', 'MEDIUM', 5.0, '00000000-0000-0000-0005-000000000024', NOW(), NOW()),
('00000000-0000-0000-0008-000000000077', '00000000-0000-0000-0003-000000000042', 'Typography Guidelines', 'Define typography system', 'TODO', 'LOW', 3.0, '00000000-0000-0000-0011-000000000030', NOW(), NOW()),

-- DevOps tasks
('00000000-0000-0000-0009-000000000078', '00000000-0000-0000-0004-000000000043', 'Setup GitHub Actions', 'Configure CI/CD pipeline', 'IN_PROGRESS', 'HIGH', 8.0, '00000000-0000-0000-0007-000000000026', NOW(), NOW()),
('00000000-0000-0000-0010-000000000079', '00000000-0000-0000-0004-000000000043', 'AWS Infrastructure', 'Setup AWS resources', 'TODO', 'HIGH', 10.0, '00000000-0000-0000-0010-000000000029', NOW(), NOW());



-- 6. TIMESHEETS (Only for active devs)
-- Thabang Siduke's timesheets
INSERT INTO timesheets (id, workspace_member_id, period_start, period_end, status, created_at, updated_at) VALUES
('00000000-0000-0000-0001-000000000080', '00000000-0000-0000-0001-000000000020', '2026-07-13', '2026-07-19', 'DRAFT', NOW(), NOW()),
('00000000-0000-0000-0002-000000000081', '00000000-0000-0000-0001-000000000020', '2026-07-20', '2026-07-26', 'DRAFT', NOW(), NOW());

-- Enzokuhle Khumalo's timesheets
INSERT INTO timesheets (id, workspace_member_id, period_start, period_end, status, created_at, updated_at) VALUES
('00000000-0000-0000-0003-000000000082', '00000000-0000-0000-0002-000000000021', '2026-07-13', '2026-07-19', 'DRAFT', NOW(), NOW()),
('00000000-0000-0000-0004-000000000083', '00000000-0000-0000-0002-000000000021', '2026-07-20', '2026-07-26', 'DRAFT', NOW(), NOW());

-- Karabo Mathebula's timesheets
INSERT INTO timesheets (id, workspace_member_id, period_start, period_end, status, created_at, updated_at) VALUES
('00000000-0000-0000-0005-000000000084', '00000000-0000-0000-0005-000000000024', '2026-07-13', '2026-07-19', 'DRAFT', NOW(), NOW()),
('00000000-0000-0000-0006-000000000085', '00000000-0000-0000-0005-000000000024', '2026-07-20', '2026-07-26', 'DRAFT', NOW(), NOW());



-- 7. TIME ENTRIES
INSERT INTO time_entries (id, timesheet_id, workspace_member_id, project_id, task_id, start_time, end_time, duration_seconds, entry_type, description, created_at, updated_at) VALUES
-- Thabang's time entries
('00000000-0000-0000-0001-000000000090', '00000000-0000-0000-0001-000000000080', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0001-000000000070', '2026-07-13 09:00:00', '2026-07-13 12:00:00', 10800, 'MANUAL', 'Working on login screen UI', NOW(), NOW()),
('00000000-0000-0000-0002-000000000091', '00000000-0000-0000-0001-000000000080', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0002-000000000041', '00000000-0000-0000-0004-000000000073', '2026-07-14 10:00:00', '2026-07-14 12:30:00', 9000, 'TIMER', 'JWT authentication implementation', NOW(), NOW()),
('00000000-0000-0000-0003-000000000092', '00000000-0000-0000-0001-000000000080', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0002-000000000041', '00000000-0000-0000-0004-000000000073', '2026-07-15 14:00:00', '2026-07-15 17:00:00', 10800, 'MANUAL', 'Testing JWT token validation', NOW(), NOW()),
('00000000-0000-0000-0004-000000000093', '00000000-0000-0000-0001-000000000080', '00000000-0000-0000-0001-000000000020', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0003-000000000072', '2026-07-16 09:00:00', '2026-07-16 10:00:00', 3600, 'MANUAL', 'Navigation implementation review', NOW(), NOW()),

-- Enzokuhle's time entries
('00000000-0000-0000-0005-000000000094', '00000000-0000-0000-0003-000000000082', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000000071', '2026-07-13 11:00:00', '2026-07-13 14:00:00', 10800, 'MANUAL', 'Dashboard UI design', NOW(), NOW()),
('00000000-0000-0000-0006-000000000095', '00000000-0000-0000-0003-000000000082', '00000000-0000-0000-0002-000000000021', '00000000-0000-0000-0001-000000000040', '00000000-0000-0000-0002-000000000071', '2026-07-15 09:00:00', '2026-07-15 12:00:00', 10800, 'TIMER', 'Dashboard UI wireframes', NOW(), NOW()),

-- Karabo's time entries
('00000000-0000-0000-0007-000000000096', '00000000-0000-0000-0005-000000000084', '00000000-0000-0000-0005-000000000024', '00000000-0000-0000-0003-000000000042', '00000000-0000-0000-0007-000000000076', '2026-07-14 09:00:00', '2026-07-14 11:30:00', 9000, 'MANUAL', 'Creating color system', NOW(), NOW()),
('00000000-0000-0000-0008-000000000097', '00000000-0000-0000-0005-000000000084', '00000000-0000-0000-0005-000000000024', '00000000-0000-0000-0003-000000000042', '00000000-0000-0000-0008-000000000077', '2026-07-16 14:00:00', '2026-07-16 15:30:00', 5400, 'MANUAL', 'Typography research', NOW(), NOW());



-- 8. USER PREFERENCES
INSERT INTO user_preferences (id, user_id, theme, email_notifications, jira_enabled, calendar_enabled, git_enabled, created_at, updated_at) VALUES
('00000000-0000-0000-0001-000000000100', '00000000-0000-0000-0001-000000000001', 'DARK', true, false, true, false, NOW(), NOW()),
('00000000-0000-0000-0002-000000000101', '00000000-0000-0000-0002-000000000002', 'LIGHT', true, true, false, true, NOW(), NOW()),
('00000000-0000-0000-0003-000000000102', '00000000-0000-0000-0004-000000000004', 'DARK', true, false, true, false, NOW(), NOW()),
('00000000-0000-0000-0004-000000000103', '00000000-0000-0000-0005-000000000005', 'LIGHT', false, true, false, true, NOW(), NOW());



-- 9. USER MFA (For Thabang - enabled)
INSERT INTO user_mfa (id, user_id, secret_key, is_enabled, created_at, updated_at) VALUES
('00000000-0000-0000-0001-000000000110', '00000000-0000-0000-0001-000000000001', 'JBSWY3DPEHPK3PXP', true, NOW(), NOW());


-- 10. EMAIL VERIFICATION TOKENS (All verified)
INSERT INTO email_verification_tokens (id, user_id, token, expires_at, verified_at, created_at) VALUES
('00000000-0000-0000-0001-000000000120', '00000000-0000-0000-0001-000000000001', 'thabang-verify-token-001', NOW() + INTERVAL '24 hours', NOW(), NOW()),
('00000000-0000-0000-0002-000000000121', '00000000-0000-0000-0002-000000000002', 'enzokuhle-verify-token-002', NOW() + INTERVAL '24 hours', NOW(), NOW()),
('00000000-0000-0000-0003-000000000122', '00000000-0000-0000-0003-000000000003', 'lethabo-verify-token-003', NOW() + INTERVAL '24 hours', NOW(), NOW());


-- 11. USER IDENTITY PROVIDERS (Google SSO)
INSERT INTO user_identity_providers (id, user_id, provider, provider_user_id, email, created_at) VALUES
('00000000-0000-0000-0001-000000000130', '00000000-0000-0000-0001-000000000001', 'GOOGLE', 'thabang-google-id-001', 'thabang.siduke@gmail.com', NOW()),
('00000000-0000-0000-0002-000000000131', '00000000-0000-0000-0003-000000000003', 'GOOGLE', 'lethabo-google-id-002', 'lethabo.maseko@gmail.com', NOW());