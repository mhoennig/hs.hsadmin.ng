
-- create administrators role with two assigned users
do language plpgsql $$
    declare
        admins uuid ;
    begin
        admins = createRole('administrators');
        call grantRoleToUser(admins, createRbacUser('mike@hostsharing.net'));
        call grantRoleToUser(admins, createRbacUser('sven@hostsharing.net'));
        commit;
    end;
$$;


BEGIN TRANSACTION;
SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
select * from RbacUser where uuid=currentUserId();
END TRANSACTION;
