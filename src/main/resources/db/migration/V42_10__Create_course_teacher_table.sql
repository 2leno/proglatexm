create table if not exists course_teacher
(
    course_id uuid not null
        constraint course_teacher_course_fk references course (id),
    teacher_id uuid not null
        constraint course_teacher_teacher_fk references teacher (id),
    constraint course_teacher_pk primary key (course_id, teacher_id)
);