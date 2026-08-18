alter table student
    add column email varchar;

update student
set email = 'student@proglatexm.com'
where username = 'student';

alter table student
    alter column email set not null;

alter table student
    add constraint student_email_unique unique (email);