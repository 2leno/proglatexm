create table if not exists teacher
(
    id uuid
        constraint teacher_pk primary key,
    username varchar not null
        constraint teacher_username_key unique,
    password varchar not null,
    reference varchar not null
        constraint teacher_reference_key unique,
    first_name varchar not null,
    last_name varchar not null
);