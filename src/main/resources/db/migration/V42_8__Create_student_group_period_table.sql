create table if not exists student_group_period
(
    id uuid
        constraint student_group_period_pk primary key,
    student_id uuid not null
        constraint sgp_student_fk references student (id),
    group_id uuid not null
        constraint sgp_group_fk references groups (id),
    start_date date not null,
    end_date date
);