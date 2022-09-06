--liquibase formatted sql

-- ============================================================================
--changeset rbac-global-GLOBAL-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    The purpose of this table is provide root business objects
    which can be referenced from global roles.
    Without this table, these columns needed to be nullable and
    many queries would be more complicated.
    In production databases, there is only a single row in this table,
    in test stages, there can be one row for each test data realm.
 */
create table Global
(
    uuid uuid primary key references RbacObject (uuid) on delete cascade,
    name varchar(63) unique
);
create unique index Global_Singleton on Global ((0));

grant select on global to restricted;
--//


-- ============================================================================
--changeset rbac-global-HAS-GLOBAL-PERMISSION:1 endDelimiter:--//
-- ------------------------------------------------------------------

create or replace function hasGlobalPermission(op RbacOp)
    returns boolean
    language sql as
$$
    -- TODO: this could to be optimized
select (select uuid from global) in
       (select queryAccessibleObjectUuidsOfSubjectIds(op, 'global', currentSubjectsUuids()));
$$;
--//


-- ============================================================================
--changeset rbac-global-GLOBAL-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the global object table which maps the identifying name to the objectUuid.
 */
drop view if exists global_iv;
create or replace view global_iv as
select target.uuid, target.name as idName
    from global as target;
grant all privileges on global_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case the idName).
 */
create or replace function globalUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from global_iv iv where iv.idName = globalUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the idName).
 */
create or replace function globalIdNameByUuid(uuid uuid)
    returns varchar
    language sql
    strict as $$
select idName from global_iv iv where iv.uuid = globalIdNameByUuid.uuid;
$$;
--//

--liquibase formatted sql

-- ============================================================================
--changeset rbac-global-PSEUDO-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
  A single row to be referenced as a global object.
 */
begin transaction;
call defineContext('initializing table "global"', null, null, null);
insert
    into RbacObject (objecttable) values ('global');
insert
    into Global (uuid, name) values ((select uuid from RbacObject where objectTable = 'global'), 'global');
commit;
--//


-- ============================================================================
--changeset rbac-global-ADMIN-ROLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A global administrator role.
 */
create or replace function globalAdmin()
    returns RbacRoleDescriptor
    returns null on null input
    stable leakproof
    language sql as $$
select 'global', (select uuid from RbacObject where objectTable = 'global'), 'admin'::RbacRoleType;
$$;

begin transaction;
call defineContext('creating global admin role', null, null, null);
select createRole(globalAdmin());
commit;

-- ============================================================================
--changeset rbac-global-ADMIN-USERS:1 context:dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Create two users and assign both to the administrators role.
 */
do language plpgsql $$
    declare
        admins uuid ;
    begin
        call defineContext('creating fake test-realm admin users', null, null, null);

        admins = findRoleId(globalAdmin());
        call grantRoleToUserUnchecked(admins, admins, createRbacUser('alex@hostsharing.net'));
        call grantRoleToUserUnchecked(admins, admins, createRbacUser('fran@hostsharing.net'));
    end;
$$;
--//


-- ============================================================================
--changeset rbac-global-TEST:1 context:dev,tc runAlways:true endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Tests if currentUserUuid() can fetch the user from the session variable.
 */

do language plpgsql $$
    declare
        userName varchar;
    begin
        call defineContext('testing currentUserUuid', null, 'fran@hostsharing.net', null);
        select userName from RbacUser where uuid = currentUserUuid() into userName;
        if userName <> 'fran@hostsharing.net' then
            raise exception 'setting or fetching initial currentUser failed, got: %', userName;
        end if;

        call defineContext('testing currentUserUuid', null, 'alex@hostsharing.net', null);
        select userName from RbacUser where uuid = currentUserUuid() into userName;
        if userName = 'alex@hostsharing.net' then
            raise exception 'currentUser should not change in one transaction, but did change, got: %', userName;
        end if;
    end; $$;
--//
