create table if not exists course
(
    id uuid
        constraint course_pk primary key,
    reference varchar not null,
    title varchar not null,
    credits int not null,
    parcours varchar not null
);