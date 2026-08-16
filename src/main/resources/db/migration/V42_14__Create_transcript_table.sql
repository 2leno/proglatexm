create table if not exists transcript
(
    id uuid
        constraint transcript_pk primary key,
    student_id uuid not null
        constraint transcript_student_fk references student (id),
    year int not null,
    status varchar not null,
    s3_key varchar,
    updated_at timestamptz not null
);