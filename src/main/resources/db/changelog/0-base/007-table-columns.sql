--liquibase formatted sql


-- ============================================================================
-- TABLE-COLUMNS-FUNCTION
--changeset michael.hoennig:table-columns-function endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function base.tableColumnNames( ofTableName text )
    returns text
    stable
    language 'plpgsql' as $$
declare
    tableName   text;
    tableSchema text;
    columns     text[];
begin
    tableSchema := CASE
                       WHEN position('.' in ofTableName) > 0 THEN split_part(ofTableName, '.', 1)
                       ELSE 'public'
                   END;

    tableName := CASE
                     WHEN position('.' in ofTableName) > 0 THEN split_part(ofTableName, '.', 2)
                     ELSE ofTableName
        END;

    columns := (select array(select column_name::text
                               from information_schema.columns
                              where table_name = tableName
                                and table_schema = tableSchema));
    assert cardinality(columns) > 0, 'cannot determine columns of table ' || ofTableName ||
                                     '("' || tableSchema || '"."' || tableName || '")';
    return array_to_string(columns, ', ');
end; $$
--//
