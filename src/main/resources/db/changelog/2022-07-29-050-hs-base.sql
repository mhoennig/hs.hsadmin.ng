--liquibase formatted sql

-- ============================================================================
--changeset hs-base-GLOBAL-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    The purpose of this table is to contain a single row
    which can be referenced from global roles as an object.
    Otherwise these columns needed to be nullable and
    many queries would be more complicated.
 */
create table Global
(
    uuid uuid primary key references RbacObject (uuid),
    name varchar(63)
);
create unique index Global_Singleton on Global ((0));

/**
  A single row to be referenced as a global object.
 */
insert
    into RbacObject (objecttable) values ('global');
insert
    into Global (uuid, name) values ((select uuid from RbacObject where objectTable = 'global'), 'hostsharing');
--//

-- ============================================================================
--changeset hs-base-GLOBAL-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the global object table which maps the identifying name to the objectUuid.
 */
drop view if exists global_iv;
create or replace view global_iv as
select distinct target.uuid, target.name as idName
    from global as target;
grant all privileges on global_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case the prefix).
 */
create or replace function globalUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from global_iv iv where iv.idName = globalUuidByIdName.idName;
$$;
--//

-- ============================================================================
--changeset hs-base-ADMIN-ROLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    A global administrator role.
 */
create or replace function hostsharingAdmin()
    returns RbacRoleDescriptor
    returns null on null input
    stable leakproof
    language sql as $$
select 'global', (select uuid from RbacObject where objectTable = 'global'), 'admin'::RbacRoleType;
$$;
select createRole(hostsharingAdmin());

-- ============================================================================
--changeset hs-base-ADMIN-USERS:1 context:dev,test,tc endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Create two users and assign both to the administrators role.
 */
do language plpgsql $$
    declare
        admins uuid ;
    begin
        admins = findRoleId(hostsharingAdmin());
        call grantRoleToUser(admins, createRbacUser('mike@hostsharing.net'));
        call grantRoleToUser(admins, createRbacUser('sven@hostsharing.net'));
    end;
$$;
--//


-- ============================================================================
--changeset hs-base-hostsharing-TEST:1 context:dev,test,tc runAlways:true endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Tests if currentUserId() can fetch the user from the session variable.
 */

do language plpgsql $$
    declare
        userName varchar;
    begin
        set local hsadminng.currentUser = 'sven@hostsharing.net';
        select userName from RbacUser where uuid = currentUserId() into userName;
        if userName <> 'sven@hostsharing.net' then
            raise exception 'setting or fetching initial currentUser failed, got: %', userName;
        end if;

        set local hsadminng.currentUser = 'mike@hostsharing.net';
        select userName from RbacUser where uuid = currentUserId() into userName;
        if userName = 'mike@hostsharing.net' then
            raise exception 'currentUser should not change in one transaction, but did change, got: %', userName;
        end if;
    end; $$;
--//
