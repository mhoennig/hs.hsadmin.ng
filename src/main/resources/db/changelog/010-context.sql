--liquibase formatted sql

-- ============================================================================
--changeset context-CURRENT-TASK:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the current tas as set by `hsadminng.currentTask`.
    Raises exception if not set.
 */
create or replace function currentTask()
    returns varchar(96)
    stable leakproof
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
        raise exception '[401] hsadminng.currentTask must be defined, please use "SET LOCAL ...;"';
    end if;
    raise debug 'currentTask: %', currentTask;
    return currentTask;
end; $$;
--//


-- ============================================================================
--changeset context-CURRENT-USER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the current user as set by `hsadminng.currentUser`.
    Raises exception if not set.
 */
create or replace function currentUser()
    returns varchar(63)
    stable leakproof
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
    if (currentUser is null or currentUser = '') then
        raise exception '[401] hsadminng.currentUser must be defined, please use "SET LOCAL ...;"';
    end if;
    raise debug 'currentUser: %', currentUser;
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
    stable leakproof
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

create or replace function pureIdentifier(rawIdentifier varchar)
    returns varchar
    returns null on null input
    language plpgsql as $$
begin
    return regexp_replace(rawIdentifier, '\W+', '');
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
        raise notice 'sql: %', sql;
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
    sql     varchar;
    idName varchar;
begin
    objectTable := pureIdentifier(objectTable);
    sql := format('select * from %sIdNameByUuid(%L::uuid);', objectTable, objectUuid);
    begin
        raise notice 'sql: %', sql;
        execute sql into idName;
    exception
        when others then
            raise exception 'function %IdNameByUuid(...) not found, add identity view support for table %', objectTable, objectTable;
    end;
    return idName;
end ; $$;

create or replace function currentSubjects()
    returns varchar(63)[]
    stable leakproof
    language plpgsql as $$
declare
    assumedRoles varchar(63)[];
begin
    assumedRoles := assumedRoles();
    if array_length(assumedRoles(), 1) > 0 then
        return assumedRoles();
    else
        return array [currentUser()]::varchar(63)[];
    end if;
end; $$;
--//

