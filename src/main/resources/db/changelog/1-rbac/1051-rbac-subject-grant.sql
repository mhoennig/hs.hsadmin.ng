--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:rbac-user-grant-GRANT-ROLE-TO-USER endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function rbac.assumedRoleUuid()
    returns uuid
    stable -- leakproof
    language plpgsql as $$
declare
    currentSubjectOrAssumedRolesUuids uuid[];
begin
    -- exactly one role must be assumed, not none not more than one
    if cardinality(base.assumedRoles()) <> 1 then
        raise exception '[400] Granting roles to user is only possible if exactly one role is assumed, given: %', base.assumedRoles();
    end if;

    currentSubjectOrAssumedRolesUuids := rbac.currentSubjectOrAssumedRolesUuids();
    return currentSubjectOrAssumedRolesUuids[1];
end; $$;

create or replace procedure rbac.grantRoleToSubjectUnchecked(grantedByRoleUuid uuid, grantedRoleUuid uuid, subjectUuid uuid, doAssume boolean = true)
    language plpgsql as $$
begin
    perform rbac.assertReferenceType('grantingRoleUuid', grantedByRoleUuid, 'rbac.role');
    perform rbac.assertReferenceType('roleId (descendant)', grantedRoleUuid, 'rbac.role');
    perform rbac.assertReferenceType('subjectUuid (ascendant)', subjectUuid, 'rbac.subject');

    insert
        into rbac.grants (grantedByRoleUuid, ascendantUuid, descendantUuid, assumed)
        values (grantedByRoleUuid, subjectUuid, grantedRoleUuid, doAssume)
    -- TODO: check if grantedByRoleUuid+doAssume are the same, otherwise raise exception?
    on conflict do nothing; -- allow granting multiple times
end; $$;

create or replace procedure rbac.grantRoleToSubject(grantedByRoleUuid uuid, grantedRoleUuid uuid, subjectUuid uuid, doAssume boolean = true)
    language plpgsql as $$
declare
    grantedByRoleIdName text;
    grantedRoleIdName text;
begin
    perform rbac.assertReferenceType('grantingRoleUuid', grantedByRoleUuid, 'rbac.role');
    perform rbac.assertReferenceType('grantedRoleUuid (descendant)', grantedRoleUuid, 'rbac.role');
    perform rbac.assertReferenceType('subjectUuid (ascendant)', subjectUuid, 'rbac.subject');

    assert grantedByRoleUuid is not null, 'grantedByRoleUuid must not be null';
    assert grantedRoleUuid is not null, 'grantedRoleUuid must not be null';
    assert subjectUuid is not null, 'subjectUuid must not be null';

    if NOT rbac.isGranted(rbac.currentSubjectOrAssumedRolesUuids(), grantedByRoleUuid) then
        select roleIdName from rbac.role_ev where uuid=grantedByRoleUuid into grantedByRoleIdName;
        raise exception '[403] Access to granted-by-role % (%) forbidden for % (%)',
                grantedByRoleIdName, grantedByRoleUuid, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
    end if;
    if NOT rbac.isGranted(grantedByRoleUuid, grantedRoleUuid) then
        select roleIdName from rbac.role_ev where uuid=grantedByRoleUuid into grantedByRoleIdName;
        select roleIdName from rbac.role_ev where uuid=grantedRoleUuid into grantedRoleIdName;
        raise exception '[403] Access to granted role % (%) forbidden for % (%)',
                grantedRoleIdName, grantedRoleUuid, grantedByRoleIdName, grantedByRoleUuid;
    end if;

    insert
        into rbac.grants (grantedByRoleUuid, ascendantUuid, descendantUuid, assumed)
        values (grantedByRoleUuid, subjectUuid, grantedRoleUuid, doAssume);
    -- TODO.impl: What should happen on multiple grants? What if options (doAssume) are not the same?
    --      Most powerful or latest grant wins? What about managed?
    -- on conflict do nothing; -- allow granting multiple times
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-user-grant-REVOKE-ROLE-FROM-USER endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure rbac.checkRevokeRoleFromSubjectPreconditions(grantedByRoleUuid uuid, grantedRoleUuid uuid, subjectUuid uuid)
    language plpgsql as $$
begin
    perform rbac.assertReferenceType('grantedByRoleUuid', grantedByRoleUuid, 'rbac.role');
    perform rbac.assertReferenceType('grantedRoleUuid (descendant)', grantedRoleUuid, 'rbac.role');
    perform rbac.assertReferenceType('subjectUuid (ascendant)', subjectUuid, 'rbac.subject');

    if NOT rbac.isGranted(rbac.currentSubjectOrAssumedRolesUuids(), grantedByRoleUuid) then
        raise exception '[403] Revoking role created by % is forbidden for %.', grantedByRoleUuid, base.currentSubjects();
    end if;

    if NOT rbac.isGranted(grantedByRoleUuid, grantedRoleUuid) then
        raise exception '[403] Revoking role % is forbidden for %.', grantedRoleUuid, base.currentSubjects();
    end if;

    --raise exception 'rbac.isGranted(%, %)', rbac.currentSubjectOrAssumedRolesUuids(), grantedByRoleUuid;
    if NOT rbac.isGranted(rbac.currentSubjectOrAssumedRolesUuids(), grantedByRoleUuid) then
        raise exception '[403] Revoking role granted by % is forbidden for %.', grantedByRoleUuid, base.currentSubjects();
    end if;

    if NOT rbac.isGranted(subjectUuid, grantedRoleUuid) then
        raise exception '[404] No such grant found granted by % for subject % to role %.', grantedByRoleUuid, subjectUuid, grantedRoleUuid;
    end if;
end; $$;

create or replace procedure rbac.revokeRoleFromSubject(grantedByRoleUuid uuid, grantedRoleUuid uuid, subjectUuid uuid)
    language plpgsql as $$
begin
    call rbac.checkRevokeRoleFromSubjectPreconditions(grantedByRoleUuid, grantedRoleUuid, subjectUuid);

    raise INFO 'delete from rbac.grants where ascendantUuid = % and descendantUuid = %', subjectUuid, grantedRoleUuid;
    delete from rbac.grants as g
       where g.ascendantUuid = subjectUuid and g.descendantUuid = grantedRoleUuid
         and g.grantedByRoleUuid = revokeRoleFromSubject.grantedByRoleUuid;
end; $$;
--//

-- ============================================================================
--changeset michael.hoennig:rbac-user-grant-REVOKE-PERMISSION-FROM-ROLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure rbac.revokePermissionFromRole(permissionUuid uuid, superRoleUuid uuid)
    language plpgsql as $$
begin
    raise INFO 'delete from rbac.grants where ascendantUuid = % and descendantUuid = %', superRoleUuid, permissionUuid;
    delete from rbac.grants as g
           where g.ascendantUuid = superRoleUuid and g.descendantUuid = permissionUuid;
end; $$;
--//
