--liquibase formatted sql

-- ============================================================================
-- RAISE-FUNCTIONS
--changeset RAISE-FUNCTIONS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Like RAISE EXCEPTION ... just as an expression instead of a statement.
 */
create or replace function raiseException(msg text)
    returns varchar
    language plpgsql as $$
begin
    raise exception using message = msg;
end; $$;
--//
