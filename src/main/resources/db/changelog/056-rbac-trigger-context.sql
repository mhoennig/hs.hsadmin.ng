--liquibase formatted sql


-- ============================================================================
--changeset rbac-trigger-context-ENTER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure enterTriggerForObjectUuid(currentObjectUuid uuid)
    language plpgsql as $$
declare
    existingObjectUuid text;
begin
    existingObjectUuid = current_setting('hsadminng.currentObjectUuid', true);
    if (existingObjectUuid > '' ) then
        raise exception '[500] currentObjectUuid already defined, already in trigger of "%"', existingObjectUuid;
    end if;
    execute format('set local hsadminng.currentObjectUuid to %L', currentObjectUuid);
end; $$;


-- ============================================================================
--changeset rbac-trigger-context-CURRENT-ID:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the uuid of the object uuid whose trigger is currently executed as set via `enterTriggerForObjectUuid(...)`.
 */

create or replace function currentTriggerObjectUuid()
    returns uuid
    stable -- leakproof
    language plpgsql as $$
declare
    currentObjectUuid uuid;
begin
    begin
        currentObjectUuid = current_setting('hsadminng.currentObjectUuid')::uuid;
        return currentObjectUuid;
    exception
        when others then
            return null::uuid;
    end;
end; $$;
--//


-- ============================================================================
--changeset rbac-trigger-context-LEAVE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure leaveTriggerForObjectUuid(currentObjectUuid uuid)
    language plpgsql as $$
declare
    existingObjectUuid uuid;
begin
    existingObjectUuid = current_setting('hsadminng.currentObjectUuid', true);
    if ( existingObjectUuid <> currentObjectUuid ) then
        raise exception '[500] currentObjectUuid does not match: "%"', existingObjectUuid;
    end if;
    execute format('reset hsadminng.currentObjectUuid');
end; $$;

