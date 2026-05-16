-- Seed data for Demo 1 development and testing
-- Password for all users is: Password123!

INSERT INTO users (id, first_name, last_name, email, password_hash, job_title,
                   seniority_level, employment_type, status)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'Alice', 'Admin', 'alice@momentum.co.za',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'System Administrator', 'SENIOR', 'FULL_TIME', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000002', 'Bob', 'Developer', 'bob@momentum.co.za',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Software Engineer', 'INTERMEDIATE', 'FULL_TIME', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000003', 'Carol', 'Manager', 'carol@momentum.co.za',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Engineering Manager', 'LEAD', 'FULL_TIME', 'ACTIVE');

INSERT INTO workspaces (id, name, owner_user_id)
VALUES ('00000000-0000-0000-0001-000000000001', 'Momentum Life IT',
        '00000000-0000-0000-0000-000000000001');

INSERT INTO workspace_roles (id, name, description)
VALUES
    ('00000000-0000-0000-0002-000000000001', 'ADMIN', 'Full workspace access'),
    ('00000000-0000-0000-0002-000000000002', 'MANAGER', 'Manages projects and teams'),
    ('00000000-0000-0000-0002-000000000003', 'DEVELOPER', 'Logs time and submits timesheets');

INSERT INTO workspace_members (id, workspace_id, user_id, workspace_role_id, joined_at)
VALUES
    ('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0001-000000000001',
     '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0002-000000000001', now()),
    ('00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0001-000000000001',
     '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0002-000000000003', now()),
    ('00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0001-000000000001',
     '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0002-000000000002', now());

INSERT INTO projects (id, workspace_id, name, description, status, created_by_workspace_member_id)
VALUES
    ('00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0001-000000000001',
     'Core Platform', 'Momently MVP', 'ACTIVE', '00000000-0000-0000-0003-000000000001'),
    ('00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0001-000000000001',
     'Mobile App', 'Companion mobile application', 'ACTIVE', '00000000-0000-0000-0003-000000000001');

INSERT INTO project_members (workspace_member_id, project_id, is_active, joined_at)
VALUES
    ('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0004-000000000001', true, now()),
    ('00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0004-000000000001', true, now()),
    ('00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0004-000000000001', true, now()),
    ('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0004-000000000002', true, now());

INSERT INTO project_managers (project_id, workspace_member_id, assigned_at)
VALUES ('00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0003-000000000003', now());

INSERT INTO task_statuses (id, code, description)
VALUES
    ('00000000-0000-0000-0005-000000000001', 'TODO', 'Not yet started'),
    ('00000000-0000-0000-0005-000000000002', 'IN_PROGRESS', 'Currently being worked on'),
    ('00000000-0000-0000-0005-000000000003', 'DONE', 'Completed'),
    ('00000000-0000-0000-0005-000000000004', 'BLOCKED', 'Blocked by a dependency');

INSERT INTO tasks (id, project_id, title, status_id, priority,
                   created_by_workspace_member_id, assigned_workspace_member_id)
VALUES
    ('00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0004-000000000001',
     'Setup authentication', '00000000-0000-0000-0005-000000000003',
     'HIGH', '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0003-000000000002'),
    ('00000000-0000-0000-0006-000000000002', '00000000-0000-0000-0004-000000000001',
     'Build time entry API', '00000000-0000-0000-0005-000000000002',
     'HIGH', '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0003-000000000002'),
    ('00000000-0000-0000-0006-000000000003', '00000000-0000-0000-0004-000000000001',
     'Frontend integration', '00000000-0000-0000-0005-000000000001',
     'MEDIUM', '00000000-0000-0000-0003-000000000003', null);

INSERT INTO time_entries (id, workspace_member_id, project_id, task_id,
                          start_time, end_time, duration_minutes, entry_type, status)
VALUES
    ('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0003-000000000002',
     '00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0006-000000000001',
     now() - interval '2 days' + interval '9 hours',
     now() - interval '2 days' + interval '17 hours',
     480, 'MANUAL', 'SUBMITTED'),
    ('00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0003-000000000002',
     '00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0006-000000000002',
     now() - interval '1 day' + interval '9 hours',
     now() - interval '1 day' + interval '14 hours',
     300, 'MANUAL', 'DRAFT');

INSERT INTO user_availability (workspace_member_id, status, notes)
VALUES ('00000000-0000-0000-0003-000000000002', 'AVAILABLE', 'Ready for new tasks');
