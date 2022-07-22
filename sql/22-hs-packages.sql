
-- ========================================================
-- Package example with RBAC
-- --------------------------------------------------------

SET SESSION SESSION AUTHORIZATION DEFAULT ;

CREATE TABLE IF NOT EXISTS package (
    uuid uuid UNIQUE REFERENCES RbacObject(uuid),
    name character varying(5),
    customerUuid uuid REFERENCES customer(uuid)
);

DROP TRIGGER IF EXISTS createRbacObjectForPackage_Trigger ON package;
CREATE TRIGGER createRbacObjectForPackage_Trigger
    BEFORE INSERT ON package
    FOR EACH ROW EXECUTE PROCEDURE createRbacObject();

CREATE OR REPLACE FUNCTION createRbacRulesForPackage()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    parentCustomer customer;
    packageOwnerRoleId uuid;
    packageTenantRoleId uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    SELECT * FROM customer AS c WHERE c.uuid=NEW.customerUuid INTO parentCustomer;

    -- an owner role is created and assigned to the customer's admin group
    packageOwnerRoleId = createRole('package#'||NEW.name||'.owner');
    call grantRoleToRole(packageOwnerRoleId, getRoleId('customer#'||parentCustomer.prefix||'.admin', 'fail'));

    -- ... and permissions for all ops are assigned
    call grantPermissionsToRole(packageOwnerRoleId,
                                createPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']));

    -- ... also a package tenant role is created and assigned to the package owner as well
    packageTenantRoleId = createRole('package#'||NEW.name||'.tenant');
    call grantRoleToRole(packageTenantRoleId, packageOwnerRoleId);

    -- ... to which a permission with view operation is assigned
    call grantPermissionsToRole(packageTenantRoleId,
                                createPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['view']));

    RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS createRbacRulesForPackage_Trigger ON package;
CREATE TRIGGER createRbacRulesForPackage_Trigger
    AFTER INSERT ON package
    FOR EACH ROW EXECUTE PROCEDURE createRbacRulesForPackage();

CREATE OR REPLACE FUNCTION deleteRbacRulesForPackage()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        --  TODO
    ELSE
        RAISE EXCEPTION 'invalid usage of TRIGGER BEFORE DELETE';
    END IF;
END; $$;

DROP TRIGGER IF EXISTS deleteRbacRulesForPackage_Trigger ON customer;
CREATE TRIGGER deleteRbacRulesForPackage_Trigger
    BEFORE DELETE ON customer
    FOR EACH ROW EXECUTE PROCEDURE deleteRbacRulesForPackage();

-- create RBAC restricted view

SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE package ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS package_rv;
CREATE OR REPLACE VIEW package_rv AS
    SELECT DISTINCT target.*
      FROM package AS target
     JOIN queryAccessibleObjectUuidsOfSubjectIds( 'view', currentSubjectIds()) AS allowedObjId
          ON target.uuid = allowedObjId;
GRANT ALL PRIVILEGES ON package_rv TO restricted;



-- generate Package test data

DO LANGUAGE plpgsql $$
    DECLARE
        cust customer;
        pacName varchar;
        currentTask varchar;
        custAdmin varchar;
    BEGIN
        SET hsadminng.currentUser TO '';

        FOR cust IN (SELECT * FROM customer) LOOP
            FOR t IN 0..9 LOOP
                pacName = cust.prefix || TO_CHAR(t, 'fm00');
                currentTask = 'creating RBAC test package #'|| pacName || ' for customer ' || cust.prefix || ' #' || cust.uuid;
                RAISE NOTICE 'task: %', currentTask;

                custAdmin = 'admin@' || cust.prefix || '.example.com';
                SET LOCAL hsadminng.currentUser TO custAdmin;
                SET LOCAL hsadminng.assumedRoles = '';
                SET LOCAL hsadminng.currentTask TO currentTask;

                insert into package (name, customerUuid)
                VALUES (pacName, cust.uuid);

                COMMIT;
            END LOOP;
        END LOOP;
    END;
$$;

