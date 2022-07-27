
-- ========================================================
-- Domain example with RBAC
-- --------------------------------------------------------

SET SESSION SESSION AUTHORIZATION DEFAULT ;

CREATE TABLE IF NOT EXISTS Domain (
    uuid uuid UNIQUE REFERENCES RbacObject(uuid),
    name character varying(32),
    unixUserUuid uuid REFERENCES unixuser(uuid)
);

DROP TRIGGER IF EXISTS createRbacObjectForDomain_Trigger ON Domain;
CREATE TRIGGER createRbacObjectForDomain_Trigger
    BEFORE INSERT ON Domain
    FOR EACH ROW EXECUTE PROCEDURE createRbacObject();

CREATE OR REPLACE FUNCTION domainOwner(unixUserName varchar, domainName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('domain', unixUserName || '/' || domainName, 'owner');
end; $$;

CREATE OR REPLACE FUNCTION domainAdmin(unixUserName varchar, domainName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('domain', unixUserName || '/' || domainName, 'admin');
end; $$;

CREATE OR REPLACE FUNCTION domainTenant(unixUserName varchar, domainName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('domain', unixUserName || '/' || domainName, 'tenant');
end; $$;


CREATE OR REPLACE FUNCTION createRbacRulesForDomain()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    parentUser unixuser;
    domainOwnerRoleUuid uuid;
    domainAdminRoleUuid uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    SELECT * FROM unixuser WHERE uuid=NEW.unixUserUuid into parentUser;

    -- a domain owner role is created and assigned to the unixuser's admin role
    domainOwnerRoleUuid = createRole(
        domainOwner(parentUser.name, NEW.name),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']),
        beneathRole(unixUserAdmin(parentUser.name))
        );

    -- a domain admin role is created and assigned to the domain's owner role
    domainAdminRoleUuid = createRole(
        domainAdmin(parentUser.name, NEW.name),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['edit', 'add-emailaddress']),
        beneathRole(domainOwnerRoleUuid)
        );

    -- and a domain tenant role is created and assigned to the domain's admiin role
    perform createRole(
        domainTenant(parentUser.name, NEW.name),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']),
        beneathRole(domainAdminRoleUuid),
        beingItselfA(createUnixUserTenantRoleIfNotExists(parentUser))
        );

    RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS createRbacRulesForDomain_Trigger ON Domain;
CREATE TRIGGER createRbacRulesForDomain_Trigger
    AFTER INSERT ON Domain
    FOR EACH ROW EXECUTE PROCEDURE createRbacRulesForDomain();

-- TODO: CREATE OR REPLACE FUNCTION deleteRbacRulesForDomain()


-- create RBAC-restricted view
SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE Domain ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS domain_rv;
CREATE OR REPLACE VIEW domain_rv AS
    SELECT DISTINCT target.*
      FROM Domain AS target
      WHERE target.uuid IN (SELECT queryAccessibleObjectUuidsOfSubjectIds( 'view', 'domain', currentSubjectIds()));
GRANT ALL PRIVILEGES ON domain_rv TO restricted;


-- generate Domain test data

DO LANGUAGE plpgsql $$
    DECLARE
        uu record;
        pac package;
        pacAdmin varchar;
        currentTask varchar;
    BEGIN
        SET hsadminng.currentUser TO '';

        FOR uu IN (
            SELECT u.uuid, u.name, u.packageuuid, c.reference
              FROM unixuser u
              JOIN package p ON u.packageuuid = p.uuid
              JOIN customer c ON p.customeruuid = c.uuid
             -- WHERE c.reference >= 18000
              ) LOOP
            IF ( random() < 0.3 ) THEN
                FOR t IN 0..1 LOOP
                    currentTask = 'creating RBAC test Domain #' || t || ' for UnixUser ' || uu.name|| ' #' || uu.uuid;
                    RAISE NOTICE 'task: %', currentTask;

                    SELECT * FROM package WHERE uuid=uu.packageUuid INTO pac;
                    pacAdmin = 'admin@' || pac.name || '.example.com';
                    SET LOCAL hsadminng.currentUser TO pacAdmin;
                    SET LOCAL hsadminng.assumedRoles = '';
                    SET LOCAL hsadminng.currentTask TO currentTask;

                    INSERT INTO Domain (name, unixUserUuid)
                        VALUES ('dom-' || t || '.' || uu.name || '.example.org' , uu.uuid);

                    COMMIT;
                END LOOP;
            END IF;
        END LOOP;

    END;
$$;


