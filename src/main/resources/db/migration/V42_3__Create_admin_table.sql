create table if not exists admin
(
    id uuid
        constraint admin_pk primary key,
    username varchar not null
        constraint admin_username_key unique,
    password varchar not null,
    first_name varchar not null,
    last_name varchar not null
);