-- ============================================================================
-- V42_22__Insert_missing_promotions.sql
--
-- Adds the promotions that predate the demo seed (V42_21): years 2021 to 2023.
-- Uses the same deterministic md5-based ids and "on conflict do nothing"
-- pattern so the script is idempotent and consistent with V42_21.
--
-- Applies on every environment (dev, test, preprod, prod).
-- ============================================================================

insert into promotion (id, name, year)
values
    (md5('promotion:2021')::uuid, 'Promotion 2021', 2021),
    (md5('promotion:2022')::uuid, 'Promotion 2022', 2022),
    (md5('promotion:2023')::uuid, 'Promotion 2023', 2023)
on conflict do nothing;