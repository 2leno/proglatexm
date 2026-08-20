alter table exam
    add column schedule timestamp;

update exam
set schedule = '2026-01-01T00:00:00Z'
where schedule is null;

alter table exam
    alter column schedule set not null;