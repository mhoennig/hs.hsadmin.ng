--liquibase formatted sql

-- ============================================================================
--changeset hs-global-historization-tx-history-txid:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function tx_history_txid()
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
    -- raise notice 'tx_history_txid() called with: (%, %)', historicalTxIdSetting, historicalTimestampSetting;

    if historicalTxIdSetting is null or historicalTxIdSetting = '' then
        select historicalTimestampSetting::timestamp into historicalTimestamp;
        select max(txc.txid) from tx_context txc where txc.txtimestamp <= historicalTimestamp into historicalTxId;
    else
        historicalTxId = historicalTxIdSetting::xid8;
    end if;
    return historicalTxId;
end; $$;
--//


-- ============================================================================
--changeset hs-global-historization-tx-historicize-tf:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create type "tx_operation" as enum ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE');

create or replace function tx_historicize_tf()
    returns trigger
    language plpgsql
    strict as $$
declare
    currentUser varchar(63);
    currentTask varchar(127);
    "row"       record;
    "alive"     boolean;
    "sql"       varchar;
begin
    -- determine user_id
    begin
        currentUser := current_setting('hsadminng.currentUser');
    exception
        when others then
            currentUser := null;
    end;
    if (currentUser is null or currentUser = '') then
        raise exception 'hsadminng.currentUser must be defined, please use "SET LOCAL ...;"';
    end if;
    raise notice 'currentUser: %', currentUser;

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

    sql := format('INSERT INTO %3$I_ex VALUES (DEFAULT, pg_current_xact_id(), %1$L, %2$L, $1.*)', TG_OP, alive, TG_TABLE_NAME);
    raise notice 'sql: %', sql;
    execute sql using "row";

    return "row";
end; $$;
--//


-- ============================================================================
--changeset hs-global-historization-tx-create-historicization:1 endDelimiter:--//
-- ----------------------------------------------------------------------------


create or replace procedure tx_create_historicization(baseTable varchar)
    language plpgsql as $$
declare
    createHistTableSql varchar;
    createTriggerSQL   varchar;
    viewName           varchar;
    exVersionsTable    varchar;
    createViewSQL      varchar;
    baseCols           varchar;
begin

    -- create the history table
    createHistTableSql = '' ||
                         'CREATE TABLE ' || baseTable || '_ex (' ||
                         '   version_id serial PRIMARY KEY,' ||
                         '   txid xid8 NOT NULL REFERENCES tx_context(txid),' ||
                         '   trigger_op tx_operation NOT NULL,' ||
                         '   alive boolean not null,' ||
                         '   LIKE ' || baseTable ||
                         '       EXCLUDING CONSTRAINTS' ||
                         '       EXCLUDING STATISTICS' ||
                         ')';
    raise notice 'sql: %', createHistTableSql;
    execute createHistTableSql;

    -- create the historical view
    viewName = quote_ident(format('%s_hv', baseTable));
    exVersionsTable = quote_ident(format('%s_ex', baseTable));
    baseCols = (select string_agg(quote_ident(column_name), ', ')
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = baseTable);

    createViewSQL = format(
            'CREATE OR REPLACE VIEW %1$s AS' ||
            '(' ||
                -- make sure the function is only called once, not for every matching row in tx_context
            '  WITH txh AS (SELECT tx_history_txid() AS txid) ' ||
            '  SELECT %2$s' ||
            '    FROM %3$s' ||
            '   WHERE alive = TRUE' ||
            '     AND version_id IN' ||
            '         (' ||
            '             SELECT max(ex.version_id) AS history_id' ||
            '               FROM %3$s AS ex' ||
            '               JOIN tx_context as txc ON ex.txid = txc.txid' ||
            '              WHERE txc.txid <= (SELECT txid FROM txh)' ||
            '              GROUP BY uuid' ||
            '         )' ||
            ')',
            viewName, baseCols, exVersionsTable
                    );
    raise notice 'sql: %', createViewSQL;
    execute createViewSQL;

    createTriggerSQL = 'CREATE TRIGGER ' || baseTable || '_tx_historicize_tg' ||
                       ' AFTER INSERT OR DELETE OR UPDATE ON ' || baseTable ||
                       '   FOR EACH ROW EXECUTE PROCEDURE tx_historicize_tf()';
    raise notice 'sql: %', createTriggerSQL;
    execute createTriggerSQL;

end; $$;
--//
