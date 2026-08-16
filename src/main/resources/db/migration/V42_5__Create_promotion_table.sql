create table if not exists promotion
(
    id uuid
        constraint promotion_pk primary key,
    name varchar not null,
    year int not null
);