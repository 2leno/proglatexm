alter table teacher
    add column reference varchar;

update teacher
set reference = 'TCH21001'
where username = 'teacher';

alter table teacher
    alter column reference set not null;

alter table teacher
    add constraint teacher_reference_key unique (reference);