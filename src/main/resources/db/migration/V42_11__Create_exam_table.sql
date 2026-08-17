create table if not exists exam
(
    id uuid
        constraint exam_pk primary key,
    course_id uuid not null
        constraint exam_course_fk references course (id),
    name varchar not null,
    coefficient double precision not null
);