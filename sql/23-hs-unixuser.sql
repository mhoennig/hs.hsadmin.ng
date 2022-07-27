
-- ========================================================
-- UnixUser example with RBAC
-- --------------------------------------------------------

SET SESSION SESSION AUTHORIZATION DEFAULT ;

CREATE TABLE IF NOT EXISTS UnixUser (
    uuid uuid UNIQUE REFERENCES RbacObject(uuid),
    name character varying(32),
    comment character varying(96),
    packageUuid uuid REFERENCES package(uuid)
);

CREATE OR REPLACE FUNCTION unixUserOwner(unixUserName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('unixuser', unixUserName, 'owner');
end; $$;

CREATE OR REPLACE FUNCTION unixUserAdmin(unixUserName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('unixuser', unixUserName, 'admin');
end; $$;

CREATE OR REPLACE FUNCTION unixUserTenant(unixUserName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('unixuser', unixUserName, 'tenant');
end; $$;

CREATE OR REPLACE FUNCTION createUnixUserTenantRoleIfNotExists(unixUser UnixUser)
    RETURNS uuid
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
DECLARE
    unixUserTenantRoleName varchar;
    unixUserTenantRoleUuid uuid;
BEGIN
    unixUserTenantRoleName = unixUserTenant(unixUser.name);
    unixUserTenantRoleUuid = findRoleId(unixUserTenantRoleName);
    IF unixUserTenantRoleUuid IS NOT NULL THEN
        RETURN unixUserTenantRoleUuid;
    END IF;

    RETURN createRole(
        unixUserTenantRoleName,
        grantingPermissions(forObjectUuid => unixUser.uuid, permitOps => ARRAY['edit', 'add-domain']),
        beneathRole(unixUserAdmin(unixUser.name))
        );
END; $$;


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
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    SELECT * FROM package WHERE uuid=NEW.packageUuid into parentPackage;

    -- an owner role is created and assigned to the package's admin group
    unixuserOwnerRoleId = createRole(
        unixUserOwner(NEW.name),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']),
        beneathRole(packageAdmin(parentPackage.name))
        );

    -- and a unixuser admin role is created and assigned to the unixuser owner as well
    unixuserAdminRoleId = createRole(
        unixUserAdmin(NEW.name),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['edit', 'add-domain']),
        beneathRole(unixuserOwnerRoleId),
        beingItselfA(packageTenant(parentPackage.name))
        );

    -- a tenent role is only created on demand

    RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS createRbacRulesForUnixUser_Trigger ON UnixUser;
CREATE TRIGGER createRbacRulesForUnixUser_Trigger
    AFTER INSERT ON UnixUser
    FOR EACH ROW EXECUTE PROCEDURE createRbacRulesForUnixUser();

-- TODO: CREATE OR REPLACE FUNCTION deleteRbacRulesForUnixUser()


-- create RBAC-restricted view
SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE unixuser ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS unixuser_rv;
CREATE OR REPLACE VIEW unixuser_rv AS
    SELECT DISTINCT target.*
      FROM unixuser AS target
    WHERE target.uuid IN (SELECT queryAccessibleObjectUuidsOfSubjectIds( 'view', 'unixuser', currentSubjectIds()));
GRANT ALL PRIVILEGES ON unixuser_rv TO restricted;


-- generate UnixUser test data

DO LANGUAGE plpgsql $$
    DECLARE
        pac record;
        pacAdmin varchar;
        currentTask varchar;
    BEGIN
        SET hsadminng.currentUser TO '';

        FOR pac IN (
            SELECT p.uuid, p.name
              FROM package p
              JOIN customer c ON p.customeruuid = c.uuid
             -- WHERE c.reference >= 18000
            ) LOOP

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
