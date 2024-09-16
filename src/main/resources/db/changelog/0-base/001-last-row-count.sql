--liquibase formatted sql

-- ============================================================================
-- LAST-ROW-COUNT
--changeset michael.hoennig:last-row-count endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the row count from the result of the previous query.
    Other than the native statement it's usable in an expression.
 */
create or replace function base.lastRowCount()
    returns bigint
    language plpgsql as $$
declare
    lastRowCount bigint;
begin
    get diagnostics lastRowCount = row_count;
    return lastRowCount;
end; $$;
--//
