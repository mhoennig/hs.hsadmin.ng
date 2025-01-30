--liquibase formatted sql

-- FIXME: check if we really need the restricted user

-- ============================================================================
-- NUMERIC-HASH-FUNCTIONS
--changeset michael.hoennig:hash runOnChange:true validCheckSum:ANY endDelimiter:--//
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
