--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:RAISE-FUNCTIONS endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Like `RAISE EXCEPTION` ... just as an expression instead of a statement.
 */
create or replace function base.raiseException(msg text)
    returns varchar
    language plpgsql as $$
begin
    raise exception using message = msg;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:ASSERT-FUNCTIONS endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Like `ASSERT` but as an expression instead of a statement.
 */
create or replace function base.assertTrue(expectedTrue boolean, msg text)
    returns boolean
    language plpgsql as $$
begin
    assert expectedTrue, msg;
    return expectedTrue;
end; $$;
--//
