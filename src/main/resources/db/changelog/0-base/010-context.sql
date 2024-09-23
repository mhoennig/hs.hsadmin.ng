--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:context-DEFINE endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Callback which is called after the context has been (re-) defined.
    This function will be overwritten by later changesets.
 */
create procedure base.contextDefined(
    currentTask varchar(127),
    currentRequest text,
    currentSubject varchar(63),
    assumedRoles varchar(1023)
)
    language plpgsql as $$
begin
end; $$;

/*
    Defines the transaction context.
 */
create or replace procedure base.defineContext(
    currentTask varchar(127),
    currentRequest text = null,
    currentSubject varchar(63) = null,
    assumedRoles varchar(1023) = null
)
    language plpgsql as $$
begin
    currentTask := coalesce(currentTask, '');
    assert length(currentTask) <= 127, FORMAT('currentTask must not be longer than 127 characters: "%s"', currentTask);
    assert length(currentTask) >= 12, FORMAT('currentTask must be at least 12 characters long: "%s""', currentTask);
    execute format('set local hsadminng.currentTask to %L', currentTask);

    currentRequest := coalesce(currentRequest, '');
    execute format('set local hsadminng.currentRequest to %L', currentRequest);

    currentSubject := coalesce(currentSubject, '');
    assert length(currentSubject) <= 63, FORMAT('currentSubject must not be longer than 63 characters: "%s"', currentSubject);
    execute format('set local hsadminng.currentSubject to %L', currentSubject);

    assumedRoles := coalesce(assumedRoles, '');
    assert length(assumedRoles) <= 1023, FORMAT('assumedRoles must not be longer than 1023 characters: "%s"', assumedRoles);
    execute format('set local hsadminng.assumedRoles to %L', assumedRoles);

    call base.contextDefined(currentTask, currentRequest, currentSubject, assumedRoles);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:context-CURRENT-TASK endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the current task as set by `hsadminng.currentTask`.
    Raises exception if not set.
 */
create or replace function base.currentTask()
    returns varchar(127)
    stable -- leakproof
    language plpgsql as $$
declare
    currentTask varchar(127);
begin
    begin
        currentTask := current_setting('hsadminng.currentTask');
    exception
        when others then
            currentTask := null;
    end;
    if (currentTask is null or currentTask = '') then
        raise exception '[401] currentTask must be defined, please call `base.defineContext(...)`';
    end if;
    return currentTask;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:context-CURRENT-REQUEST endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the current http request as set via `base.defineContext(...)`.
    Raises exception if not set.
 */
create or replace function base.currentRequest()
    returns text
    stable -- leakproof
    language plpgsql as $$
declare
    currentRequest text;
begin
    begin
        currentRequest := current_setting('hsadminng.currentRequest');
    exception
        when others then
            currentRequest := null;
    end;
    return currentRequest;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:context-current-subject endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the current user as defined by `base.defineContext(...)`.
 */
create or replace function base.currentSubject()
    returns varchar(63)
    stable -- leakproof
    language plpgsql as $$
declare
    currentSubject varchar(63);
begin
    begin
        currentSubject := current_setting('hsadminng.currentSubject');
    exception
        when others then
            currentSubject := null;
    end;
    return currentSubject;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:context-base.ASSUMED-ROLES endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns assumed role names as set in `hsadminng.assumedRoles`
    or empty array, if not set.
 */
create or replace function base.assumedRoles()
    returns varchar(1023)[]
    stable -- leakproof
    language plpgsql as $$
begin
    return string_to_array(current_setting('hsadminng.assumedRoles', true), ';');
end; $$;

create or replace function base.cleanIdentifier(rawIdentifier varchar)
    returns varchar
    returns null on null input
    language plpgsql as $$
declare
    cleanIdentifier varchar;
begin
    cleanIdentifier := regexp_replace(rawIdentifier, '[^A-Za-z0-9\-._|]+', '', 'g');
    return cleanIdentifier;
end; $$;

create or replace function base.pureIdentifier(rawIdentifier varchar)
    returns varchar
    returns null on null input
    language plpgsql as $$
declare
    cleanIdentifier varchar;
begin
    cleanIdentifier := base.cleanIdentifier(rawIdentifier);
    if cleanIdentifier != rawIdentifier then
        raise exception 'identifier "%" contains invalid characters, maybe use "%"', rawIdentifier, cleanIdentifier;
    end if;
    return cleanIdentifier;
end; $$;

create or replace function base.currentSubjects()
    returns varchar(1023)[]
    stable -- leakproof
    language plpgsql as $$
declare
    assumedRoles varchar(1023)[];
begin
    assumedRoles := base.assumedRoles();
    if array_length(assumedRoles, 1) > 0 then
        return assumedRoles;
    else
        return array [base.currentSubject()]::varchar(1023)[];
    end if;
end; $$;

create or replace function base.hasAssumedRole()
    returns boolean
    stable -- leakproof
    language plpgsql as $$
begin
    return array_length(base.assumedRoles(), 1) > 0;
end; $$;
--//

