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
--changeset michael.hoennig:audit-TX-JOURNAL-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A view combining base.tx_journal with base.tx_context.
 */
create view base.tx_journal_v as
select txc.*, txj.targettable, txj.targetop, txj.targetuuid, txj.targetdelta
    from base.tx_journal txj
    left join base.tx_context txc using (txId)
    order by txc.txtimestamp;
--//

-- ============================================================================
--changeset michael.hoennig:audit-TX-JOURNAL-TRIGGER endDelimiter:--//
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
begin
    curTask := base.currentTask();
    curTxId := pg_current_xact_id();
    tableSchemaAndName := base.combine_table_schema_and_name(tg_table_schema, tg_table_name);

    insert
        into base.tx_context (txId, txTimestamp, currentSubject, assumedRoles, currentTask, currentRequest)
            values ( curTxId, now(),
                     base.currentSubject(), base.assumedRoles(), curTask, base.currentRequest())
        on conflict do nothing;

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
