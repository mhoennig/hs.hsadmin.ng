--liquibase formatted sql


-- ============================================================================
--changeset rbac-context-DETERMINE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function determineCurrentUserUuid(currentUser varchar)
    returns uuid
    stable leakproof
    language plpgsql as $$
declare
    currentUserUuid uuid;
begin
    if currentUser = '' then
        return null;
    end if;

    select uuid from RbacUser where name = currentUser into currentUserUuid;
    if currentUserUuid is null then
        raise exception '[401] user % given in `defineContext(...)` does not exist', currentUser;
    end if;
    return currentUserUuid;
end; $$;

create or replace function determineCurrentSubjectsUuids(currentUserUuid uuid, assumedRoles varchar)
    returns uuid[]
    stable leakproof
    language plpgsql as $$
declare
    roleName            varchar(63);
    roleNameParts       varchar(63);
    objectTableToAssume varchar(63);
    objectNameToAssume  varchar(63);
    objectUuidToAssume  uuid;
    roleTypeToAssume    RbacRoleType;
    roleIdsToAssume     uuid[];
    roleUuidToAssume    uuid;
begin
    if currentUserUuid is null then
        if length(coalesce(assumedRoles, '')) > 0 then
            raise exception '[403] undefined has no permission to assume role %', assumedRoles;
        else
            return array[]::uuid[];
        end if;
    end if;
    if  length(coalesce(assumedRoles, '')) = 0 then
        return array [currentUserUuid];
    end if;

    foreach roleName in array string_to_array(assumedRoles, ';')
        loop
            roleNameParts = overlay(roleName placing '#' from length(roleName) + 1 - strpos(reverse(roleName), '.'));
            objectTableToAssume = split_part(roleNameParts, '#', 1);
            objectNameToAssume = split_part(roleNameParts, '#', 2);
            roleTypeToAssume = split_part(roleNameParts, '#', 3);

            objectUuidToAssume = findObjectUuidByIdName(objectTableToAssume, objectNameToAssume);

            select uuid as roleuuidToAssume
                from RbacRole r
                where r.objectUuid = objectUuidToAssume
                  and r.roleType = roleTypeToAssume
                into roleUuidToAssume;
            if roleUuidToAssume is null then
                raise exception '[403] role % not accessible for user %', roleName, currentSubjects();
            end if;
            if not isGranted(currentUserUuid, roleUuidToAssume) then
                raise exception '[403] user % has no permission to assume role %', currentUser(), roleName;
            end if;
            roleIdsToAssume := roleIdsToAssume || roleUuidToAssume;
        end loop;

    return roleIdsToAssume;
end; $$;

-- ============================================================================
--changeset rbac-context-CONTEXT-DEFINED:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Callback which is called after the context has been (re-) defined.
    This function will be overwritten by later changesets.
 */
create or replace procedure contextDefined(
    currentTask varchar,
    currentRequest varchar,
    currentUser varchar,
    assumedRoles varchar
)
    language plpgsql as $$
declare
    currentUserUuid uuid;
begin
    execute format('set local hsadminng.currentTask to %L', currentTask);

    execute format('set local hsadminng.currentRequest to %L', currentRequest);

    execute format('set local hsadminng.currentUser to %L', currentUser);
    select determineCurrentUserUuid(currentUser) into currentUserUuid;
    execute format('set local hsadminng.currentUserUuid to %L', coalesce(currentUserUuid::text, ''));

    execute format('set local hsadminng.assumedRoles to %L', assumedRoles);
    execute format('set local hsadminng.currentSubjectsUuids to %L',
       (select array_to_string(determinecurrentSubjectsUuids(currentUserUuid, assumedRoles), ';')));

    raise notice 'Context defined as: %, %, %, [%]', currentTask, currentRequest, currentUser, assumedRoles;
end; $$;


-- ============================================================================
--changeset rbac-context-CURRENT-USER-ID:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the uuid of the current user as set via `defineContext(...)`.
 */

create or replace function currentUserUuid()
    returns uuid
    stable leakproof
    language plpgsql as $$
declare
    currentUserUuid text;
    currentUserName text;
begin
    begin
        currentUserUuid := current_setting('hsadminng.currentUserUuid');
    exception
        when others then
            currentUserUuid := null;
    end;
    if (currentUserUuid is null or currentUserUuid = '') then
        currentUserName := currentUser();
        if (length(currentUserName) > 0) then
            raise exception '[401] currentUserUuid cannot be determined, unknown user name "%"', currentUserName;
        else
            raise exception '[401] currentUserUuid cannot be determined, please call `defineContext(...)` first;"';
        end if;
    end if;
    return currentUserUuid::uuid;
end; $$;
--//

-- ============================================================================
--changeset rbac-context-CURRENT-SUBJECT-UUIDS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the uuid of the current user as set via `defineContext(...)`,
    or, if any, the uuids of all assumed roles as set via `defineContext(...)`
    or empty array, if context is not defined.
 */
create or replace function currentSubjectsUuids()
    returns uuid[]
    stable leakproof
    language plpgsql as $$
declare
    currentSubjectsUuids text;
    currentUserName text;
begin
    begin
        currentSubjectsUuids := current_setting('hsadminng.currentSubjectsUuids');
    exception
        when others then
            currentSubjectsUuids := null;
    end;
    if (currentSubjectsUuids is null or length(currentSubjectsUuids) = 0 ) then
        currentUserName := currentUser();
        if (length(currentUserName) > 0) then
            raise exception '[401] currentSubjectsUuids (%) cannot be determined, unknown user name "%"', currentSubjectsUuids, currentUserName;
        else
            raise exception '[401] currentSubjectsUuids cannot be determined, please call `defineContext(...)` with a valid user;"';
        end if;
    end if;
    return string_to_array(currentSubjectsUuids, ';');
end; $$;
--//

