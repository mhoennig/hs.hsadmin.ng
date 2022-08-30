--liquibase formatted sql

-- ============================================================================
--changeset rbac-user-grant-GRANT-ROLE-TO-USER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function assumedRoleUuid()
    returns uuid
    stable leakproof
    language plpgsql as $$
declare
    currentSubjectUuids uuid[];
begin
    -- exactly one role must be assumed, not none not more than one
    if cardinality(assumedRoles()) <> 1 then
        raise exception '[400] Granting roles to user is only possible if exactly one role is assumed, given: %', assumedRoles();
    end if;

    currentSubjectUuids := currentSubjectsUuids();
    return currentSubjectUuids[1];
end; $$;

create or replace procedure grantRoleToUserUnchecked(grantedByRoleUuid uuid, roleUuid uuid, userUuid uuid, doAssume boolean = true)
    language plpgsql as $$
begin
    perform assertReferenceType('grantingRoleUuid', grantedByRoleUuid, 'RbacRole');
    perform assertReferenceType('roleId (descendant)', roleUuid, 'RbacRole');
    perform assertReferenceType('userId (ascendant)', userUuid, 'RbacUser');

    insert
        into RbacGrants (grantedByRoleUuid, ascendantUuid, descendantUuid, assumed)
        values (grantedByRoleUuid, userUuid, roleUuid, doAssume);
    -- TODO: What should happen on mupltiple grants? What if options are not the same?
    --      Most powerful or latest grant wins? What about managed?
    -- on conflict do nothing; -- allow granting multiple times
end; $$;

create or replace procedure grantRoleToUser(grantedByRoleUuid uuid, grantedRoleUuid uuid, userUuid uuid, doAssume boolean = true)
    language plpgsql as $$
begin
    perform assertReferenceType('grantingRoleUuid', grantedByRoleUuid, 'RbacRole');
    perform assertReferenceType('grantedRoleUuid (descendant)', grantedRoleUuid, 'RbacRole');
    perform assertReferenceType('userUuid (ascendant)', userUuid, 'RbacUser');

    if NOT isGranted(currentSubjectsUuids(), grantedByRoleUuid) then
        raise exception '[403] Access to granted-by-role % forbidden for %', grantedByRoleUuid, currentSubjects();
    end if;

    if NOT isGranted(grantedByRoleUuid, grantedRoleUuid) then
        raise exception '[403] Access to granted role % forbidden for %', grantedRoleUuid, currentSubjects();
    end if;

    insert
        into RbacGrants (grantedByRoleUuid, ascendantUuid, descendantUuid, assumed)
        values (grantedByRoleUuid, userUuid, grantedRoleUuid, doAssume);
    -- TODO: What should happen on mupltiple grants? What if options are not the same?
    --      Most powerful or latest grant wins? What about managed?
    -- on conflict do nothing; -- allow granting multiple times
end; $$;
--//


-- ============================================================================
--changeset rbac-user-grant-REVOKE-ROLE-FROM-USER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure checkRevokeRoleFromUserPreconditions(grantedByRoleUuid uuid, grantedRoleUuid uuid, userUuid uuid)
    language plpgsql as $$
begin
    perform assertReferenceType('grantedByRoleUuid', grantedByRoleUuid, 'RbacRole');
    perform assertReferenceType('grantedRoleUuid (descendant)', grantedRoleUuid, 'RbacRole');
    perform assertReferenceType('userUuid (ascendant)', userUuid, 'RbacUser');

    if NOT isGranted(currentSubjectsUuids(), grantedByRoleUuid) then
        raise exception '[403] Revoking role created by % is forbidden for %.', grantedByRoleUuid, currentSubjects();
    end if;

    if NOT isGranted(grantedByRoleUuid, grantedRoleUuid) then
        raise exception '[403] Revoking role % is forbidden for %.', grantedRoleUuid, currentSubjects();
    end if;

    --raise exception 'isGranted(%, %)', currentSubjectsUuids(), grantedByRoleUuid;
    if NOT isGranted(currentSubjectsUuids(), grantedByRoleUuid) then
        raise exception '[403] Revoking role granted by % is forbidden for %.', grantedByRoleUuid, currentSubjects();
    end if;

    if NOT isGranted(userUuid, grantedRoleUuid) then
        raise exception '[404] No such grant found granted by % for user % to role %.', grantedByRoleUuid, userUuid, grantedRoleUuid;
    end if;
end; $$;

create or replace procedure revokeRoleFromUser(grantedByRoleUuid uuid, grantedRoleUuid uuid, userUuid uuid)
    language plpgsql as $$
begin
    call checkRevokeRoleFromUserPreconditions(grantedByRoleUuid, grantedRoleUuid, userUuid);

    raise INFO 'delete from RbacGrants where ascendantUuid = % and descendantUuid = %', userUuid, grantedRoleUuid;
    delete from RbacGrants as g
       where g.ascendantUuid = userUuid and g.descendantUuid = grantedRoleUuid
         and g.grantedByRoleUuid = revokeRoleFromUser.grantedByRoleUuid;
end; $$;
--/
