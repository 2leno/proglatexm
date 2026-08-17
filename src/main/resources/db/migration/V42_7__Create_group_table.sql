create table if not exists groups
(
    id uuid
        constraint groups_pk primary key,
    reference varchar not null,
    promotion_id uuid not null
        constraint groups_promotion_fk references promotion (id)
);