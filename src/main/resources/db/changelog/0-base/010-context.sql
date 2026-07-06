--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:context-DEFINE runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

/*
    Callback, which is called after the context has been (re-) defined.
    This function will be overwritten by later changesets.

    To avoid that we do not temporarily have an empty context check during a DB schema migration,
    we do NOT use `create or replace` but ignore the duplicate-function error, in case the procedure already exists.
    When it gets overridden later with the actual implementation, it will be replaced with the new version.
 */
do $$
begin
    create procedure base.contextDefined(
        currentTask varchar(127),
        currentRequest text,
        currentSubject varchar(63),
        assumedRoles varchar(4096),
        currentSubjectGroups text = null
    )
        language plpgsql as $procedure$
    begin
    end; $procedure$;
exception
    when duplicate_function then
        null;
end; $$;

/*
    Defines the transaction context.
 */
create or replace procedure base.defineContext(
    currentTask varchar(127),
    currentRequest text = null,
    currentSubject varchar(63) = null,
    assumedRoles text = null,
    currentSubjectGroups text = null
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
    assert length(assumedRoles) <= 4096, FORMAT('assumedRoles must not be longer than 4096 characters: "%s"', assumedRoles);
    execute format('set local hsadminng.assumedRoles to %L', assumedRoles);

    currentSubjectGroups := coalesce(currentSubjectGroups, '');
    assert length(currentSubjectGroups) <= 4096, FORMAT('currentSubjectGroups must not be longer than 4096 characters: "%s"', currentSubjectGroups);
    execute format('set local hsadminng.currentSubjectGroups to %L', currentSubjectGroups);

    call base.contextDefined(currentTask, currentRequest, currentSubject, assumedRoles, currentSubjectGroups);
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
--changeset michael.hoennig:context-base.ASSUMED-ROLES runOnChange:true endDelimiter:--//
--validCheckSum: ANY
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
    immutable
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
    immutable
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
