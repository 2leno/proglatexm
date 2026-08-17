create table if not exists student
(
    id uuid
        constraint student_pk primary key,
    username varchar not null
        constraint student_username_key unique,
    password varchar not null,
    first_name varchar not null,
    last_name varchar not null,
    reference varchar not null
        constraint student_reference_key unique,
    parcours varchar not null
);