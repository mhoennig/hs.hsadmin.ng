--liquibase formatted sql

-- ============================================================================
--changeset rbac-current-CURRENT-USER:1 endDelimiter:--//
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
    return currentUser;
end; $$;

create or replace function currentUserId()
    returns uuid
    stable leakproof
    language plpgsql as $$
declare
    currentUser   varchar(63);
    currentUserId uuid;
begin
    currentUser := currentUser();
    currentUserId = (select uuid from RbacUser where name = currentUser);
    if currentUserId is null then
        raise exception '[401] hsadminng.currentUser defined as %, but does not exists', currentUser;
    end if;
    return currentUserId;
end; $$;
--//

-- ============================================================================
--changeset rbac-current-ASSUMED-ROLES:1 endDelimiter:--//
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

create or replace function currentSubjectIds()
    returns uuid[]
    stable leakproof
    language plpgsql as $$
declare
    currentUserId       uuid;
    roleNames           varchar(63)[];
    roleName            varchar(63);
    objectTableToAssume varchar(63);
    objectNameToAssume  varchar(63);
    objectUuidToAssume  uuid;
    roleTypeToAssume    RbacRoleType;
    roleIdsToAssume     uuid[];
    roleUuidToAssume    uuid;
begin
    currentUserId := currentUserId();
    if currentUserId is null then
        raise exception '[401] user % does not exist', currentUser();
    end if;

    roleNames := assumedRoles();
    if cardinality(roleNames) = 0 then
        return array [currentUserId];
    end if;

    raise notice 'assuming roles: %', roleNames;

    foreach roleName in array roleNames
        loop
            roleName = overlay(roleName placing '#' from length(roleName) + 1 - strpos(reverse(roleName), '.'));
            objectTableToAssume = split_part(roleName, '#', 1);
            objectNameToAssume = split_part(roleName, '#', 2);
            roleTypeToAssume = split_part(roleName, '#', 3);

            objectUuidToAssume = findObjectUuidByIdName(objectTableToAssume, objectNameToAssume);

            -- TODO: either the result needs to be cached at least per transaction or we need to get rid of SELCT in a loop
            select uuid as roleuuidToAssume
                from RbacRole r
                where r.objectUuid = objectUuidToAssume
                  and r.roleType = roleTypeToAssume
                into roleUuidToAssume;
            if (not isGranted(currentUserId, roleUuidToAssume)) then
                raise exception '[403] user % (%) has no permission to assume role % (%)', currentUser(), currentUserId, roleName, roleUuidToAssume;
            end if;
            roleIdsToAssume := roleIdsToAssume || roleUuidToAssume;
        end loop;

    return roleIdsToAssume;
end; $$;
--//

