
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

CREATE OR REPLACE FUNCTION customerOwner(customerName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('customer', customerName, 'owner');
end; $$;

CREATE OR REPLACE FUNCTION customerAdmin(customerName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('customer', customerName, 'admin');
end; $$;

CREATE OR REPLACE FUNCTION customerTenant(customerName varchar)
    RETURNS varchar
    LANGUAGE plpgsql STRICT AS $$
begin
    return roleName('customer', customerName, 'tenant');
end; $$;


CREATE OR REPLACE FUNCTION createRbacRulesForCustomer()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    customerOwnerUuid uuid;
    customerAdminUuid uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;

    -- the owner role with full access for Hostsharing administrators
    customerOwnerUuid = createRole(
            customerOwner(NEW.prefix),
            grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['*']),
            beneathRole('administrators')
        );

    -- the admin role for the customer's admins, who can view and add products
    customerAdminUuid = createRole(
            customerAdmin(NEW.prefix),
            grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['view', 'add-package']),
            -- NO auto follow for customer owner to avoid exploding permissions for administrators
            withUser(NEW.adminUserName, 'create') -- implicitly ignored if null
        );

    -- allow the customer owner role (thus administrators) to assume the customer admin role
    call grantRoleToRole(customerAdminUuid, customerOwnerUuid, FALSE);

    -- the tenant role which later can be used by owners+admins of sub-objects
    perform createRole(
            customerTenant(NEW.prefix),
            grantingPermissions(forObjectUuid => NEW.uuid, permitOps => ARRAY['view'])
        );

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
DROP VIEW IF EXISTS customer_rv;
CREATE OR REPLACE VIEW customer_rv AS
    SELECT DISTINCT target.*
      FROM customer AS target
     WHERE target.uuid IN (SELECT queryAccessibleObjectUuidsOfSubjectIds( 'view', 'customer', currentSubjectIds()));
GRANT ALL PRIVILEGES ON customer_rv TO restricted;


-- generate Customer test data

SET SESSION SESSION AUTHORIZATION DEFAULT;
DO LANGUAGE plpgsql $$
    DECLARE
        currentTask varchar;
        custReference integer;
        custRowId uuid;
        custPrefix varchar;
        custAdminName varchar;
    BEGIN
        SET hsadminng.currentUser TO '';

        FOR t IN 0..6999 LOOP
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
