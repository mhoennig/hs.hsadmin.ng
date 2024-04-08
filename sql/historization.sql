
-- ========================================================
-- Historization
-- --------------------------------------------------------

CREATE TABLE "tx_history" (
                              "tx_id" BIGINT NOT NULL UNIQUE,
                              "tx_timestamp" TIMESTAMP NOT NULL,
                              "user" VARCHAR(64) NOT NULL, -- references postgres user
                              "task" VARCHAR NOT NULL
);

CREATE TYPE "operation" AS ENUM ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE');

-- see https://www.postgresql.org/docs/current/plpgsql-trigger.html

CREATE OR REPLACE FUNCTION historicize()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    currentUser VARCHAR(63);
    currentTask VARCHAR(127);
    "row" RECORD;
    "alive" BOOLEAN;
    "sql" varchar;
BEGIN
    -- determine user_id
BEGIN
        currentUser := current_setting('hsadminng.currentUser');
EXCEPTION WHEN OTHERS THEN
        currentUser := NULL;
END;
    IF (currentUser IS NULL OR currentUser = '') THEN
        RAISE EXCEPTION 'hsadminng.currentUser must be defined, please use "SET LOCAL ...;"';
END IF;
    RAISE NOTICE 'currentUser: %', currentUser;

    -- determine task
    currentTask = current_setting('hsadminng.currentTask');
    assert currentTask IS NOT NULL AND length(currentTask) >= 12,
        format('hsadminng.currentTask (%s) must be defined and min 12 characters long, please use "SET LOCAL ...;"', currentTask);
    assert length(currentTask) <= 127,
        format('hsadminng.currentTask (%s) must not be longer than 127 characters"', currentTask);

    IF (TG_OP = 'INSERT') OR (TG_OP = 'UPDATE') THEN
        "row" := NEW;
        "alive" := TRUE;
    ELSE -- DELETE or TRUNCATE
            "row" := OLD;
            "alive" := FALSE;
    END IF;

    sql := format('INSERT INTO tx_history VALUES (txid_current(), now(), %1L, %2L) ON CONFLICT DO NOTHING', currentUser, currentTask);
    RAISE NOTICE 'sql: %', sql;
    EXECUTE sql;
    sql := format('INSERT INTO %3$I_versions VALUES (DEFAULT, txid_current(), %1$L, %2$L, $1.*)', TG_OP, alive, TG_TABLE_NAME);
        RAISE NOTICE 'sql: %', sql;
    EXECUTE sql USING "row";

    RETURN "row";
END; $$;

CREATE OR REPLACE PROCEDURE create_historical_view(baseTable varchar)
    LANGUAGE plpgsql AS $$
DECLARE
createTriggerSQL varchar;
    viewName varchar;
    versionsTable varchar;
    createViewSQL varchar;
    baseCols varchar;
BEGIN

    viewName = quote_ident(format('%s_hv', baseTable));
    versionsTable = quote_ident(format('%s_versions', baseTable));
    baseCols = (SELECT string_agg(quote_ident(column_name), ', ')
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = baseTable);

    createViewSQL = format(
                'CREATE OR REPLACE VIEW %1$s AS' ||
                '(' ||
                '  SELECT %2$s' ||
                '    FROM %3$s' ||
                '   WHERE alive = TRUE' ||
                '     AND version_id IN' ||
                '         (' ||
                '             SELECT max(vt.version_id) AS history_id' ||
                '               FROM %3$s AS vt' ||
                '               JOIN tx_history as txh ON vt.tx_id = txh.tx_id' ||
                '              WHERE txh.tx_timestamp <= current_setting(''hsadminng.timestamp'')::timestamp' ||
                '              GROUP BY id' ||
                '         )' ||
                ')',
                viewName, baseCols, versionsTable
        );
    RAISE NOTICE 'sql: %', createViewSQL;
EXECUTE createViewSQL;

createTriggerSQL = 'CREATE TRIGGER ' || baseTable || '_historicize' ||
                    ' AFTER INSERT OR DELETE OR UPDATE ON ' || baseTable ||
                    '   FOR EACH ROW EXECUTE PROCEDURE historicize()';
    RAISE NOTICE 'sql: %', createTriggerSQL;
EXECUTE createTriggerSQL;

END; $$;

CREATE OR REPLACE PROCEDURE create_historicization(baseTable varchar)
    LANGUAGE plpgsql AS $$
DECLARE
    createHistTableSql varchar;
    createTriggerSQL varchar;
    viewName varchar;
    versionsTable varchar;
    createViewSQL varchar;
    baseCols varchar;
BEGIN

    -- create the history table
    createHistTableSql = '' ||
        'CREATE TABLE ' || baseTable || '_versions (' ||
        '   version_id serial PRIMARY KEY,' ||
        '   tx_id bigint NOT NULL REFERENCES tx_history(tx_id),' ||
        '   trigger_op operation NOT NULL,' ||
        '   alive boolean not null,' ||

        '   LIKE ' || baseTable ||
        '       EXCLUDING CONSTRAINTS' ||
        '       EXCLUDING STATISTICS' ||
        ')';
    RAISE NOTICE 'sql: %', createHistTableSql;
    EXECUTE createHistTableSql;

    -- create the historical view
    viewName = quote_ident(format('%s_hv', baseTable));
        versionsTable = quote_ident(format('%s_versions', baseTable));
        baseCols = (SELECT string_agg(quote_ident(column_name), ', ')
                    FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = baseTable);

        createViewSQL = format(
            'CREATE OR REPLACE VIEW %1$s AS' ||
            '(' ||
            '  SELECT %2$s' ||
            '    FROM %3$s' ||
            '   WHERE alive = TRUE' ||
            '     AND version_id IN' ||
            '         (' ||
            '             SELECT max(vt.version_id) AS history_id' ||
            '               FROM %3$s AS vt' ||
            '               JOIN tx_history as txh ON vt.tx_id = txh.tx_id' ||
            '              WHERE txh.tx_timestamp <= current_setting(''hsadminng.timestamp'')::timestamp' ||
            '              GROUP BY id' ||
            '         )' ||
            ')',
            viewName, baseCols, versionsTable
            );
        RAISE NOTICE 'sql: %', createViewSQL;
    EXECUTE createViewSQL;

    createTriggerSQL = 'CREATE TRIGGER ' || baseTable || '_historicize' ||
                           ' AFTER INSERT OR DELETE OR UPDATE ON ' || baseTable ||
                           '   FOR EACH ROW EXECUTE PROCEDURE historicize()';
        RAISE NOTICE 'sql: %', createTriggerSQL;
    EXECUTE createTriggerSQL;

END; $$;
