--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-global-historization-tx-history-txid endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function base.tx_history_txid()
    returns xid8 stable
    language plpgsql as $$
declare
    historicalTxIdSetting text;
    historicalTimestampSetting text;
    historicalTxId xid8;
    historicalTimestamp timestamp;
begin
    select coalesce(current_setting('hsadminng.tx_history_txid', true), '') into historicalTxIdSetting;
    select coalesce(current_setting('hsadminng.tx_history_timestamp', true), '') into historicalTimestampSetting;
    if historicalTxIdSetting > '' and historicalTimestampSetting > '' then
        raise exception 'either hsadminng.tx_history_txid or hsadminng.tx_history_timestamp must be set, but both are set: (%, %)',
            historicalTxIdSetting, historicalTimestampSetting;
    end if;
    if historicalTxIdSetting = '' and historicalTimestampSetting = '' then
        raise exception 'either hsadminng.tx_history_txid or hsadminng.tx_history_timestamp must be set, but both are unset or empty: (%, %)',
            historicalTxIdSetting, historicalTimestampSetting;
    end if;
    -- just for debugging / making sure the function is only called once per query
    -- raise notice 'base.tx_history_txid() called with: (%, %)', historicalTxIdSetting, historicalTimestampSetting;

    if historicalTxIdSetting is null or historicalTxIdSetting = '' then
        select historicalTimestampSetting::timestamp into historicalTimestamp;
        select max(txc.txid) from base.tx_context txc where txc.txtimestamp <= historicalTimestamp into historicalTxId;
    else
        historicalTxId = historicalTxIdSetting::xid8;
    end if;
    return historicalTxId;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-global-historization-tx-historicize-tf endDelimiter:--//
-- ----------------------------------------------------------------------------

-- create type base.tx_operation as enum ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE');

create or replace function base.tx_historicize_tf()
    returns trigger
    language plpgsql
    strict as $$
declare
    currentSubject varchar(63);
    currentTask varchar(127);
    "row"       record;
    "alive"     boolean;
    "sql"       varchar;
begin
    -- determine user_id
    begin
        currentSubject := current_setting('hsadminng.currentSubject');
    exception
        when others then
            currentSubject := null;
    end;
    if (currentSubject is null or currentSubject = '') then
        raise exception 'hsadminng.currentSubject must be defined, please use "SET LOCAL ...;"';
    end if;

    -- determine task
    currentTask = current_setting('hsadminng.currentTask');
    assert currentTask is not null and length(currentTask) >= 12,
        format('hsadminng.currentTask (%s) must be defined and min 12 characters long, please use "SET LOCAL ...;"',
               currentTask);
    assert length(currentTask) <= 127,
        format('hsadminng.currentTask (%s) must not be longer than 127 characters"', currentTask);

    if (TG_OP = 'INSERT') or (TG_OP = 'UPDATE') then
        "row" := NEW;
        "alive" := true;
    else -- DELETE or TRUNCATE
        "row" := OLD;
        "alive" := false;
    end if;

    sql := format('INSERT INTO %3$s_ex VALUES (DEFAULT, pg_current_xact_id(), %1$L, %2$L, $1.*)',
                  TG_OP, alive, base.combine_table_schema_and_name(tg_table_schema, tg_table_name)::name);
    -- raise exception 'generated-SQL: %', sql;
    execute sql using "row";

    return "row";
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-global-historization-tx-create-historicization runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure base.tx_create_historicization(
    basetable varchar -- format 'schemaname.tablename'
)
    language plpgsql as $$
declare
    baseSchemaName     varchar;
    baseTableName      varchar;
    createHistTableSql varchar;
    createTriggerSQL   varchar;
    viewName           varchar;
    exVersionsTable    varchar;
    createViewSQL      varchar;
    baseCols           varchar;
begin

    -- determine schema and pure table name
    SELECT split_part(basetable, '.', 1),
           split_part(basetable, '.', 2)
        INTO baseSchemaName, baseTableName;

    -- create the history table
    createHistTableSql = '' ||
                         'CREATE TABLE ' || basetable || '_ex (' ||
                         '   version_id serial PRIMARY KEY,' ||
                         '   txid xid8 NOT NULL REFERENCES base.tx_context(txid),' ||
                         '   trigger_op base.tx_operation NOT NULL,' ||
                         '   alive boolean not null,' ||
                         '   LIKE ' || basetable ||
                         '       EXCLUDING CONSTRAINTS' ||
                         '       EXCLUDING STATISTICS' ||
                         ')';
    -- raise notice 'sql: %', createHistTableSql;
    execute createHistTableSql;

    -- create the historical view
    viewName = basetable || '_hv';
    exVersionsTable = basetable || '_ex';
    baseCols = (select string_agg(quote_ident(column_name), ', ')
                    from information_schema.columns
                    where table_schema = baseSchemaName
                      and table_name = baseTableName);

    createViewSQL = format(
            'CREATE OR REPLACE VIEW %1$s AS' ||
            '(' ||
                -- make sure the function is only called once, not for every matching row in base.tx_context
            '  WITH txh AS (SELECT base.tx_history_txid() AS txid) ' ||
            '  SELECT %2$s' ||
            '    FROM %3$s' ||
            '   WHERE alive = TRUE' ||
            '     AND version_id IN' ||
            '         (' ||
            '             SELECT max(ex.version_id) AS history_id' ||
            '               FROM %3$s AS ex' ||
            '               JOIN base.tx_context as txc ON ex.txid = txc.txid' ||
            '              WHERE txc.txid <= (SELECT txid FROM txh)' ||
            '              GROUP BY uuid' ||
            '         )' ||
            ')',
            viewName, baseCols, exVersionsTable
        );
    -- raise notice 'generated-sql: %', createViewSQL;
    execute createViewSQL;

    -- "-9-" to put the trigger execution after any alphabetically lesser tx-triggers
    createTriggerSQL = 'CREATE TRIGGER tx_9_historicize_tg' ||
                       ' AFTER INSERT OR DELETE OR UPDATE ON ' || basetable ||
                       '   FOR EACH ROW EXECUTE PROCEDURE base.tx_historicize_tf()';
    execute createTriggerSQL;

end; $$;
--//
