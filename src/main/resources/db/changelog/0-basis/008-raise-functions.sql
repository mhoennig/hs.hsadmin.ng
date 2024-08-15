--liquibase formatted sql

-- ============================================================================
--changeset RAISE-FUNCTIONS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Like `RAISE EXCEPTION` ... just as an expression instead of a statement.
 */
create or replace function raiseException(msg text)
    returns varchar
    language plpgsql as $$
begin
    raise exception using message = msg;
end; $$;
--//


-- ============================================================================
--changeset ASSERT-FUNCTIONS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Like `ASSERT` but as an expression instead of a statement.
 */
create or replace function assertTrue(expectedTrue boolean, msg text)
    returns boolean
    language plpgsql as $$
begin
    assert expectedTrue, msg;
    return expectedTrue;
end; $$;
--//
