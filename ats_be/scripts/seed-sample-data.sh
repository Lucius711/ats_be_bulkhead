#!/usr/bin/env bash

set -euo pipefail

# Seed data cho candidate-service và job-service.
# Chạy sau khi PostgreSQL và các service đã khởi động ít nhất một lần:
#   ./scripts/seed-sample-data.sh

docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d postgres' <<'SQL'
\set ON_ERROR_STOP on

-- ============================================================
-- candidate-service database
-- ============================================================
\connect ats_candidate_dbs

BEGIN;

INSERT INTO skills (id, skill_name, category)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Java', 'Backend'),
    ('10000000-0000-0000-0000-000000000002', 'Spring Boot', 'Backend'),
    ('10000000-0000-0000-0000-000000000003', 'PostgreSQL', 'Database'),
    ('10000000-0000-0000-0000-000000000004', 'React', 'Frontend'),
    ('10000000-0000-0000-0000-000000000005', 'Docker', 'DevOps')
ON CONFLICT (id) DO UPDATE SET
    skill_name = EXCLUDED.skill_name,
    category = EXCLUDED.category;

INSERT INTO candidates (
    id,
    full_name,
    email,
    auth_provider,
    status,
    phone,
    source,
    is_duplicate
)
VALUES
    (
        '20000000-0000-0000-0000-000000000001',
        'Nguyen Van An',
        'an.nguyen@example.com',
        'KEYCLOAK',
        'ACTIVE',
        '0901234567',
        'WEBSITE',
        false
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        'Tran Thi Binh',
        'binh.tran@example.com',
        'KEYCLOAK',
        'ACTIVE',
        '0902345678',
        'LINKEDIN',
        false
    ),
    (
        '20000000-0000-0000-0000-000000000003',
        'Le Van Cuong',
        'cuong.le@example.com',
        'KEYCLOAK',
        'ACTIVE',
        '0903456789',
        'REFERRAL',
        false
    )
ON CONFLICT (id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    auth_provider = EXCLUDED.auth_provider,
    status = EXCLUDED.status,
    phone = EXCLUDED.phone,
    source = EXCLUDED.source,
    is_duplicate = EXCLUDED.is_duplicate;

INSERT INTO candidate_skills (candidate_id, skill_id, date)
VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', CURRENT_DATE),
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', CURRENT_DATE),
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', CURRENT_DATE),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000004', CURRENT_DATE),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000005', CURRENT_DATE),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', CURRENT_DATE),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000005', CURRENT_DATE)
ON CONFLICT (candidate_id, skill_id) DO UPDATE SET
    date = EXCLUDED.date;

COMMIT;

-- ============================================================
-- job-service database
-- ============================================================
\connect ats_job_dbs

BEGIN;

INSERT INTO departments (id, department_name, description, manager_id)
VALUES
    (
        '30000000-0000-0000-0000-000000000001',
        'Engineering',
        'Software development and engineering team',
        1001
    ),
    (
        '30000000-0000-0000-0000-000000000002',
        'Product',
        'Product management and product design team',
        1001
    ),
    (
        '30000000-0000-0000-0000-000000000003',
        'Human Resources',
        'Recruitment and people operations team',
        1002
    )
ON CONFLICT (id) DO UPDATE SET
    department_name = EXCLUDED.department_name,
    description = EXCLUDED.description,
    manager_id = EXCLUDED.manager_id;

INSERT INTO jobs (
    id,
    department_id,
    recruiter_id,
    title,
    description,
    location,
    employment_type,
    work_mode,
    salary_min,
    salary_max,
    currency,
    status,
    deadline,
    published_at,
    created_at,
    updated_at,
    utm_source
)
VALUES
    (
        '40000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000001',
        1001,
        'Backend Java Developer',
        'Build and maintain backend services for the ATS platform.',
        'Ho Chi Minh City',
        'FULL_TIME',
        'HYBRID',
        1800.00,
        3000.00,
        'USD',
        'PUBLISHED',
        CURRENT_DATE + 45,
        CURRENT_DATE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'seed'
    ),
    (
        '40000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000001',
        1001,
        'Frontend React Developer',
        'Develop accessible and responsive user interfaces for recruiters and candidates.',
        'Da Nang',
        'FULL_TIME',
        'REMOTE',
        1500.00,
        2600.00,
        'USD',
        'PUBLISHED',
        CURRENT_DATE + 60,
        CURRENT_DATE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'seed'
    ),
    (
        '40000000-0000-0000-0000-000000000003',
        '30000000-0000-0000-0000-000000000002',
        1002,
        'Product Intern',
        'Support product discovery, user research, and feature documentation.',
        'Hanoi',
        'INTERNSHIP',
        'ONSITE',
        300.00,
        600.00,
        'USD',
        'DRAFT',
        CURRENT_DATE + 90,
        NULL,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'seed'
    )
ON CONFLICT (id) DO UPDATE SET
    department_id = EXCLUDED.department_id,
    recruiter_id = EXCLUDED.recruiter_id,
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    location = EXCLUDED.location,
    employment_type = EXCLUDED.employment_type,
    work_mode = EXCLUDED.work_mode,
    salary_min = EXCLUDED.salary_min,
    salary_max = EXCLUDED.salary_max,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    deadline = EXCLUDED.deadline,
    published_at = EXCLUDED.published_at,
    updated_at = EXCLUDED.updated_at,
    utm_source = EXCLUDED.utm_source;

-- Xóa và tạo lại skill mapping của các job mẫu để script chạy lại an toàn.
DELETE FROM job_skills
WHERE job_id IN (
    '40000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000002',
    '40000000-0000-0000-0000-000000000003'
);

INSERT INTO job_skills (job_id, skill_id)
VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002'),
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003'),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000004'),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000005'),
    ('40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004');

COMMIT;

\connect ats_candidate_dbs
SELECT 'candidate-service: ' || COUNT(*) || ' candidates' AS result
FROM candidates;

\connect ats_job_dbs
SELECT 'job-service: ' || COUNT(*) || ' jobs' AS result
FROM jobs;
SQL

echo "Sample data seeded successfully."
