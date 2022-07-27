
-- ========================================================
-- Package example with RBAC
-- --------------------------------------------------------

SET SESSION SESSION AUTHORIZATION DEFAULT ;

CREATE TABLE IF NOT EXISTS package (
    uuid uuid UNIQUE REFERENCES RbacObject(uuid),
    name character varying(5),
    customerUuid uuid REFERENCES customer(uuid)
);

CREATE OR REPLACE FUNCTION packageOwner(packageName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('package', packageName, 'owner');
end; $$;

CREATE OR REPLACE FUNCTION packageAdmin(packageName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('package', packageName, 'admin');
end; $$;

CREATE OR REPLACE FUNCTION packageTenant(packageName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('package', packageName, 'tenant');
end; $$;


DROP TRIGGER IF EXISTS createRbacObjectForPackage_Trigger ON package;
CREATE TRIGGER createRbacObjectForPackage_Trigger
    BEFORE INSERT ON package
    FOR EACH ROW EXECUTE PROCEDURE createRbacObject();

CREATE OR REPLACE FUNCTION createRbacRulesForPackage()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    parentCustomer customer;
    packageOwnerRoleUuid uuid;
    packageAdminRoleUuid uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    SELECT * FROM customer AS c WHERE c.uuid=NEW.customerUuid INTO parentCustomer;

    -- an owner role is created and assigned to the customer's admin role
    packageOwnerRoleUuid = createRole(
        packageOwner(NEW.name),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']),
        beneathRole(customerAdmin(parentCustomer.prefix))
        );

    -- an owner role is created and assigned to the package owner role
    packageAdminRoleUuid = createRole(
        packageAdmin(NEW.name),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['edit', 'add-unixuser']),
        beneathRole(packageOwnerRoleUuid)
        );

    -- and a package tenant role is created and assigned to the package admin as well
    perform createRole(
        packageTenant(NEW.name),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY ['view']),
        beneathRole(packageAdminRoleUuid),
        beingItselfA(customerTenant(parentCustomer.prefix))
        );

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

-- create RBAC-restricted view

-- automatically updatable, but slow with WHERE IN
SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE package ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS package_rv;
CREATE OR REPLACE VIEW package_rv AS
    SELECT DISTINCT target.*
      FROM package AS target
     WHERE target.uuid IN (SELECT uuid FROM queryAccessibleObjectUuidsOfSubjectIds( 'view', 'package', currentSubjectIds()));
GRANT ALL PRIVILEGES ON package_rv TO restricted;

-- not automatically updatable, but fast with JOIN
SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE package ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS package_rv;
CREATE OR REPLACE VIEW package_rv AS
    SELECT DISTINCT target.*
      FROM package AS target
     JOIN queryAccessibleObjectUuidsOfSubjectIds( 'view', 'package', currentSubjectIds()) AS allowedObjId
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
            -- CONTINUE WHEN cust.reference < 18000;

            FOR t IN 0..randominrange(1, 2) LOOP
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

