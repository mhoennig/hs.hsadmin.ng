--liquibase formatted sql

-- ============================================================================
--changeset test-customer-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('test_customer');
--//


-- ============================================================================
--changeset test-customer-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('testCustomer', 'test_customer');
--//


-- ============================================================================
--changeset test-customer-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles and their assignments for a new customer for the AFTER INSERT TRIGGER.
 */

create or replace function createRbacRolesForTestCustomer()
    returns trigger
    language plpgsql
    strict as $$
declare
    testCustomerOwnerUuid uuid;
    customerAdminUuid uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    -- the owner role with full access for Hostsharing administrators
    testCustomerOwnerUuid = createRoleWithGrants(
        testCustomerOwner(NEW),
        permissions => array['*'],
        incomingSuperRoles => array[globalAdmin()]
        );

    -- the admin role for the customer's admins, who can view and add products
    customerAdminUuid = createRoleWithGrants(
        testCustomerAdmin(NEW),
        permissions => array['view', 'add-package'],
        -- NO auto assume for customer owner to avoid exploding permissions for administrators
        userUuids => array[getRbacUserId(NEW.adminUserName, 'create')], -- implicitly ignored if null
        grantedByRole => globalAdmin()
        );

    -- allow the customer owner role (thus administrators) to assume the customer admin role
    call grantRoleToRole(customerAdminUuid, testCustomerOwnerUuid, false);

    -- the tenant role which later can be used by owners+admins of sub-objects
    perform createRoleWithGrants(
        testCustomerTenant(NEW),
        permissions =>  array['view']
        );

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */

drop trigger if exists createRbacRolesForTestCustomer_Trigger on test_customer;
create trigger createRbacRolesForTestCustomer_Trigger
    after insert
    on test_customer
    for each row
execute procedure createRbacRolesForTestCustomer();
--//


-- ============================================================================
--changeset test-customer-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('test_customer', $idName$
    target.prefix
    $idName$);
--//


-- ============================================================================
--changeset test-customer-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('test_customer', 'target.prefix',
    $updates$
        reference = new.reference,
        prefix = new.prefix,
        adminUserName = new.adminUserName
    $updates$);
--//


-- ============================================================================
--changeset test-customer-rbac-ADD-CUSTOMER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for add-customer and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions  uuid[];
        globalObjectUuid        uuid;
        globalAdminRoleUuid         uuid ;
    begin
        call defineContext('granting global add-customer permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['add-customer']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addTestCustomerNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] add-customer not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to add a new customer.
 */
create trigger test_customer_insert_trigger
    before insert
    on test_customer
    for each row
    when ( not hasGlobalPermission('add-customer') )
execute procedure addTestCustomerNotAllowedForCurrentSubjects();
--//

