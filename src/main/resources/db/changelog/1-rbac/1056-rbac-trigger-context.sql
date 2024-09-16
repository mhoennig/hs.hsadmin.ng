--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:rbac-trigger-context-ENTER endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure rbac.enterTriggerForObjectUuid(currentObjectUuid uuid)
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
--changeset michael.hoennig:rbac-trigger-context-CURRENT-ID endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the uuid of the object uuid whose trigger is currently executed as set via `rbac.enterTriggerForObjectUuid(...)`.
 */

create or replace function rbac.currentTriggerObjectUuid()
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
--changeset michael.hoennig:rbac-trigger-context-LEAVE endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure rbac.leaveTriggerForObjectUuid(currentObjectUuid uuid)
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

