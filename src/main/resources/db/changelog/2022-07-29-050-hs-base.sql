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
create table Hostsharing
(
    uuid uuid primary key references RbacObject (uuid)
);
create unique index Hostsharing_Singleton on Hostsharing ((0));

/**
  A single row to be referenced as a global object.
 */
insert
    into RbacObject (objecttable) values ('hostsharing');
insert
    into Hostsharing (uuid) values ((select uuid from RbacObject where objectTable = 'hostsharing'));
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
    select 'global', (select uuid from RbacObject where objectTable = 'hostsharing'), 'admin'::RbacRoleType;
$$;
select createRole(hostsharingAdmin());

-- ============================================================================
--changeset hs-base-ADMIN-USERS:1 context:dev endDelimiter:--//
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
--changeset hs-base-hostsharing-TEST:1 context:dev runAlways:true endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Tests if currentUserId() can fetch the user from the session variable.
 */

do language plpgsql $$
    declare
        userName varchar;
    begin
        set local hsadminng.currentUser = 'mike@hostsharing.net';
        select userName from RbacUser where uuid = currentUserId() into userName;
        if userName <> 'mike@hostsharing.net' then
            raise exception 'fetching initial currentUser failed';
        end if;

        set local hsadminng.currentUser = 'sven@hostsharing.net';
        select userName from RbacUser where uuid = currentUserId() into userName;
        if userName <> 'sven@hostsharing.net' then
            raise exception 'fetching changed currentUser failed';
        end if;
    end; $$;
--//
