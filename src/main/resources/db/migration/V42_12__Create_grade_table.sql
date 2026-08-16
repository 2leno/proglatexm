create table if not exists grade
(
    id uuid
        constraint grade_pk primary key,
    student_id uuid not null
        constraint grade_student_fk references student (id),
    exam_id uuid not null
        constraint grade_exam_fk references exam (id),
    value double precision not null,
    current boolean not null
);