--liquibase formatted sql

--changeset random-in-range:1 endDelimiter:--//

/*
    Returns a random integer in the given range (both included),
    to be used for test data generation.

    Example:
        randomInRange(0, 4) might return any of 0, 1, 2, 3, 4
 */
create or replace function randomInRange(min integer, max integer)
    returns integer
    returns null on null input
    language 'plpgsql' as $$
begin
    return floor(random() * (max - min + 1) + min);
end; $$;
--//




