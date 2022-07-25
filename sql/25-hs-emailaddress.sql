
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

CREATE OR REPLACE FUNCTION emailAddressOwner(emailAddress varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('emailaddress', emailAddress, 'owner');
end; $$;

CREATE OR REPLACE FUNCTION emailAddressAdmin(emailAddress varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('emailaddress', emailAddress, 'admin');
end; $$;

CREATE OR REPLACE FUNCTION createRbacRulesForEMailAddress()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    parentDomain              record;
    eMailAddress              varchar;
    eMailAddressOwnerRoleUuid uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    SELECT d.name as name, u.name as unixUserName FROM domain d
             LEFT JOIN unixuser u ON u.uuid = d.unixuseruuid
             WHERE d.uuid=NEW.domainUuid into parentDomain;
    eMailAddress = NEW.localPart || '@' || parentDomain.name;

    -- an owner role is created and assigned to the domains's admin group
    eMailAddressOwnerRoleUuid = createRole(
        emailAddressOwner(eMailAddress),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']),
        beneathRole(domainAdmin( parentDomain.unixUserName, parentDomain.name))
        );

    -- and an admin role is created and assigned to the unixuser owner as well
    perform createRole(
        emailAddressAdmin(eMailAddress),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['edit']),
        beneathRole(eMailAddressOwnerRoleUuid),
        beingItselfA(domainTenant(parentDomain.unixUserName, parentDomain.name))
        );

    RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS createRbacRulesForEMailAddress_Trigger ON EMailAddress;
CREATE TRIGGER createRbacRulesForEMailAddress_Trigger
    AFTER INSERT ON EMailAddress
    FOR EACH ROW EXECUTE PROCEDURE createRbacRulesForEMailAddress();

-- TODO: CREATE OR REPLACE FUNCTION deleteRbacRulesForEMailAddress()


-- create RBAC restricted view

SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE EMailAddress ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS EMailAddress_rv;
CREATE OR REPLACE VIEW EMailAddress_rv AS
    SELECT DISTINCT target.*
    FROM EMailAddress AS target
    JOIN queryAccessibleObjectUuidsOfSubjectIds( 'view', 'emailaddress', currentSubjectIds()) AS allowedObjId
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
            FOR t IN 0..4 LOOP
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


