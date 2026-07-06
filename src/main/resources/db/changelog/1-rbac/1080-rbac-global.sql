--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:rbac-global-OBJECT runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------
/*
    The purpose of this table is provide root business objects
    which can be referenced from global roles.
    Without this table, these columns needed to be nullable and
    many queries would be more complicated.
    In production databases, there is only a single row in this table,
    in test stages, there can be one row for each test data realm.
 */
create table if not exists rbac.global
(
    uuid uuid primary key references rbac.object (uuid) on delete cascade,
    name varchar(63) unique
);
create unique index if not exists Global_Singleton on rbac.global ((0));

grant select on rbac.global to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-IS-GLOBAL-ADMIN runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ------------------------------------------------------------------
/*
    Returns true if the current subject itself has the rbac.global ADMIN role.

    This intentionally ignores any assumed role. A global admin who assumed a
    non-global role still remains a global admin subject, but does not currently
    act with global-admin permissions. Permission checks that need the effective
    assumed-role context should use rbac.hasGlobalAdminRole() instead.
 */
create or replace function rbac.isGlobalAdmin()
    returns boolean
    language plpgsql as $$
declare
    isGlobalAdmin text;
begin
    isGlobalAdmin := current_setting('hsadminng.isGlobalAdmin', true);
    if isGlobalAdmin is not null then
        return isGlobalAdmin::boolean;
    end if;

    raise exception '`hsadminng.isGlobalAdmin` should have been set by `rbac.defineContext()`';
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-HAS-GLOBAL-ADMIN-ROLE runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------
/*
    Returns true if the current effective RBAC context has effective global-admin
    permissions.

    A global admin has these permissions if no role is assumed, or if one of the
    assumed roles is rbac.global#global:ADMIN. If a global admin assumes only
    non-global roles, this returns false because the effective context is limited
    to that assumed role.
 */
create or replace function rbac.hasGlobalAdminRole()
    returns boolean
    stable -- leakproof
    language plpgsql as $$
declare
    hasGlobalAdminRole text;
begin
    hasGlobalAdminRole := current_setting('hsadminng.hasGlobalAdminRole', true);
    if hasGlobalAdminRole is not null then
        return hasGlobalAdminRole::boolean;
    end if;

    raise exception '`hsadminng.hasGlobalAdminRole` should have been set by `rbac.defineContext()`';
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-HAS-GLOBAL-PERMISSION endDelimiter:--//
-- ------------------------------------------------------------------

create or replace function rbac.hasGlobalPermission(op rbac.RbacOp)
    returns boolean
    language sql as
$$
    -- TODO.perf: this could to be optimized
select (select uuid from rbac.global) in
       (select rbac.queryAccessibleObjectUuidsOfSubjectIds(op, 'rbac.global', rbac.currentSubjectOrAssumedRolesUuids()));
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-IDENTITY-VIEW runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

/*
    Creates a view to the rbac.global object table which maps the identifying name to the objectUuid.
 */
create or replace view rbac.global_iv as
select target.uuid, target.name as idName
    from rbac.global as target;
grant all privileges on rbac.global_iv to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};

/*
    Returns the objectUuid for a given identifying name (in this case the idName).
 */
create or replace function rbac.global_uuid_by_id_name(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from rbac.global_iv iv where iv.idName = global_uuid_by_id_name.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the idName).
 */
create or replace function rbac.global_id_name_by_uuid(uuid uuid)
    returns varchar
    language sql
    strict as $$
select idName from rbac.global_iv iv where iv.uuid = global_id_name_by_uuid.uuid;
$$;
--//

--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:rbac-global-PSEUDO-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
  A single row to be referenced as a rbac.Global object.
 */
begin transaction;
call base.defineContext('initializing table "rbac.global"', null, null, null);
insert
    into rbac.object (objecttable) values ('rbac.global');
insert
    into rbac.global (uuid, name) values ((select uuid from rbac.object where objectTable = 'rbac.global'), 'global');
commit;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-ADMIN-ROLE endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A rbac.Global administrator role.
 */
create or replace function rbac.global_ADMIN(assumed boolean = true)
    returns rbac.RoleDescriptor
    returns null on null input
    stable -- leakproof
    language sql as $$
select 'rbac.global', (select uuid from rbac.object where objectTable = 'rbac.global'), 'ADMIN'::rbac.RoleType, assumed;
$$;

begin transaction;
    call base.defineContext('creating role:rbac.global#global:ADMIN', null, null, null);
    select rbac.createRole(rbac.global_ADMIN());
commit;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-GUEST-ROLE runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------
/*
    A rbac.Global guest role.
 */
create or replace function rbac.global_GUEST(assumed boolean = true)
    returns rbac.RoleDescriptor
    returns null on null input
    stable -- leakproof
    language sql as $$
select 'rbac.global', (select uuid from rbac.object where objectTable = 'rbac.global'), 'GUEST'::rbac.RoleType, assumed;
$$;

do language plpgsql $$
    begin
        call base.defineContext('creating role:rbac.global#global:guest', null, null, null);
        begin
            perform rbac.createRole(rbac.global_GUEST());
        exception
            when unique_violation then
                null; -- ignore if it already exists from prev execution of this changeset
        end;
    end;
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-ADMIN-USERS context:!without-test-data endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------
/*
    Create two users and assign both to the administrators' role.
 */
do language plpgsql $$
    declare
        admins uuid ;
    begin
        call base.defineContext('creating fake test-realm admin users', null, null, null);

        admins = rbac.findRoleId(rbac.global_ADMIN());
        call rbac.grantRoleToSubjectUnchecked(admins, admins, rbac.create_subject('hsh-alex_superuser'));
        call rbac.grantRoleToSubjectUnchecked(admins, admins, rbac.create_subject('hsh-fran_superuser'));
        perform rbac.create_subject('tst-drew_selfregistered');
        perform rbac.create_subject('tst-rene_selfregistered');
    end;
$$;
--//

-- ============================================================================
--changeset michael.hoennig:rbac-global-RENAME-SEED-SUBJECTS endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Renames the known global subjects from the old email-address format to the new
    realm-prefixed format. Safe to run on a fresh install (no rows match) or
    multiple times (already-renamed rows are not touched).
 */
do language plpgsql $$
    begin
        call base.defineContext('renaming seed subjects to realm-prefixed format', null, null, null);
        update rbac.subject set name = 'hsh-alex_superuser'      where name = 'superuser-alex@hostsharing.net';
        update rbac.subject set name = 'hsh-fran_superuser'      where name = 'superuser-fran@hostsharing.net';
        update rbac.subject set name = 'tst-drew_selfregistered' where name = 'selfregistered-user-drew@hostsharing.org';
        update rbac.subject set name = 'tst-rene_selfregistered' where name = 'selfregistered-test-user@hostsharing.org';
        update rbac.subject set name = 'hsh-import_superuser'    where name = 'import-superuser@hostsharing.net';
    end;
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-TEST-GROUPS context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating fake Assembly group subjects', null, null, null);
        perform rbac.create_subject('/hsh-Hostmasters', 'GROUP'::rbac.SubjectType);
        perform rbac.create_subject_if_not_exist('/hsh-Team', 'GROUP'::rbac.SubjectType);
        perform rbac.create_subject_if_not_exist('/hsh-Service', 'GROUP'::rbac.SubjectType);
        perform rbac.create_subject('/xyz-Team', 'GROUP'::rbac.SubjectType);
        perform rbac.create_subject('/xyz-Service', 'GROUP'::rbac.SubjectType);
    end;
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-TEST context:!without-test-data runAlways:true validCheckSum:ANY endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

/*
    Tests if rbac.currentSubjectUuid() can fetch the user from the session variable.
 */

do language plpgsql $$
    declare
        userName varchar;
    begin
        call base.defineContext('testing currentSubjectUuid', null, 'hsh-fran_superuser', null);
        select userName from rbac.subject where uuid = rbac.currentSubjectUuid() into userName;
        if userName <> 'hsh-fran_superuser' then
            raise exception 'setting or fetching initial currentSubject failed, got: %', userName;
        end if;

        call base.defineContext('testing currentSubjectUuid', null, 'hsh-alex_superuser', null);
        select userName from rbac.subject where uuid = rbac.currentSubjectUuid() into userName;
        if userName = 'hsh-alex_superuser' then
            raise exception 'currentSubject should not change in one transaction, but did change, got: %', userName;
        end if;
    end; $$;
--//
