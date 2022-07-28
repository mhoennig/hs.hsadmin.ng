
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

CREATE OR REPLACE FUNCTION emailAddressOwner(emAddr EMailAddress)
    RETURNS RbacRoleDescriptor
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
begin
    return roleDescriptor('emailaddress', emAddr.uuid, 'owner');
end; $$;

CREATE OR REPLACE FUNCTION emailAddressAdmin(emAddr EMailAddress)
    RETURNS RbacRoleDescriptor
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
begin
    return roleDescriptor('emailaddress', emAddr.uuid, 'admin');
end; $$;

CREATE OR REPLACE FUNCTION createRbacRulesForEMailAddress()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    parentDomain              Domain;
    eMailAddressOwnerRoleUuid uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    SELECT d.*
      FROM domain d
      LEFT JOIN unixuser u ON u.uuid = d.unixuseruuid
     WHERE d.uuid=NEW.domainUuid INTO parentDomain;

    -- an owner role is created and assigned to the domains's admin group
    eMailAddressOwnerRoleUuid = createRole(
        emailAddressOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']),
        beneathRole(domainAdmin( parentDomain))
        );

    -- and an admin role is created and assigned to the unixuser owner as well
    perform createRole(
        emailAddressAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['edit']),
        beneathRole(eMailAddressOwnerRoleUuid),
        beingItselfA(domainTenant(parentDomain))
        );

    RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS createRbacRulesForEMailAddress_Trigger ON EMailAddress;
CREATE TRIGGER createRbacRulesForEMailAddress_Trigger
    AFTER INSERT ON EMailAddress
    FOR EACH ROW EXECUTE PROCEDURE createRbacRulesForEMailAddress();

-- TODO: CREATE OR REPLACE FUNCTION deleteRbacRulesForEMailAddress()


-- create RBAC-restricted view
SET SESSION SESSION AUTHORIZATION DEFAULT;
-- ALTER TABLE EMailAddress ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS EMailAddress_rv;
CREATE OR REPLACE VIEW EMailAddress_rv AS
    SELECT DISTINCT target.*
    FROM EMailAddress AS target
    WHERE target.uuid IN (SELECT queryAccessibleObjectUuidsOfSubjectIds( 'view', 'emailaddress', currentSubjectIds()));
GRANT ALL PRIVILEGES ON EMailAddress_rv TO restricted;

-- generate EMailAddress test data

DO LANGUAGE plpgsql $$
    DECLARE
        dom record;
        pacAdmin varchar;
        currentTask varchar;
    BEGIN
        SET hsadminng.currentUser TO '';

        FOR dom IN (
            SELECT d.uuid, d.name, p.name as packageName
              FROM domain d
              JOIN unixuser u ON u.uuid = d.unixuseruuid
              JOIN package p ON u.packageuuid = p.uuid
              JOIN customer c ON p.customeruuid = c.uuid
             -- WHERE c.reference >= 18000
                  ) LOOP
            FOR t IN 0..4 LOOP
                currentTask = 'creating RBAC test EMailAddress #' || t || ' for Domain ' || dom.name;
                RAISE NOTICE 'task: %', currentTask;

                pacAdmin = 'admin@' || dom.packageName || '.example.com';
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


