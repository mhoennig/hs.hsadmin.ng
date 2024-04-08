--liquibase formatted sql

-- ============================================================================
-- LAST-ROW-COUNT
--changeset last-row-count:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the row count from the result of the previous query.
    Other than the native statement it's usable in an expression.
 */
create or replace function lastRowCount()
    returns bigint
    language plpgsql as $$
declare
    lastRowCount bigint;
begin
    get diagnostics lastrowCount = row_count;
    return lastRowCount;
end; $$;
--//
