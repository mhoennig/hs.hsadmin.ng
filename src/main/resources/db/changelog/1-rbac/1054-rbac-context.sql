--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:rbac-context-DETERMINE runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function rbac.determineCurrentSubjectUuid(currentSubject varchar)
    returns uuid
    stable -- leakproof
    language plpgsql as $$
declare
    currentSubjectUuid uuid;
begin
    if currentSubject = '' then
        return null;
    end if;

    select uuid from rbac.subject where name = currentSubject into currentSubjectUuid;
    if currentSubjectUuid is null then
        raise exception '[401] subject % given in `base.defineContext(...)` does not exist', currentSubject;
    end if;
    return currentSubjectUuid;
end; $$;

create or replace function rbac.determineCurrentSubjectGroupUuids(currentSubjectGroups text)
    returns uuid[]
    stable -- leakproof
    language plpgsql as $$
declare
    groupSubjectUuids uuid[];
begin
    if length(coalesce(currentSubjectGroups, '')) = 0 then
        return array[]::uuid[];
    end if;

    select coalesce(array_agg(distinct s.uuid order by s.uuid), array[]::uuid[])
        from (
            select trim(groupName) as rawGroupName
                from unnest(string_to_array(currentSubjectGroups, ';')) groupName
                where length(trim(groupName)) > 0
        ) givenGroups
        cross join lateral (
            with recursive pathParts as (
                select
                    string_to_array(ltrim(givenGroups.rawGroupName, '/'), '/') as parts,
                    1 as partIndex
                union all
                select
                    parts,
                    partIndex + 1
                from pathParts
                where partIndex < array_length(parts, 1)
            )
            select '/' || array_to_string(parts[1:partIndex], '/') as name
                from pathParts
                where array_length(parts, 1) > 1
            union
            select givenGroups.rawGroupName
        ) groupNames
        join rbac.subject s
            on s.type = 'GROUP'::rbac.SubjectType
           and s.name = groupNames.name
        into groupSubjectUuids;

    return groupSubjectUuids;
end; $$;

drop function if exists rbac.determineCurrentSubjectOrAssumedRolesUuids(uuid, text);

create or replace function rbac.determinecurrentsubjectorassumedrolesuuids(effectiveSubjectUuids uuid[], assumedRoles text)
    returns uuid[]
    stable -- leakproof
    language plpgsql as $$
declare
    roleName            text;
    roleNameParts       text;
    objectTableToAssume varchar(63);
    objectNameToAssume  varchar(1024); -- e.g. for relation: 2*(96+48+48)+length('-with-REPRESENTATIVE-') = 405
    objectUuidToAssume  uuid;
    roleTypeToAssume    rbac.RoleType;
    roleIdsToAssume     uuid[];
    roleUuidToAssume    uuid;
begin
    if cardinality(effectiveSubjectUuids) = 0 then
        if length(coalesce(assumedRoles, '')) > 0 then
            raise exception '[403] undefined has no permission to assume role %', assumedRoles;
        else
            return array[]::uuid[];
        end if;
    end if;
    if  length(coalesce(assumedRoles, '')) = 0 then
        return effectiveSubjectUuids;
    end if;

    foreach roleName in array string_to_array(assumedRoles, ';')
        loop
            roleNameParts = overlay(roleName placing '#' from length(roleName) + 1 - strpos(reverse(roleName), ':'));
            objectTableToAssume = split_part(roleNameParts, '#', 1);
            objectNameToAssume = split_part(roleNameParts, '#', 2);
            roleTypeToAssume = split_part(roleNameParts, '#', 3);

            begin
                objectUuidToAssume = objectNameToAssume::uuid;
            exception when invalid_text_representation then
                objectUuidToAssume = rbac.findObjectUuidByIdName(objectTableToAssume, objectNameToAssume);
            end;

            if objectUuidToAssume is null then
                raise exception '[401] object % cannot be found in table % (from roleNameParts=%)', objectNameToAssume, objectTableToAssume, roleNameParts;
            end if;

            select uuid
                from rbac.role r
                where r.objectUuid = objectUuidToAssume
                  and r.roleType = roleTypeToAssume
                into roleUuidToAssume;
            if roleUuidToAssume is null then
                raise exception '[403] role % does not exist or is not accessible for subject %', roleName, base.currentSubject();
            end if;
            if not rbac.isGranted(effectiveSubjectUuids, roleUuidToAssume) then
                raise exception '[403] subject % has no permission to assume role %', base.currentSubject(), roleName;
            end if;
            roleIdsToAssume := roleIdsToAssume || roleUuidToAssume;
        end loop;

    return roleIdsToAssume;
end; $$;

-- ============================================================================
--changeset michael.hoennig:rbac-context-CONTEXT-DEFINED runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    This callback gets called after the context has been (re-) defined.
    This function will be overwritten by later changesets.
 */
drop procedure if exists base.contextDefined(varchar, text, varchar, varchar);

create or replace procedure base.contextDefined(
    currentTask varchar(127),
    currentRequest text,
    currentSubject varchar(63),
    assumedRoles varchar(4096),
    currentSubjectGroups text = null
)
    language plpgsql as $$
declare
    currentSubjectUuid uuid;
    currentSubjectGroupUuids uuid[];
    effectiveSubjectUuids uuid[];
    currentSubjectOrAssumedRolesUuids uuid[];
    globalAdminRoleUuid uuid;
    currentSubjectHasGlobalAdminRole boolean;
    currentSubjectHasEffectiveGlobalAdminRole boolean;
begin
    execute format('set local hsadminng.currentTask to %L', currentTask);

    execute format('set local hsadminng.currentRequest to %L', currentRequest);

    execute format('set local hsadminng.currentSubject to %L', currentSubject);
    select rbac.determineCurrentSubjectUuid(currentSubject) into currentSubjectUuid;
    execute format('set local hsadminng.currentSubjectUuid to %L', coalesce(currentSubjectUuid::text, ''));

    execute format('set local hsadminng.assumedRoles to %L', assumedRoles);
    execute format('set local hsadminng.currentSubjectGroups to %L', currentSubjectGroups);
    currentSubjectGroupUuids := rbac.determineCurrentSubjectGroupUuids(currentSubjectGroups);
    effectiveSubjectUuids := case
        when currentSubjectUuid is null then currentSubjectGroupUuids
        else array[currentSubjectUuid] || currentSubjectGroupUuids
    end;
    currentSubjectOrAssumedRolesUuids := rbac.determineCurrentSubjectOrAssumedRolesUuids(effectiveSubjectUuids, assumedRoles);
    execute format('set local hsadminng.currentSubjectOrAssumedRolesUuids to %L',
       (select array_to_string(currentSubjectOrAssumedRolesUuids, ';')));

    if cardinality(effectiveSubjectUuids) = 0 then
        currentSubjectHasGlobalAdminRole := false;
    else
        globalAdminRoleUuid := rbac.findRoleId(rbac.global_ADMIN());
        currentSubjectHasGlobalAdminRole := rbac.isGranted(effectiveSubjectUuids, globalAdminRoleUuid);
    end if;
    execute format('set local hsadminng.isGlobalAdmin to %L', currentSubjectHasGlobalAdminRole::text);

    if currentSubjectHasGlobalAdminRole then
        currentSubjectHasEffectiveGlobalAdminRole := trim(coalesce(assumedRoles, '')) = ''
            or globalAdminRoleUuid = any(currentSubjectOrAssumedRolesUuids);
    else
        currentSubjectHasEffectiveGlobalAdminRole := false;
    end if;
    execute format('set local hsadminng.hasGlobalAdminRole to %L', currentSubjectHasEffectiveGlobalAdminRole::text);

    raise notice 'Context defined as: %, %, %, [%]', currentTask, currentRequest, currentSubject, assumedRoles;
end; $$;

drop procedure if exists base.defineContext(varchar, text, varchar, text);

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


-- ============================================================================
--changeset michael.hoennig:rbac-context-current-subject-ID endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the uuid of the current subject as set via `base.defineContext(...)`.
 */

create or replace function rbac.currentSubjectUuid()
    returns uuid
    stable -- leakproof
    language plpgsql as $$
declare
    currentSubjectUuid text;
    currentSubjectName text;
begin
    begin
        currentSubjectUuid := current_setting('hsadminng.currentSubjectUuid');
    exception
        when others then
            currentSubjectUuid := null;
    end;
    if (currentSubjectUuid is null or currentSubjectUuid = '') then
        currentSubjectName := base.currentSubject();
        if (length(currentSubjectName) > 0) then
            raise exception '[401] currentSubjectUuid cannot be determined, unknown subject name "%"', currentSubjectName;
        else
            raise exception '[401] currentSubjectUuid cannot be determined, please call `base.defineContext(...)` first;"';
        end if;
    end if;
    return currentSubjectUuid::uuid;
end; $$;
--//

-- ============================================================================
--changeset michael.hoennig:rbac-context-CURRENT-SUBJECT-UUIDS endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns the uuid of the current subject as set via `base.defineContext(...)`,
    or, if any, the uuids of all assumed roles as set via `base.defineContext(...)`
    or empty array, if context is not defined.
 */
create or replace function rbac.currentSubjectOrAssumedRolesUuids()
    returns uuid[]
    stable -- leakproof
    language plpgsql as $$
declare
    currentSubjectOrAssumedRolesUuids text;
    currentSubjectName text;
begin
    begin
        currentSubjectOrAssumedRolesUuids := current_setting('hsadminng.currentSubjectOrAssumedRolesUuids');
    exception
        when others then
            currentSubjectOrAssumedRolesUuids := null;
    end;
    if (currentSubjectOrAssumedRolesUuids is null or length(currentSubjectOrAssumedRolesUuids) = 0 ) then
        currentSubjectName := base.currentSubject();
        if (length(currentSubjectName) > 0) then
            raise exception '[401] currentSubjectOrAssumedRolesUuids (%) cannot be determined, unknown subject name "%"', currentSubjectOrAssumedRolesUuids, currentSubjectName;
        else
            raise exception '[401] currentSubjectOrAssumedRolesUuids cannot be determined, please call `base.defineContext(...)` with a valid subject;"';
        end if;
    end if;
    return string_to_array(currentSubjectOrAssumedRolesUuids, ';');
end; $$;
--//
