alter table student
    rename column student_code to reference;

alter table student
    rename constraint student_student_code_key to student_reference_key;

update student
set reference = 'STD24432'
where username = 'student';