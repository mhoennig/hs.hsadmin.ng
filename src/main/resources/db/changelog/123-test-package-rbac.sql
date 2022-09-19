--liquibase formatted sql

-- ============================================================================
--changeset test-package-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('test_package');
--//


-- ============================================================================
--changeset test-package-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('testPackage', 'test_package');
--//


-- ============================================================================
--changeset test-package-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the roles and their assignments for a new package for the AFTER INSERT TRIGGER.
 */
create or replace function createRbacRolesForTestPackage()
    returns trigger
    language plpgsql
    strict as $$
declare
    parentCustomer       test_customer;
    packageOwnerRoleUuid uuid;
    packageAdminRoleUuid uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    select * from test_customer as c where c.uuid = NEW.customerUuid into parentCustomer;

    -- an owner role is created and assigned to the customer's admin role
    packageOwnerRoleUuid = createRole(
        testPackageOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(testCustomerAdmin(parentCustomer))
        );

    -- an owner role is created and assigned to the package owner role
    packageAdminRoleUuid = createRole(
        testPackageAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['add-domain']),
        beneathRole(packageOwnerRoleUuid)
        );

    -- and a package tenant role is created and assigned to the package admin as well
    perform createRole(
        testPackageTenant(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view']),
        beneathRole(packageAdminRoleUuid),
        beingItselfA(testCustomerTenant(parentCustomer))
        );

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new package.
 */

create trigger createRbacRolesForTestPackage_Trigger
    after insert
    on test_package
    for each row
execute procedure createRbacRolesForTestPackage();
--//


-- ============================================================================
--changeset test-package-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('test_package', 'target.name');
--//


-- ============================================================================
--changeset test-package-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the customer main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
-- drop view if exists test_package_rv;
-- create or replace view test_package_rv as
-- select target.*
--     from test_package as target
--     where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'test_package', currentSubjectsUuids()))
--     order by target.name;
-- grant all privileges on test_package_rv to restricted;

call generateRbacRestrictedView('test_package', 'target.name',
    $updates$
        version = new.version,
        customerUuid = new.customerUuid,
        name = new.name,
        description = new.description
    $updates$);

--//


