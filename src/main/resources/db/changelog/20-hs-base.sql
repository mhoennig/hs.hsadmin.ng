


CREATE TABLE Hostsharing
(
    uuid uuid PRIMARY KEY REFERENCES RbacObject(uuid)
);
CREATE UNIQUE INDEX Hostsharing_Singleton ON Hostsharing ((0));


INSERT INTO RbacObject (objecttable) VALUES ('hostsharing');
INSERT INTO Hostsharing (uuid) VALUES ((SELECT uuid FROM RbacObject WHERE objectTable='hostsharing'));

CREATE OR REPLACE FUNCTION hostsharingAdmin()
    RETURNS RbacRoleDescriptor
    RETURNS NULL ON NULL INPUT
    STABLE LEAKPROOF
    LANGUAGE sql AS $$
SELECT 'global', (SELECT uuid FROM RbacObject WHERE objectTable='hostsharing'), 'admin'::RbacRoleType;
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


BEGIN TRANSACTION;
SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
select * from RbacUser where uuid=currentUserId();
END TRANSACTION;
