-- ============================================================================
-- V42_23__Complete_promotions_2021_2023_and_group_lucas.sql
--
-- Completes the demo dataset for the promotions that predate V42_21:
--   - 2021, 2022, 2023 (2024/2025 are already fully seeded)
-- Adds, per year: 2 groups (EL/TN), 14 students, group periods, exams,
-- grades and a transcript, following the V42_21 conventions:
--   - deterministic ids via md5('key')::uuid
--   - idempotent inserts via "on conflict do nothing"
-- Also assigns the student 'hei.lucas.13' (email hei.lucas.13@gmail.com,
-- reference STD26001, parcours EL) to the G-EL-2024 group of promotion 2024.
--
-- Applies on every environment (dev, test, preprod, prod).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Working table: students for 2021..2023
-- ----------------------------------------------------------------------------
drop table if exists tmp_promo21_23_students;
create temporary table tmp_promo21_23_students as
select
    y,
    n,
    'student' || (y - 2000) || lpad(n::text, 3, '0') as username,
    case when n <= 7 then 'EL' else 'TN' end as parcours,
    'G-' || case when n <= 7 then 'EL' else 'TN' end || '-' || y as group_code,
    'STD' || (y - 2000) || lpad(n::text, 3, '0') as reference,
    names.first_name,
    names.last_name
from generate_series(2021, 2023) as y
cross join lateral generate_series(1, 14) as n
cross join lateral (
    select first_name, last_name
    from (values
        ('Lucas','Petit'), ('Emma','Roux'), ('Hugo','Fournier'), ('Lea','Girard'),
        ('Louis','Bonnet'), ('Chloe','Faure'), ('Nathan','Mercier'), ('Manon','Blanc'),
        ('Theo','Garnier'), ('Camille','Rousseau'), ('Enzo','Muller'), ('Sarah','Henry'),
        ('Rayan','Lambert'), ('Jade','Fontaine')
    ) as names(first_name, last_name)
    offset (n - 1) limit 1
) as names;

-- ----------------------------------------------------------------------------
-- Groups for 2021..2023 (2024/2025 already exist)
-- ----------------------------------------------------------------------------
insert into groups (id, reference, promotion_id)
select md5('group:' || g.code)::uuid, g.code, md5('promotion:' || g.year)::uuid
from (values
    ('G-EL-2021', 2021), ('G-TN-2021', 2021),
    ('G-EL-2022', 2022), ('G-TN-2022', 2022),
    ('G-EL-2023', 2023), ('G-TN-2023', 2023)
) as g(code, year)
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Students
-- ----------------------------------------------------------------------------
insert into student (id, username, password, first_name, last_name, reference, parcours, email)
select
    md5('student:' || username)::uuid,
    username,
    '$2a$10$i9N9mZu4WiCukm2G4z0yi.WjdYotthstxwKQK0.IQJLCyA6elP2OO',
    first_name,
    last_name,
    reference,
    parcours,
    username || '@proglatexm.com'
from tmp_promo21_23_students
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Student / group assignment
-- ----------------------------------------------------------------------------
insert into student_group_period (id, student_id, group_id, start_date, end_date)
select
    md5('sgp:' || username)::uuid,
    md5('student:' || username)::uuid,
    md5('group:' || group_code)::uuid,
    (y || '-09-01')::date,
    null
from tmp_promo21_23_students
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Exams per year and course (2021..2025; 2026 finals already exist)
-- Midterm is scheduled mid-year, finale is scheduled end of the very same year,
-- so every year owns a complete pair of exams per course.
-- ----------------------------------------------------------------------------
insert into exam (id, course_id, name, coefficient, schedule)
select
    md5('exam:' || c.reference || ':' || y || ':' || e.exam_name)::uuid,
    c.id,
    e.exam_name,
    e.coefficient,
    (y || '-' || e.month_day)::timestamp
from course c
cross join lateral (
    values
        ('Midterm', 0.4::double precision, '05-15'),
        ('Final', 0.6::double precision, '11-15')
) as e(exam_name, coefficient, month_day)
cross join generate_series(2021, 2025) as y
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Grades: each student of year Y is graded on the exams scheduled in year Y
-- for the courses of their track.
-- ----------------------------------------------------------------------------
insert into grade (id, student_id, exam_id, value, current)
select
    md5('grade:' || s.username || ':' || c.reference || ':' || y || ':' || e.exam_name)::uuid,
    md5('student:' || s.username)::uuid,
    md5('exam:' || c.reference || ':' || y || ':' || e.exam_name)::uuid,
    least(20.0, greatest(4.0, round(
        (8 + ((((y * 7) + length(c.reference) * 3) % 90) / 10.0))::numeric, 1
    ))) as value,
    true
from tmp_promo21_23_students s
join course c on c.parcours = s.parcours
cross join lateral (
    values
        ('Midterm', 0.4::double precision, '05-15'),
        ('Final', 0.6::double precision, '11-15')
) as e(exam_name, coefficient, month_day)
where c.reference like (case when s.parcours = 'EL' then 'ELG%' else 'TNG%' end)
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Transcripts for the new students (PENDING so they can be generated and sent)
-- ----------------------------------------------------------------------------
insert into transcript (id, student_id, year, status, s3_key, updated_at)
select
    md5('transcript:' || username || ':' || y)::uuid,
    md5('student:' || username)::uuid,
    y,
    'PENDING',
    null,
    now()
from tmp_promo21_23_students
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Assign hei.lucas.13 (STD26001, EL, 2024 promotion) to group G-EL-2024
-- The student was created through the API, so its id is not the deterministic
-- md5-based one: it is looked up by email to stay idempotent.
-- ----------------------------------------------------------------------------
insert into student_group_period (id, student_id, group_id, start_date, end_date)
select
    md5('sgp:hei.lucas.13')::uuid,
    s.id,
    g.id,
    '2024-09-01',
    null
from student s
cross join groups g
where s.email = 'hei.lucas.13@gmail.com'
  and g.reference = 'G-EL-2024'
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Grades for year 2024 for hei.lucas.13 (same courses/exams as V42_21 pattern,
-- scheduled in 2024 so his 2024 transcript is not empty)
-- ----------------------------------------------------------------------------
insert into grade (id, student_id, exam_id, value, current)
select
    md5('grade:hei.lucas.13:' || c.reference || ':2024:' || e.name)::uuid,
    s.id,
    e.id,
    grades.value,
    true
from student s
join course c on c.parcours = 'EL'
join exam e
    on e.course_id = c.id
    and e.name in ('Midterm', 'Final')
    and extract(year from e.schedule) = 2024
join (values
    ('ELG101', 'Midterm', 13.5),
    ('ELG101', 'Final', 14.0),
    ('ELG102', 'Midterm', 15.0),
    ('ELG102', 'Final', 12.5),
    ('ELG103', 'Midterm', 16.0),
    ('ELG103', 'Final', 15.5)
) as grades(ref, exam_name, value)
    on grades.ref = c.reference and grades.exam_name = e.name
where s.email = 'hei.lucas.13@gmail.com'
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Transcript year 2024 for hei.lucas.13 (PENDING so it can be generated/sent)
-- ----------------------------------------------------------------------------
insert into transcript (id, student_id, year, status, s3_key, updated_at)
select
    md5('transcript:hei.lucas.13:2024')::uuid,
    s.id,
    2024,
    'PENDING',
    null,
    now()
from student s
where s.email = 'hei.lucas.13@gmail.com'
on conflict do nothing;

drop table tmp_promo21_23_students;