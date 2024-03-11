--liquibase formatted sql


-- ============================================================================
--changeset context-DEFINE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Callback which is called after the context has been (re-) defined.
    This function will be overwritten by later changesets.
 */
create procedure contextDefined(
    currentTask varchar,
    currentRequest varchar,
    currentUser varchar,
    assumedRoles varchar
)
    language plpgsql as $$
begin
end; $$;

/*
    Defines the transaction context.
 */
create or replace procedure defineContext(
    currentTask varchar(96),
    currentRequest text = null,
    currentUser varchar(63) = null,
    assumedRoles varchar(256) = null
)
    language plpgsql as $$
begin
    currentTask := coalesce(currentTask, '');
    assert length(currentTask) <= 96, FORMAT('currentTask must not be longer than 96 characters: "%s"', currentTask);
    assert length(currentTask) > 8, FORMAT('currentTask must be at least 8 characters long: "%s""', currentTask);
    execute format('set local hsadminng.currentTask to %L', currentTask);

    currentRequest := coalesce(currentRequest, '');
    execute format('set local hsadminng.currentRequest to %L', currentRequest);

    currentUser := coalesce(currentUser, '');
    assert length(currentUser) <= 63, FORMAT('currentUser must not be longer than 63 characters: "%s"', currentUser);
    execute format('set local hsadminng.currentUser to %L', currentUser);

    assumedRoles := coalesce(assumedRoles, '');
    assert length(assumedRoles) <= 256, FORMAT('assumedRoles must not be longer than 256 characters: "%s"', assumedRoles);
    execute format('set local hsadminng.assumedRoles to %L', assumedRoles);

    call contextDefined(currentTask, currentRequest, currentUser, assumedRoles);
end; $$;
--//


-- ============================================================================
--changeset context-CURRENT-TASK:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the current task as set by `hsadminng.currentTask`.
    Raises exception if not set.
 */
create or replace function currentTask()
    returns varchar(96)
    stable -- leakproof
    language plpgsql as $$
declare
    currentTask varchar(96);
begin
    begin
        currentTask := current_setting('hsadminng.currentTask');
    exception
        when others then
            currentTask := null;
    end;
    if (currentTask is null or currentTask = '') then
        raise exception '[401] currentTask must be defined, please call `defineContext(...)`';
    end if;
    return currentTask;
end; $$;
--//


-- ============================================================================
--changeset context-CURRENT-REQUEST:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the current http request as set via `defineContext(...)`.
    Raises exception if not set.
 */
create or replace function currentRequest()
    returns varchar(512)
    stable -- leakproof
    language plpgsql as $$
declare
    currentRequest varchar(512);
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
--changeset context-CURRENT-USER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the current user as defined by `defineContext(...)`.
 */
create or replace function currentUser()
    returns varchar(63)
    stable -- leakproof
    language plpgsql as $$
declare
    currentUser varchar(63);
begin
    begin
        currentUser := current_setting('hsadminng.currentUser');
    exception
        when others then
            currentUser := null;
    end;
    return currentUser;
end; $$;
--//

-- ============================================================================
--changeset context-ASSUMED-ROLES:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns assumed role names as set in `hsadminng.assumedRoles`
    or empty array, if not set.
 */
create or replace function assumedRoles()
    returns varchar(63)[]
    stable -- leakproof
    language plpgsql as $$
declare
    currentSubject varchar(63);
begin
    begin
        currentSubject := current_setting('hsadminng.assumedRoles');
    exception
        when others then
            return array []::varchar[];
    end;
    if (currentSubject = '') then
        return array []::varchar[];
    end if;
    return string_to_array(currentSubject, ';');
end; $$;

create or replace function cleanIdentifier(rawIdentifier varchar)
    returns varchar
    returns null on null input
    language plpgsql as $$
declare
    cleanIdentifier varchar;
begin
    cleanIdentifier := regexp_replace(rawIdentifier, '[^A-Za-z0-9\-._:]+', '', 'g');
    return cleanIdentifier;
end; $$;

create or replace function pureIdentifier(rawIdentifier varchar)
    returns varchar
    returns null on null input
    language plpgsql as $$
declare
    cleanIdentifier varchar;
begin
    cleanIdentifier := cleanIdentifier(rawIdentifier);
    if cleanIdentifier != rawIdentifier then
        raise exception 'identifier "%" contains invalid characters, maybe use "%"', rawIdentifier, cleanIdentifier;
    end if;
    return cleanIdentifier;
end; $$;

create or replace function findObjectUuidByIdName(objectTable varchar, objectIdName varchar)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    sql  varchar;
    uuid uuid;
begin
    objectTable := pureIdentifier(objectTable);
    objectIdName := pureIdentifier(objectIdName);
    sql := format('select * from %sUuidByIdName(%L);', objectTable, objectIdName);
    begin
        execute sql into uuid;
    exception
        when others then
            raise exception 'function %UuidByIdName(...) not found, add identity view support for table %', objectTable, objectTable;
    end;
    return uuid;
end ; $$;

create or replace function findIdNameByObjectUuid(objectTable varchar, objectUuid uuid)
    returns varchar
    returns null on null input
    language plpgsql as $$
declare
    sql    varchar;
    idName varchar;
begin
    objectTable := pureIdentifier(objectTable);
    sql := format('select * from %sIdNameByUuid(%L::uuid);', objectTable, objectUuid);
    begin
        execute sql into idName;
    exception
        when others then
            raise exception 'function %IdNameByUuid(...) not found, add identity view support for table %', objectTable, objectTable;
    end;
    return idName;
end ; $$;

create or replace function currentSubjects()
    returns varchar(63)[]
    stable -- leakproof
    language plpgsql as $$
declare
    assumedRoles varchar(63)[];
begin
    assumedRoles := assumedRoles();
    if array_length(assumedRoles, 1) > 0 then
        return assumedRoles();
    else
        return array [currentUser()]::varchar(63)[];
    end if;
end; $$;

create or replace function hasAssumedRole()
    returns boolean
    stable -- leakproof
    language plpgsql as $$
begin
    return array_length(assumedRoles(), 1) > 0;
end; $$;
--//

