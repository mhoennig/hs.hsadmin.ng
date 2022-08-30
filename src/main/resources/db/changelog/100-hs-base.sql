--liquibase formatted sql

-- ============================================================================
--changeset hs-base-GLOBAL-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
  A single row to be referenced as a global object.
 */
begin transaction;
    call defineContext('initializing table "global"', null, null, null);
    insert
        into RbacObject (objecttable) values ('global');
    insert
        into Global (uuid, name) values ((select uuid from RbacObject where objectTable = 'global'), 'hostsharing');
commit;
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

begin transaction;
    call defineContext('creating Hostsharing admin role', null, null, null);
    select createRole(hostsharingAdmin());
commit;

-- ============================================================================
--changeset hs-base-ADMIN-USERS:1 context:dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Create two users and assign both to the administrators role.
 */
do language plpgsql $$
    declare
        admins uuid ;
    begin
        call defineContext('creating fake Hostsharing admin users', null, null, null);

        admins = findRoleId(hostsharingAdmin());
        call grantRoleToUserUnchecked(admins, admins, createRbacUser('mike@hostsharing.net'));
        call grantRoleToUserUnchecked(admins, admins, createRbacUser('sven@hostsharing.net'));
    end;
$$;
--//


-- ============================================================================
--changeset hs-base-hostsharing-TEST:1 context:dev,tc runAlways:true endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Tests if currentUserUuid() can fetch the user from the session variable.
 */

do language plpgsql $$
    declare
        userName varchar;
    begin
        call defineContext('testing currentUserUuid', null, 'sven@hostsharing.net', null);
        select userName from RbacUser where uuid = currentUserUuid() into userName;
        if userName <> 'sven@hostsharing.net' then
            raise exception 'setting or fetching initial currentUser failed, got: %', userName;
        end if;

        call defineContext('testing currentUserUuid', null, 'mike@hostsharing.net', null);
        select userName from RbacUser where uuid = currentUserUuid() into userName;
        if userName = 'mike@hostsharing.net' then
            raise exception 'currentUser should not change in one transaction, but did change, got: %', userName;
        end if;
    end; $$;
--//
