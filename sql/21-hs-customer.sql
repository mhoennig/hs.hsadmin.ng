
-- ========================================================
-- Customer example with RBAC
-- --------------------------------------------------------

SET SESSION SESSION AUTHORIZATION DEFAULT ;

CREATE TABLE IF NOT EXISTS customer (
    uuid uuid UNIQUE REFERENCES RbacObject(uuid),
    reference int not null unique CHECK (reference BETWEEN 10000 AND 99999),
    prefix character(3) unique,
    adminUserName varchar(63)
);

DROP TRIGGER IF EXISTS createRbacObjectForCustomer_Trigger ON customer;
CREATE TRIGGER createRbacObjectForCustomer_Trigger
    BEFORE INSERT ON customer
    FOR EACH ROW EXECUTE PROCEDURE createRbacObject();

CREATE OR REPLACE FUNCTION createRbacRulesForCustomer()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    adminUserNameUuid      uuid;
    customerOwnerRoleId    uuid;
    customerAdminRoleId    uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    -- an owner role is created and assigned to the administrators group
    customerOwnerRoleId = createRole('customer#'||NEW.prefix||'.owner');
    call grantRoleToRole(customerOwnerRoleId, getRoleId('administrators', 'create'));
    -- ... and permissions for all ops are assigned
    call grantPermissionsToRole(customerOwnerRoleId,
        createPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']));

    -- ... also a customer admin role is created and granted to the customer owner role
    customerAdminRoleId = createRole('customer#'||NEW.prefix||'.admin');
    call grantRoleToRole(customerAdminRoleId, customerOwnerRoleId);
    -- ... to which a permission with view and add- ops is assigned
    call grantPermissionsToRole(customerAdminRoleId,
        createPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['view', 'add-package']));
    -- if a admin user is given for the customer,
    IF (NEW.adminUserName IS NOT NULL) THEN
        -- ... the customer admin role is also assigned to the admin user of the customer
        adminUserNameUuid = findRoleId(NEW.adminUserName);
        IF ( adminUserNameUuid IS NULL ) THEN
            adminUserNameUuid = createRbacUser(NEW.adminUserName);
        END IF;
        call grantRoleToUser(customerAdminRoleId, adminUserNameUuid);
    END IF;

    RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS createRbacRulesForCustomer_Trigger ON customer;
CREATE TRIGGER createRbacRulesForCustomer_Trigger
    AFTER INSERT ON customer
    FOR EACH ROW EXECUTE PROCEDURE createRbacRulesForCustomer();

CREATE OR REPLACE FUNCTION deleteRbacRulesForCustomer()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    objectTable varchar = 'customer';
BEGIN
    IF TG_OP = 'DELETE' THEN

        -- delete the owner role (for admininstrators)
        call deleteRole(findRoleId(objectTable||'#'||NEW.prefix||'.owner'));

        -- delete the customer admin role
        call deleteRole(findRoleId(objectTable||'#'||NEW.prefix||'.admin'));
    ELSE
        RAISE EXCEPTION 'invalid usage of TRIGGER BEFORE DELETE';
    END IF;
END; $$;

DROP TRIGGER IF EXISTS deleteRbacRulesForCustomer_Trigger ON customer;
CREATE TRIGGER deleteRbacRulesForCustomer_Trigger
    BEFORE DELETE ON customer
    FOR EACH ROW EXECUTE PROCEDURE deleteRbacRulesForCustomer();


-- create RBAC restricted view

SET SESSION SESSION AUTHORIZATION DEFAULT;
ALTER TABLE customer ENABLE ROW LEVEL SECURITY;
DROP VIEW IF EXISTS cust_view;
DROP VIEW IF EXISTS customer_rv;
CREATE OR REPLACE VIEW customer_rv AS
    SELECT DISTINCT target.*
      FROM customer AS target
      JOIN queryAccessibleObjectUuidsOfSubjectIds( 'view', currentSubjectIds()) AS allowedObjId
           ON target.uuid = allowedObjId;
GRANT ALL PRIVILEGES ON customer_rv TO restricted;


-- generate Customer test data

DO LANGUAGE plpgsql $$
    DECLARE
        currentTask varchar;
        custReference integer;
        custRowId uuid;
        custPrefix varchar;
        custAdminName varchar;
    BEGIN
        SET hsadminng.currentUser TO '';

        FOR t IN 0..9999 LOOP
                currentTask = 'creating RBAC test customer #' || t;
                SET LOCAL hsadminng.currentUser TO 'mike@hostsharing.net';
                SET LOCAL hsadminng.assumedRoles = '';
                SET LOCAL hsadminng.currentTask TO currentTask;

                -- When a new customer is created,
                custReference = 10000 + t;
                custRowId = uuid_generate_v4();
                custPrefix = intToVarChar(t, 3 );
                custAdminName = 'admin@' || custPrefix || '.example.com';

                raise notice 'creating customer %:%', custReference, custPrefix;
                insert into customer (reference, prefix, adminUserName)
                VALUES (custReference, custPrefix, custAdminName);

                COMMIT;

            END LOOP;

    END;
$$;
