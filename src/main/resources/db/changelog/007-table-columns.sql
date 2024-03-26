--liquibase formatted sql


-- ============================================================================
-- TABLE-COLUMNS-FUNCTION
--changeset table-columns-function:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function columnsNames( tableName text )
    returns text
    stable
    language 'plpgsql' as $$
declare columns text[];
begin
    columns := (select array(select column_name::text
                    from information_schema.columns
                    where table_name = tableName));
    return array_to_string(columns, ', ');
end; $$
--//
