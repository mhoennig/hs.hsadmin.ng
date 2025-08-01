--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:audit-OPERATION-TYPE endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A type representing a DML operation.
 */
do $$
    begin
        if not exists(select 1 from pg_type where typname = 'base.tx_operation') then
            create type base.tx_operation as enum ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE');
        end if;
        --more types here...
    end $$;
--//

-- ============================================================================
--changeset michael.hoennig:audit-TX-CONTEXT-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A table storing transactions with context data.
 */
create table base.tx_context
(
    txId            xid8            primary key     not null,
    txTimestamp     timestamp                       not null,
    currentSubject  varchar(63)                     not null, -- not the uuid, because users can be deleted
    assumedRoles    varchar(1023)                   not null, -- not the uuids, because roles can be deleted
    currentTask     varchar(127)                    not null,
    currentRequest  text                            not null
);

create index on base.tx_context using brin (txTimestamp);
--//


-- ============================================================================
--changeset michael.hoennig:audit-TX-CONTEXT-TABLE-COLUMN-SEQUENTIAL-TX-ID endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Adds a column to base.tx_context which keeps a strictly sequentially ordered tx-id.
 */

alter table base.tx_context
    add column seqTxId BIGINT;

CREATE OR REPLACE FUNCTION set_next_sequential_txid()
    RETURNS TRIGGER AS $$
BEGIN
    LOCK TABLE base.tx_context IN EXCLUSIVE MODE;
    SELECT COALESCE(MAX(seqTxId)+1, 0) INTO NEW.seqTxId FROM base.tx_context;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_commit_order_trigger
    BEFORE INSERT ON base.tx_context
    FOR EACH ROW
EXECUTE FUNCTION set_next_sequential_txid();
--//


-- ============================================================================
--changeset michael.hoennig:audit-TX-JOURNAL-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A table storing the transaction audit journal for all target tables it's configured for.
 */
create table base.tx_journal
(
    txId        xid8                not null references base.tx_context (txId),
    targetTable text                not null,
    targetUuid  uuid                not null, -- Assumes that all audited tables have a uuid column.
    targetOp    base.tx_operation   not null,
    targetDelta jsonb
);

create index on base.tx_journal (targetTable, targetUuid);
--//

-- ============================================================================
--changeset michael.hoennig:audit-TX-JOURNAL-VIEW runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A view combining base.tx_journal with base.tx_context.
 */
drop view if exists base.tx_journal_v;
create view base.tx_journal_v as
select txc.seqTxId,
       txc.txId,
       txc.txTimeStamp,
       txc.currentSubject,
       txc.assumedRoles,
       txc.currentTask,
       txc.currentRequest,
       txj.targetTable,
       txj.targeTop,
       txj.targetUuid,
       txj.targetDelta
    from base.tx_journal txj
    left join base.tx_context txc using (txId)
    order by txc.txtimestamp;
--//

-- ============================================================================
--changeset michael.hoennig:audit-TX-JOURNAL-TRIGGER runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Trigger function for transaction audit journal.
 */
create or replace function base.tx_journal_trigger()
    returns trigger
    language plpgsql as $$
declare
    curTask text;
    curTxId xid8;
    tableSchemaAndName text;
    next_timestamp timestamp;
begin
    curTask := base.currentTask();
    curTxId := pg_current_xact_id();
    tableSchemaAndName := base.combine_table_schema_and_name(tg_table_schema, tg_table_name);

    next_timestamp = '1970-01-01';
    insert
        into base.tx_context (txId, txTimestamp, currentSubject, assumedRoles, currentTask, currentRequest)
            values ( curTxId, now(),
                     base.currentSubject(), base.assumedRoles(), curTask, base.currentRequest())
        on conflict do nothing
        returning txTimestamp into next_timestamp;

    -- only if a row was inserted, a notification should be send
    if next_timestamp <> '1970-01-01' then
        PERFORM pg_notify ('tx_context_inserted', CONCAT(curTxId, ',', extract(epoch from next_timestamp), ',', curTask));
    end if;

    case tg_op
        when 'INSERT' then insert
                               into base.tx_journal
                               values (curTxId, tableSchemaAndName,
                                       new.uuid, tg_op::base.tx_operation,
                                       to_jsonb(new));
        when 'UPDATE' then insert
                               into base.tx_journal
                               values (curTxId, tableSchemaAndName,
                                       old.uuid, tg_op::base.tx_operation,
                                       base.jsonb_changes_delta(to_jsonb(old), to_jsonb(new)));
        when 'DELETE' then insert
                               into base.tx_journal
                               values (curTxId,tableSchemaAndName,
                                       old.uuid, 'DELETE'::base.tx_operation,
                                       null::jsonb);
        else raise exception 'Trigger op % not supported for %.', tg_op, tableSchemaAndName;
        end case;
    return null;
end; $$;
--//

-- ============================================================================
--changeset michael.hoennig:audit-CREATE-JOURNAL-LOG endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Trigger function for transaction audit journal.
 */

create or replace procedure base.create_journal(targetTable varchar)
    language plpgsql as $$
declare
    createTriggerSQL varchar;
begin
    targetTable := lower(targetTable);

    -- "-0-" to put the trigger execution before any alphabetically greater tx-triggers
    createTriggerSQL = 'CREATE TRIGGER tx_0_journal_tg' ||
                       ' AFTER INSERT OR UPDATE OR DELETE ON ' || targetTable ||
                       '   FOR EACH ROW EXECUTE PROCEDURE base.tx_journal_trigger()';
    execute createTriggerSQL;
end; $$;
--//
