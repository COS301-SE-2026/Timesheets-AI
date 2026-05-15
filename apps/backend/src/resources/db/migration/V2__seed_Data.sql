-- Seed data for Demo 1 development and testing
-- Password for all users is: Password123!
-- BCrypt hash generated with strength 10

INSERT INTO users (id, first_name, last_name, email, password_hash, job_title,
                   seniority_level, employment_type, status)
VALUES
    ('11111111-1111-1111-1111-111111111111',
     'Alice', 'Admin',
     'alice@momentum.co.za',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'System Administrator', 'SENIOR', 'FULL_TIME', 'ACTIVE'),

    ('22222222-2222-2222-2222-222222222222',
     'Bob', 'Developer',
     'bob@momentum.co.za',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Software Engineer', 'MID', 'FULL_TIME', 'ACTIVE'),

    ('33333333-3333-3333-3333-333333333333',
     'Carol', 'Manager',
     'carol@momentum.co.za',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Engineering Manager', 'LEAD', 'FULL_TIME', 'ACTIVE');

INSERT INTO workspaces (id, name, owner_user_id)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'Momentum Life IT', '11111111-1111-1111-1111-111111111111');

INSERT INTO workspace_roles (id, name, description)
VALUES
    ('rrrrrrrr-rrrr-rrrr-rrrr-rrrrrrrrrr01', 'ADMIN',
     'Full workspace access — manages users, projects and settings'),
    ('rrrrrrrr-rrrr-rrrr-rrrr-rrrrrrrrrr02', 'MANAGER',
     'Manages projects and teams, approves timesheets'),
    ('rrrrrrrr-rrrr-rrrr-rrrr-rrrrrrrrrr03', 'DEVELOPER',
     'Logs time, creates tasks, submits timesheets');

INSERT INTO workspace_members (id, workspace_id, user_id, workspace_role_id, joined_at)
VALUES
    ('mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm01',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     '11111111-1111-1111-1111-111111111111',
     'rrrrrrrr-rrrr-rrrr-rrrr-rrrrrrrrrr01',
     now()),

    ('mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm02',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     '22222222-2222-2222-2222-222222222222',
     'rrrrrrrr-rrrr-rrrr-rrrr-rrrrrrrrrr03',
     now()),

    ('mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm03',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     '33333333-3333-3333-3333-333333333333',
     'rrrrrrrr-rrrr-rrrr-rrrr-rrrrrrrrrr02',
     now());

INSERT INTO projects (id, workspace_id, name, description, status,
                      created_by_workspace_member_id)
VALUES
    ('pppppppp-pppp-pppp-pppp-pppppppppp01',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'Core Platform', 'Momently MVP — core time-tracking features',
     'ACTIVE', 'mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm01'),

    ('pppppppp-pppp-pppp-pppp-pppppppppp02',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'Mobile App', 'Companion mobile application',
     'ACTIVE', 'mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm01');

INSERT INTO project_members (workspace_member_id, project_id, is_active, joined_at)
VALUES
    ('mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm01',
     'pppppppp-pppp-pppp-pppp-pppppppppp01', true, now()),
    ('mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm02',
     'pppppppp-pppp-pppp-pppp-pppppppppp01', true, now()),
    ('mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm03',
     'pppppppp-pppp-pppp-pppp-pppppppppp01', true, now()),
    ('mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm01',
     'pppppppp-pppp-pppp-pppp-pppppppppp02', true, now());

INSERT INTO project_managers (project_id, workspace_member_id, assigned_at)
VALUES ('pppppppp-pppp-pppp-pppp-pppppppppp01',
        'mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm03', now());

INSERT INTO task_statuses (id, code, description)
VALUES
    ('ssssssss-ssss-ssss-ssss-ssssssssss01', 'TODO',        'Not yet started'),
    ('ssssssss-ssss-ssss-ssss-ssssssssss02', 'IN_PROGRESS',  'Currently being worked on'),
    ('ssssssss-ssss-ssss-ssss-ssssssssss03', 'DONE',         'Completed'),
    ('ssssssss-ssss-ssss-ssss-ssssssssss04', 'BLOCKED',      'Blocked by a dependency');

INSERT INTO tasks (id, project_id, title, status_id, priority,
                   created_by_workspace_member_id, assigned_workspace_member_id)
VALUES
    ('tttttttt-tttt-tttt-tttt-tttttttttt01',
     'pppppppp-pppp-pppp-pppp-pppppppppp01',
     'Setup authentication', 'ssssssss-ssss-ssss-ssss-ssssssssss03',
     'HIGH', 'mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm01',
     'mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm02'),

    ('tttttttt-tttt-tttt-tttt-tttttttttt02',
     'pppppppp-pppp-pppp-pppp-pppppppppp01',
     'Build time entry API', 'ssssssss-ssss-ssss-ssss-ssssssssss02',
     'HIGH', 'mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm01',
     'mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm02'),

    ('tttttttt-tttt-tttt-tttt-tttttttttt03',
     'pppppppp-pppp-pppp-pppp-pppppppppp01',
     'Frontend integration', 'ssssssss-ssss-ssss-ssss-ssssssssss01',
     'MEDIUM', 'mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm03', null);

-- Two sample time entries for Bob on Core Platform (for insight testing)
INSERT INTO time_entries (id, workspace_member_id, project_id, task_id,
                          start_time, end_time, duration_minutes, entry_type, status)
VALUES
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01',
     'mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm02',
     'pppppppp-pppp-pppp-pppp-pppppppppp01',
     'tttttttt-tttt-tttt-tttt-tttttttttt01',
     now() - interval '2 days' + interval '9 hours',
     now() - interval '2 days' + interval '17 hours',
     480, 'MANUAL', 'SUBMITTED'),

    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02',
     'mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm02',
     'pppppppp-pppp-pppp-pppp-pppppppppp01',
     'tttttttt-tttt-tttt-tttt-tttttttttt02',
     now() - interval '1 day' + interval '9 hours',
     now() - interval '1 day' + interval '14 hours',
     300, 'MANUAL', 'DRAFT');

-- Bob's current availability
INSERT INTO user_availability (workspace_member_id, status, notes)
VALUES ('mmmmmmmm-mmmm-mmmm-mmmm-mmmmmmmmmm02', 'AVAILABLE', 'Ready for work');