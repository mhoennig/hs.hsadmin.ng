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
--changeset audit-TABLE-TX-AUDIT-LOG:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A table storing the transaction audit log for all target tables.
 */
create table "tx_audit_log"
(
    txId         bigint    not null,
    txTimestamp  timestamp not null,
    currentUser  varchar(63) not null,  -- TODO.SPEC: Keep user name or uuid in audit-log?
    assumedRoles varchar   not null,    -- TODO.SPEC: Store role names or uuids in audit-log?
    currentTask  varchar   not null,
    targetTable  text      not null,
    targetUuid   uuid      not null,    -- TODO.SPEC: All audited tables have a uuid column.
    targetOp     operation not null,
    targetDelta  jsonb
);

create index on tx_audit_log using brin (txTimestamp);
create index on tx_audit_log (targetTable, targetUuid);
--//

-- ============================================================================
--changeset audit-TX-AUDIT-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Trigger function for transaction audit log.
 */
create or replace function tx_audit_log_trigger()
    returns trigger
    language plpgsql as $$
begin
    case tg_op
        when 'INSERT' then
            insert
                into tx_audit_log
                values (txid_current(), now(),
                        currentUser(), assumedRoles(), currentTask(),
                        tg_table_name, new.uuid, tg_op::operation,
                        to_jsonb(new));
        when 'UPDATE' then
            insert
                into tx_audit_log
                values (txid_current(), now(),
                        currentUser(), assumedRoles(), currentTask(),
                        tg_table_name, old.uuid, tg_op::operation,
                        jsonb_changes_delta(to_jsonb(old), to_jsonb(new)));
        when 'DELETE' then
            insert
                into tx_audit_log
                values (txid_current(), now(),
                        currentUser(), assumedRoles(), currentTask(),
                        tg_table_name, old.uuid, 'DELETE'::operation,
                        null::jsonb);
        else
            raise exception 'Trigger op % not supported for %.', tg_op, tg_table_name;
    end case;
    return null;
end; $$;
--//

-- ============================================================================
--changeset audit-CREATE-AUDIT-LOG:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Trigger function for transaction audit log.
 */

create or replace procedure create_audit_log(targetTable varchar)
    language plpgsql as $$
declare
    createTriggerSQL   varchar;
begin
    createTriggerSQL = 'CREATE TRIGGER ' || targetTable || '_audit_log' ||
                       ' AFTER INSERT OR UPDATE OR DELETE ON ' || targetTable ||
                       '   FOR EACH ROW EXECUTE PROCEDURE tx_audit_log_trigger()';
    raise notice 'sql: %', createTriggerSQL;
    execute createTriggerSQL;
end; $$;
--//
