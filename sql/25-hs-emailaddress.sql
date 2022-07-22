
-- ========================================================
-- EMailAddress example with RBAC
-- --------------------------------------------------------

SET SESSION SESSION AUTHORIZATION DEFAULT ;

CREATE TABLE IF NOT EXISTS EMailAddress (
    uuid uuid UNIQUE REFERENCES RbacObject(uuid),
    localPart character varying(64),
    domainUuid uuid REFERENCES domain(uuid)
);

DROP TRIGGER IF EXISTS createRbacObjectForEMailAddress_Trigger ON EMailAddress;
CREATE TRIGGER createRbacObjectForEMailAddress_Trigger
    BEFORE INSERT ON EMailAddress
    FOR EACH ROW EXECUTE PROCEDURE createRbacObject();

CREATE OR REPLACE FUNCTION createRbacRulesForEMailAddress()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    eMailAddress             varchar;
    parentDomain             domain;
    eMailAddressOwnerRoleId  uuid;
    eMailAddressTenantRoleId uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    SELECT * FROM domain WHERE uuid=NEW.domainUuid into parentDomain;
    eMailAddress = NEW.localPart || '@' || parentDomain.name;

    -- an owner role is created and assigned to the domain owner
    eMailAddressOwnerRoleId = getRoleId('emailaddress#'||eMailAddress||'.owner', 'create');
    call grantRoleToRole(eMailAddressOwnerRoleId, getRoleId('domain#'||parentDomain.name||'.owner', 'fail'));
    -- ... and permissions for all ops are assigned
    call grantPermissionsToRole(eMailAddressOwnerRoleId,
                                createPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']));

    -- a tenant role is created and assigned to a user with the new email address
    eMailAddressTenantRoleId = getRoleId('emailaddress#'||eMailAddress||'.tenant', 'create');
    call grantRoleToUser(eMailAddressTenantRoleId, getRbacUserId(eMailAddress, 'create'));
    -- ... and permissions for all ops are assigned
    call grantPermissionsToRole(eMailAddressTenantRoleId,
                                createPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*'])); -- TODO '*' -> 'edit', 'view'

    RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS createRbacRulesForEMailAddress_Trigger ON EMailAddress;
CREATE TRIGGER createRbacRulesForEMailAddress_Trigger
    AFTER INSERT ON EMailAddress
    FOR EACH ROW EXECUTE PROCEDURE createRbacRulesForEMailAddress();

-- TODO: CREATE OR REPLACE FUNCTION deleteRbacRulesForEMailAddress()


-- create RBAC restricted view

abort;
set session authorization default ;
START TRANSACTION;
    SET LOCAL hsadminng.currentUser = 'mike@hostsharing.net';
    SET LOCAL hsadminng.assumedRoles = 'customer#bbb.owner;customer#bbc.owner';
    -- SET LOCAL hsadminng.assumedRoles = 'package#bbb00.owner;package#bbb01.owner';

    select count(*) from queryAccessibleObjectUuidsOfSubjectIds( 'view', currentSubjectIds(), 7) as a
        join rbacobject as o on a=o.uuid;

    /* SELECT DISTINCT target.*
        FROM EMailAddress AS target
             JOIN queryAccessibleObjectUuidsOfSubjectIds( 'view', currentSubjectIds()) AS allowedObjId
                  ON target.uuid = allowedObjId;*/
END TRANSACTION;

SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE EMailAddress ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS EMailAddress_rv;
CREATE OR REPLACE VIEW EMailAddress_rv AS
    SELECT DISTINCT target.*
    FROM EMailAddress AS target
    JOIN queryAccessibleObjectUuidsOfSubjectIds( 'view', currentSubjectIds()) AS allowedObjId
         ON target.uuid = allowedObjId;
GRANT ALL PRIVILEGES ON EMailAddress_rv TO restricted;


-- generate EMailAddress test data

DO LANGUAGE plpgsql $$
    DECLARE
        pac package;
        uu unixuser;
        dom domain;
        pacAdmin varchar;
        currentTask varchar;
    BEGIN
        SET hsadminng.currentUser TO '';

        FOR dom IN (SELECT * FROM domain) LOOP
            FOR t IN 0..5 LOOP
                currentTask = 'creating RBAC test EMailAddress #' || t || ' for Domain ' || dom.name;
                RAISE NOTICE 'task: %', currentTask;

                SELECT * FROM unixuser WHERE uuid=dom.unixuserUuid INTO uu;
                SELECT * FROM package WHERE uuid=uu.packageUuid INTO pac;
                pacAdmin = 'admin@' || pac.name || '.example.com';
                SET LOCAL hsadminng.currentUser TO pacAdmin;
                SET LOCAL hsadminng.assumedRoles = '';
                SET LOCAL hsadminng.currentTask TO currentTask;

                INSERT INTO EMailAddress (localPart, domainUuid)
                    VALUES ('local' || t, dom.uuid);

                COMMIT;
            END LOOP;
        END LOOP;
    END;
$$;


