--liquibase formatted sql

-- ============================================================================
--changeset audit-OPERATION-TYPE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A type representing a DML operation.
 */
do $$
    begin
        if not exists(select 1 from pg_type where typname = 'operation') then
            create type "operation" as enum ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE');
        end if;
        --more types here...
    end $$;
--//

-- ============================================================================
--changeset audit-TX-CONTEXT-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A table storing transactions with context data.
 */
create table tx_context
(
    contextId       bigint primary key not null,
    txId            bigint             not null,
    txTimestamp     timestamp          not null,
    currentUser     varchar(63)        not null, -- not the uuid, because users can be deleted
    assumedRoles    varchar            not null, -- not the uuids, because roles can be deleted
    currentTask     varchar(96)        not null,
    currentRequest  varchar(512)       not null
);

create index on tx_context using brin (txTimestamp);
--//

-- ============================================================================
--changeset audit-TX-JOURNAL-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A table storing the transaction audit journal for all target tables it's configured for.
 */
create table tx_journal
(
    contextId   bigint    not null references tx_context (contextId),
    targetTable text      not null,
    targetUuid  uuid      not null, -- Assumes that all audited tables have a uuid column.
    targetOp    operation not null,
    targetDelta jsonb
);

create index on tx_journal (targetTable, targetUuid);
--//

-- ============================================================================
--changeset audit-TX-JOURNAL-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A view combining tx_journal with tx_context.
 */
create view tx_journal_v as
select txc.*, txj.targettable, txj.targetop, txj.targetuuid, txj.targetdelta
    from tx_journal txj
    left join tx_context txc using (contextid)
    order by txc.txtimestamp;
--//

-- ============================================================================
--changeset audit-TX-JOURNAL-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Trigger function for transaction audit journal.
 */
create or replace function tx_journal_trigger()
    returns trigger
    language plpgsql as $$
declare
    curTask text;
    curContextId bigint;
begin
    curTask := currentTask();
    curContextId := txid_current()+bigIntHash(curTask);

    insert
        into tx_context (contextId, txId, txTimestamp, currentUser, assumedRoles, currentTask, currentRequest)
        values (curContextId, txid_current(), now(),
                currentUser(), assumedRoles(), curTask, currentRequest())
        on conflict do nothing;

    case tg_op
        when 'INSERT' then insert
                               into tx_journal
                               values (curContextId,
                                       tg_table_name, new.uuid, tg_op::operation,
                                       to_jsonb(new));
        when 'UPDATE' then insert
                               into tx_journal
                               values (curContextId,
                                       tg_table_name, old.uuid, tg_op::operation,
                                       jsonb_changes_delta(to_jsonb(old), to_jsonb(new)));
        when 'DELETE' then insert
                               into tx_journal
                               values (curContextId,
                                       tg_table_name, old.uuid, 'DELETE'::operation,
                                       null::jsonb);
        else raise exception 'Trigger op % not supported for %.', tg_op, tg_table_name;
        end case;
    return null;
end; $$;
--//

-- ============================================================================
--changeset audit-CREATE-JOURNAL-LOG:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Trigger function for transaction audit journal.
 */

create or replace procedure create_journal(targetTable varchar)
    language plpgsql as $$
declare
    createTriggerSQL varchar;
begin
    targetTable := lower(targetTable);

    createTriggerSQL = 'CREATE TRIGGER ' || targetTable || '_journal' ||
                       ' AFTER INSERT OR UPDATE OR DELETE ON ' || targetTable ||
                       '   FOR EACH ROW EXECUTE PROCEDURE tx_journal_trigger()';
    execute createTriggerSQL;
end; $$;
--//
