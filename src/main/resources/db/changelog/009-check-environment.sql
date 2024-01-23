--liquibase formatted sql


-- ============================================================================
-- NUMERIC-HASH-FUNCTIONS
--changeset hash:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

do $$
    begin
        if starts_with ('${HSADMINNG_POSTGRES_ADMIN_USERNAME}', '$') then
            RAISE EXCEPTION 'environment variable HSADMINNG_POSTGRES_ADMIN_USERNAME not set';
        end if;
        if starts_with ('${HSADMINNG_POSTGRES_RESTRICTED_USERNAME}', '$') then
            RAISE EXCEPTION 'environment variable HSADMINNG_POSTGRES_RESTRICTED_USERNAME not set';
        end if;
    end $$
--//
