--liquibase formatted sql

-- ============================================================================
--changeset rbac-context-CURRENT-USER-ID:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the id of the current user as set by `hsadminng.currentUser`.
    Raises exception if not set.
 */

create or replace function currentUserUuid()
    returns uuid
    stable leakproof
    language plpgsql as $$
declare
    currentUser   varchar(63);
    currentUserUuid uuid;
begin
    currentUser := currentUser();
    currentUserUuid = (select uuid from RbacUser where name = currentUser);
    if currentUserUuid is null then
        raise exception '[401] hsadminng.currentUser defined as %, but does not exists', currentUser;
    end if;
    return currentUserUuid;
end; $$;
--//

-- ============================================================================
--changeset rbac-context-CURRENT-SUBJECT-IDS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns id of current user as set in `hsadminng.currentUser`
    or, if any, ids of assumed role names as set in `hsadminng.assumedRoles`
    or empty array, if not set.
 */
create or replace function currentSubjectsUuids()
    returns uuid[]
    stable leakproof
    language plpgsql as $$
declare
    currentUserUuid       uuid;
    roleNames           varchar(63)[];
    roleName            varchar(63);
    objectTableToAssume varchar(63);
    objectNameToAssume  varchar(63);
    objectUuidToAssume  uuid;
    roleTypeToAssume    RbacRoleType;
    roleIdsToAssume     uuid[];
    roleUuidToAssume    uuid;
begin
    currentUserUuid := currentUserUuid();
    if currentUserUuid is null then
        raise exception '[401] user % does not exist', currentUser();
    end if;

    roleNames := assumedRoles();
    if cardinality(roleNames) = 0 then
        return array [currentUserUuid];
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
            if (not isGranted(currentUserUuid, roleUuidToAssume)) then
                raise exception '[403] user % (%) has no permission to assume role % (%)', currentUser(), currentUserUuid, roleName, roleUuidToAssume;
            end if;
            roleIdsToAssume := roleIdsToAssume || roleUuidToAssume;
        end loop;

    return roleIdsToAssume;
end; $$;
--//

