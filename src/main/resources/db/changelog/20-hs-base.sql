create table Hostsharing
(
    uuid uuid primary key references RbacObject (uuid)
);
create unique index Hostsharing_Singleton on Hostsharing ((0));


insert
into RbacObject (objecttable) values ('hostsharing');
insert
    into Hostsharing (uuid) values ((select uuid from RbacObject where objectTable = 'hostsharing'));

create or replace function hostsharingAdmin()
    returns RbacRoleDescriptor
    returns null on null input
    stable leakproof
    language sql as $$
select 'global', (select uuid from RbacObject where objectTable = 'hostsharing'), 'admin'::RbacRoleType;
$$;

-- create administrators role with two assigned users
do language plpgsql $$
    declare
        admins uuid ;
    begin
        admins = createRole(hostsharingAdmin());
        call grantRoleToUser(admins, createRbacUser('mike@hostsharing.net'));
        call grantRoleToUser(admins, createRbacUser('sven@hostsharing.net'));
        commit;
    end;
$$;


begin transaction;
set local hsadminng.currentUser = 'mike@hostsharing.net';
select * from RbacUser where uuid = currentUserId();
end transaction;
