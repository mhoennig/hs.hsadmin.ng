--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:base-COMBINE-TABLE-SCHEMA-AND-NAME endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function base.combine_table_schema_and_name(tableSchema name, tableName name)
    returns text
    language plpgsql as $$
begin
    assert  LEFT(tableSchema, 1) <> '"', 'tableSchema must not start with "';
    assert  LEFT(tableName, 1) <> '"', 'tableName must not start with "';

    if tableSchema is null or tableSchema = 'public' or tableSchema = '' then
        return tableName::text;
    else
        return tableSchema::text || '.' || tableName::text;
    end if;
end; $$;
--//
