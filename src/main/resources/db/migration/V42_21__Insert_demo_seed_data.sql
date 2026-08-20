-- ============================================================================
-- V42_21__Insert_demo_seed_data.sql
--
-- Demo dataset:
--   - 2 promotions (2024, 2025)
--   - 4 groups (2 tracks x 2 promotions)
--   - 5 teachers
--   - 28 students (14 per promotion, 7 per group)
--   - 6 courses (3 per track)
--   - 12 exams (2 per course)
--   - ~168 grades, including a subset with correction history
--   - 28 transcripts (one per student)
--
-- IDs are deterministically generated via md5('key')::uuid so the script is
-- readable and replayable (idempotent thanks to "on conflict do nothing").
-- Applies on every environment (dev, test, preprod, prod).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Working table: students
-- ----------------------------------------------------------------------------
create temporary table tmp_seed_students as
select
    n,
    'student' || lpad(n::text, 3, '0') as username,
    case
        when n <= 7 then 'G-EL-2024'
        when n <= 14 then 'G-TN-2024'
        when n <= 21 then 'G-EL-2025'
        else 'G-TN-2025'
    end as group_code,
    case when n <= 14 then 2024 else 2025 end as promo_year,
    case
        when n <= 7 or (n > 14 and n <= 21) then 'EL'
        else 'TN'
    end as parcours,
    case
        when n <= 14 then 'STD24' || lpad(n::text, 3, '0')
        else 'STD25' || lpad((n - 14 + 100)::text, 3, '0')
    end as reference,
    fn.first_name,
    fn.last_name
from generate_series(1, 28) as n
cross join lateral (
    select first_name, last_name
    from (values
        ('Lucas','Petit'), ('Emma','Roux'), ('Hugo','Fournier'), ('Lea','Girard'),
        ('Louis','Bonnet'), ('Chloe','Faure'), ('Nathan','Mercier'), ('Manon','Blanc'),
        ('Theo','Garnier'), ('Camille','Rousseau'), ('Enzo','Muller'), ('Sarah','Henry'),
        ('Rayan','Lambert'), ('Jade','Fontaine'), ('Arthur','Barbier'), ('Ines','Chevalier'),
        ('Mathis','Francois'), ('Lina','Legrand'), ('Noah','Gauthier'), ('Zoe','Perrin'),
        ('Ethan','Morin'), ('Louise','Robin'), ('Gabriel','Clement'), ('Alice','Morel'),
        ('Adam','Simon'), ('Anna','Michel'), ('Raphael','Lefevre'), ('Julia','Andre')
    ) as names(first_name, last_name)
    offset (n - 1) limit 1
) as fn;

-- ----------------------------------------------------------------------------
-- Working table: courses + exams (denormalized to simplify the seed)
-- ----------------------------------------------------------------------------
create temporary table tmp_seed_courses as
select * from (values
    ('ELG101', 'Algorithmics and data structures', 6, 'EL',   'teacher.dubois'),
    ('ELG102', 'Applied mathematics',              5, 'EL',   'teacher.martin'),
    ('ELG103', 'Technical English',                3, 'EL',   'teacher.leroy'),
    ('TNG101', 'Relational databases',             6, 'TN',   'teacher.bernard'),
    ('TNG102', 'Networks and protocols',           5, 'TN',   'teacher.moreau'),
    ('TNG103', 'Web programming',                  4, 'TN',   'teacher.dubois')
) as c(reference, title, credits, parcours, teacher_username);

create temporary table tmp_seed_exams as
select
    c.reference as course_reference,
    c.parcours,
    c.teacher_username,
    e.exam_name,
    e.coefficient,
    e.schedule
from tmp_seed_courses c
cross join lateral (
    values
        ('Midterm', 0.4::double precision, timestamp '2025-11-15 09:00:00'),
        ('Final',   0.6::double precision, timestamp '2026-01-20 09:00:00')
) as e(exam_name, coefficient, schedule);

-- ----------------------------------------------------------------------------
-- Working table: grades (each student x every exam of their track)
-- ----------------------------------------------------------------------------
create temporary table tmp_seed_grades as
select
    s.n,
    s.username as student_username,
    e.course_reference,
    e.exam_name,
    e.teacher_username,
    least(20.0, greatest(4.0, round(
        (8 + (((s.n * 31) + (length(e.exam_name) * 7) + (length(e.course_reference) * 3)) % 121) / 10.0)::numeric, 1
    ))) as value
from tmp_seed_students s
join tmp_seed_exams e on e.parcours = s.parcours;

-- ----------------------------------------------------------------------------
-- Promotions
-- ----------------------------------------------------------------------------
insert into promotion (id, name, year)
values
    (md5('promotion:2024')::uuid, 'Promotion 2024', 2024),
    (md5('promotion:2025')::uuid, 'Promotion 2025', 2025)
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Groups
-- ----------------------------------------------------------------------------
insert into groups (id, reference, promotion_id)
select md5('group:' || g.code)::uuid, g.code, md5('promotion:' || g.year)::uuid
from (values
    ('G-EL-2024', 2024),
    ('G-TN-2024', 2024),
    ('G-EL-2025', 2025),
    ('G-TN-2025', 2025)
) as g(code, year)
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Teachers (demo password identical to the default "teacher" account)
-- ----------------------------------------------------------------------------
insert into teacher (id, username, password, first_name, last_name, reference)
select
    md5('teacher:' || t.username)::uuid,
    t.username,
    '$2a$10$oY7434KvSbHqLRH4Lt/wuuT8qaIfHFnS4zYLvtNiUlDcRye13A5U2',
    t.first_name,
    t.last_name,
    t.reference
from (values
    ('teacher.dubois',  'Claire', 'Dubois',  'TCH24001'),
    ('teacher.martin',  'Julien', 'Martin',  'TCH24002'),
    ('teacher.leroy',   'Sophie', 'Leroy',   'TCH24003'),
    ('teacher.bernard', 'Marc',   'Bernard', 'TCH24004'),
    ('teacher.moreau',  'Anne',   'Moreau',  'TCH24005')
) as t(username, first_name, last_name, reference)
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Students (demo password identical to the default "student" account)
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
from tmp_seed_students
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Student / group assignment
-- ----------------------------------------------------------------------------
insert into student_group_period (id, student_id, group_id, start_date, end_date)
select
    md5('sgp:' || username)::uuid,
    md5('student:' || username)::uuid,
    md5('group:' || group_code)::uuid,
    (promo_year || '-09-01')::date,
    null
from tmp_seed_students
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Courses
-- ----------------------------------------------------------------------------
insert into course (id, reference, title, credits, parcours)
select md5('course:' || reference)::uuid, reference, title, credits, parcours
from tmp_seed_courses
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Course / teacher assignment
-- ----------------------------------------------------------------------------
insert into course_teacher (course_id, teacher_id)
select md5('course:' || reference)::uuid, md5('teacher:' || teacher_username)::uuid
from tmp_seed_courses
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Exams
-- ----------------------------------------------------------------------------
insert into exam (id, course_id, name, coefficient, schedule)
select
    md5('exam:' || course_reference || ':' || exam_name)::uuid,
    md5('course:' || course_reference)::uuid,
    exam_name,
    coefficient,
    schedule
from tmp_seed_exams
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Grades
-- ----------------------------------------------------------------------------
insert into grade (id, student_id, exam_id, value, current)
select
    md5('grade:' || student_username || ':' || course_reference || ':' || exam_name)::uuid,
    md5('student:' || student_username)::uuid,
    md5('exam:' || course_reference || ':' || exam_name)::uuid,
    value,
    true
from tmp_seed_grades
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Correction history on ~1/6 of grades (simulates a re-entered grade)
-- ----------------------------------------------------------------------------
insert into grade_history (id, grade_id, value, reason, modified_by, modified_at)
select
    md5('gradehist:' || student_username || ':' || course_reference || ':' || exam_name)::uuid,
    md5('grade:' || student_username || ':' || course_reference || ':' || exam_name)::uuid,
    greatest(0.0, value - 3.0),
    'Entry error corrected after review',
    teacher_username,
    now() - interval '4 days'
from tmp_seed_grades
where (n + length(exam_name)) % 6 = 0
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- Transcripts
-- ----------------------------------------------------------------------------
insert into transcript (id, student_id, year, status, s3_key, updated_at)
select
    md5('transcript:' || username || ':' || promo_year)::uuid,
    md5('student:' || username)::uuid,
    promo_year,
    'GENERATED',
    'transcripts/' || username || '-' || promo_year || '.pdf',
    now()
from tmp_seed_students
on conflict do nothing;

drop table tmp_seed_grades;
drop table tmp_seed_exams;
drop table tmp_seed_courses;
drop table tmp_seed_students;