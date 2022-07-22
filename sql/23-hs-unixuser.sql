
-- ========================================================
-- UnixUser example with RBAC
-- --------------------------------------------------------

SET SESSION SESSION AUTHORIZATION DEFAULT ;

CREATE TABLE IF NOT EXISTS UnixUser (
    uuid uuid UNIQUE REFERENCES RbacObject(uuid),
    name character varying(32),
    packageUuid uuid REFERENCES package(uuid)
);

DROP TRIGGER IF EXISTS createRbacObjectForUnixUser_Trigger ON UnixUser;
CREATE TRIGGER createRbacObjectForUnixUser_Trigger
    BEFORE INSERT ON UnixUser
    FOR EACH ROW EXECUTE PROCEDURE createRbacObject();

CREATE OR REPLACE FUNCTION createRbacRulesForUnixUser()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    parentPackage package;
    unixuserOwnerRoleId uuid;
    unixuserAdminRoleId uuid;
    unixuserTenantRoleId uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    SELECT * FROM package WHERE uuid=NEW.packageUuid into parentPackage;

    -- an owner role is created and assigned to the package owner group
    unixuserOwnerRoleId = createRole('unixuser#'||NEW.name||'.owner');
    call grantRoleToRole(unixuserOwnerRoleId, getRoleId('package#'||parentPackage.name||'.owner', 'fail'));
    -- ... and permissions for all ops are assigned
    call grantPermissionsToRole(unixuserOwnerRoleId,
                                createPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']));

    -- ... also a unixuser admin role is created and assigned to the unixuser owner as well
    unixuserAdminRoleId = createRole('unixuser#'||NEW.name||'.admin');
    call grantRoleToRole(unixuserAdminRoleId, unixuserOwnerRoleId);
    -- ... to which a permission with view operation is assigned
    call grantPermissionsToRole(unixuserAdminRoleId,
                                createPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['edit', 'add-domain']));

    -- ... also a unixuser tenant role is created and assigned to the unixuser admin
    unixuserTenantRoleId = createRole('unixuser#'||NEW.name||'.tenant');
    call grantRoleToRole(unixuserTenantRoleId, unixuserAdminRoleId);
    -- ... to which a permission with view operation is assigned
    call grantPermissionsToRole(unixuserTenantRoleId,
                                createPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['view']));

    RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS createRbacRulesForUnixUser_Trigger ON UnixUser;
CREATE TRIGGER createRbacRulesForUnixUser_Trigger
    AFTER INSERT ON UnixUser
    FOR EACH ROW EXECUTE PROCEDURE createRbacRulesForUnixUser();

-- TODO: CREATE OR REPLACE FUNCTION deleteRbacRulesForUnixUser()


-- create RBAC restricted view

SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE unixuser ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS unixuser_rv;
CREATE OR REPLACE VIEW unixuser_rv AS
    SELECT DISTINCT target.*
     FROM unixuser AS target
     JOIN queryAccessibleObjectUuidsOfSubjectIds( 'view', 'unixuser', currentSubjectIds()) AS allowedObjId
          ON target.uuid = allowedObjId;
GRANT ALL PRIVILEGES ON unixuser_rv TO restricted;


-- generate UnixUser test data

DO LANGUAGE plpgsql $$
    DECLARE
        pac package;
        pacAdmin varchar;
        currentTask varchar;
    BEGIN
        SET hsadminng.currentUser TO '';

        FOR pac IN (SELECT * FROM package) LOOP
            FOR t IN 0..9 LOOP
                currentTask = 'creating RBAC test unixuser #' || t || ' for package ' || pac.name|| ' #' || pac.uuid;
                RAISE NOTICE 'task: %', currentTask;
                pacAdmin = 'admin@' || pac.name || '.example.com';
                SET LOCAL hsadminng.currentUser TO 'mike@hostsharing.net'; -- TODO: use a package-admin
                SET LOCAL hsadminng.assumedRoles = '';
                SET LOCAL hsadminng.currentTask TO currentTask;

                INSERT INTO unixuser (name, packageUuid)
                VALUES (pac.name||'-'|| intToVarChar(t, 4), pac.uuid);

                COMMIT;
            END LOOP;
        END LOOP;

    END;
$$;
