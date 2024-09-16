--liquibase formatted sql

-- ============================================================================
-- INT-TO-VAR
--changeset michael.hoennig:int-to-var endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns a textual representation of an integer number to be used as generated test data.

    Examples :
        base.intToVarChar(0, 3) => 'aaa'
        base.intToVarChar(1, 3) => 'aab'
 */
create or replace function base.intToVarChar(i integer, len integer)
    returns varchar
    language plpgsql as $$
declare
    partial varchar;
begin
    select chr(ascii('a') + i % 26) into partial;
    if len > 1 then
        return base.intToVarChar(i / 26, len - 1) || partial;
    else
        return partial;
    end if;
end; $$;
--//
