
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

CREATE OR REPLACE FUNCTION createRbacRulesForDomain()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    parentUser        unixuser;
    domainOwnerRoleId uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    SELECT * FROM unixuser WHERE uuid=NEW.unixUserUuid into parentUser;

    -- an owner role is created and assigned to the unix user admin
    RAISE NOTICE 'creating domain owner role: %', 'domain#'||NEW.name||'.owner';
    domainOwnerRoleId = getRoleId('domain#'||NEW.name||'.owner', 'create');
        call grantRoleToRole(domainOwnerRoleId, getRoleId('unixuser#'||parentUser.name||'.admin', 'fail'));
    -- ... and permissions for all ops are assigned
    call grantPermissionsToRole(domainOwnerRoleId,
                                createPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']));

    RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS createRbacRulesForDomain_Trigger ON Domain;
CREATE TRIGGER createRbacRulesForDomain_Trigger
    AFTER INSERT ON Domain
    FOR EACH ROW EXECUTE PROCEDURE createRbacRulesForDomain();

-- TODO: CREATE OR REPLACE FUNCTION deleteRbacRulesForDomain()


-- create RBAC restricted view

SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE Domain ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS domain_rv;
CREATE OR REPLACE VIEW domain_rv AS
    SELECT DISTINCT target.*
      FROM Domain AS target
      JOIN queryAccessibleObjectUuidsOfSubjectIds( 'view', currentSubjectIds()) AS allowedObjId
           ON target.uuid = allowedObjId;
GRANT ALL PRIVILEGES ON domain_rv TO restricted;


-- generate Domain test data

DO LANGUAGE plpgsql $$
    DECLARE
        uu unixuser;
        pac package;
        pacAdmin varchar;
        currentTask varchar;
    BEGIN
        SET hsadminng.currentUser TO '';

        FOR uu IN (SELECT * FROM unixuser) LOOP
            IF ( random() < 0.3 ) THEN
                FOR t IN 0..2 LOOP
                    currentTask = 'creating RBAC test Domain #' || t || ' for UnixUser ' || uu.name|| ' #' || uu.uuid;
                    RAISE NOTICE 'task: %', currentTask;

                    SELECT * FROM package WHERE uuid=uu.packageUuid INTO pac;
                    pacAdmin = 'admin@' || pac.name || '.example.com';
                    SET LOCAL hsadminng.currentUser TO pacAdmin;
                    SET LOCAL hsadminng.assumedRoles = '';
                    SET LOCAL hsadminng.currentTask TO currentTask;

                    INSERT INTO Domain (name, unixUserUuid)
                        VALUES ('dom-' || t || '.' || pac.name || '.example.org' , uu.uuid);

                    COMMIT;
                END LOOP;
            END IF;
        END LOOP;

    END;
$$;


