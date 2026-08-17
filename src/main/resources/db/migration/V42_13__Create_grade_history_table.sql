create table if not exists grade_history
(
    id uuid
        constraint grade_history_pk primary key,
    grade_id uuid not null
        constraint grade_history_grade_fk references grade (id),
    value double precision not null,
    reason varchar not null,
    modified_by varchar not null,
    modified_at timestamptz not null
);