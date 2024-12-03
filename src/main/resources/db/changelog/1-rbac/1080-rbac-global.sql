--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:rbac-global-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    The purpose of this table is provide root business objects
    which can be referenced from global roles.
    Without this table, these columns needed to be nullable and
    many queries would be more complicated.
    In production databases, there is only a single row in this table,
    in test stages, there can be one row for each test data realm.
 */
create table rbac.global
(
    uuid uuid primary key references rbac.object (uuid) on delete cascade,
    name varchar(63) unique
);
create unique index Global_Singleton on rbac.global ((0));

grant select on rbac.global to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-IS-GLOBAL-ADMIN endDelimiter:--//
-- ------------------------------------------------------------------

create or replace function rbac.isGlobalAdmin()
    returns boolean
    language plpgsql as $$
begin
    return rbac.isGranted(rbac.currentSubjectOrAssumedRolesUuids(), rbac.findRoleId(rbac.global_ADMIN()));
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-HAS-GLOBAL-ADMIN-ROLE endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns true if the current user is a global admin and has no assumed role.
 */
create or replace function rbac.hasGlobalAdminRole()
    returns boolean
    stable -- leakproof
    language plpgsql as $$
declare
    assumedRoles text;
begin
    begin
        assumedRoles := current_setting('hsadminng.assumedRoles');
    exception
        when others then
            assumedRoles := null;
    end;
    return TRIM(COALESCE(assumedRoles, '')) = '' and rbac.isGlobalAdmin();
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
--changeset michael.hoennig:rbac-global-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the rbac.global object table which maps the identifying name to the objectUuid.
 */
drop view if exists rbac.global_iv;
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
--changeset michael.hoennig:rbac-global-GUEST-ROLE endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A rbac.Global guest role.
 */
create or replace function rbac.globalglobalGuest(assumed boolean = true)
    returns rbac.RoleDescriptor
    returns null on null input
    stable -- leakproof
    language sql as $$
select 'rbac.global', (select uuid from rbac.object where objectTable = 'rbac.global'), 'GUEST'::rbac.RoleType, assumed;
$$;

begin transaction;
    call base.defineContext('creating role:rbac.global#global:guest', null, null, null);
    select rbac.createRole(rbac.globalglobalGuest());
commit;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-ADMIN-USERS context:dev,tc endDelimiter:--//
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
        call rbac.grantRoleToSubjectUnchecked(admins, admins, rbac.create_subject('superuser-alex@hostsharing.net'));
        call rbac.grantRoleToSubjectUnchecked(admins, admins, rbac.create_subject('superuser-fran@hostsharing.net'));
        perform rbac.create_subject('selfregistered-user-drew@hostsharing.org');
        perform rbac.create_subject('selfregistered-test-user@hostsharing.org');
    end;
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-global-TEST context:dev,tc runAlways:true endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Tests if rbac.currentSubjectUuid() can fetch the user from the session variable.
 */

do language plpgsql $$
    declare
        userName varchar;
    begin
        call base.defineContext('testing currentSubjectUuid', null, 'superuser-fran@hostsharing.net', null);
        select userName from rbac.subject where uuid = rbac.currentSubjectUuid() into userName;
        if userName <> 'superuser-fran@hostsharing.net' then
            raise exception 'setting or fetching initial currentSubject failed, got: %', userName;
        end if;

        call base.defineContext('testing currentSubjectUuid', null, 'superuser-alex@hostsharing.net', null);
        select userName from rbac.subject where uuid = rbac.currentSubjectUuid() into userName;
        if userName = 'superuser-alex@hostsharing.net' then
            raise exception 'currentSubject should not change in one transaction, but did change, got: %', userName;
        end if;
    end; $$;
--//
