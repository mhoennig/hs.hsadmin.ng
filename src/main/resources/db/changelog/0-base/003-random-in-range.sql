--liquibase formatted sql


-- ============================================================================
-- RANDOM-IN-RANGE
--changeset michael.hoennig:random-in-range endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns a random integer in the given range (both included),
    to be used for test data generation.

    Example:
        base.randomInRange(0, 4) might return any of 0, 1, 2, 3, 4
 */
create or replace function base.randomInRange(min integer, max integer)
    returns integer
    returns null on null input
    language 'plpgsql' as $$
begin
    return floor(random() * (max - min + 1) + min);
end; $$;
--//




