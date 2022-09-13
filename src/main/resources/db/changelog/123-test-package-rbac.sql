--liquibase formatted sql

-- ============================================================================
--changeset test-package-rbac-CREATE-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the related RbacObject through a BEFORE INSERT TRIGGER.
 */
drop trigger if exists createRbacObjectForPackage_Trigger on test_package;
create trigger createRbacObjectForPackage_Trigger
    before insert
    on test_package
    for each row
execute procedure insertRelatedRbacObject();
--//


-- ============================================================================
--changeset test-package-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function testPackageOwner(pac test_package)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('test_package', pac.uuid, 'owner');
end; $$;

create or replace function testPackageAdmin(pac test_package)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('test_package', pac.uuid, 'admin');
end; $$;

create or replace function testPackageTenant(pac test_package)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('test_package', pac.uuid, 'tenant');
end; $$;
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
        withoutPermissions(),
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

drop trigger if exists createRbacRolesForTestPackage_Trigger on test_package;
create trigger createRbacRolesForTestPackage_Trigger
    after insert
    on test_package
    for each row
execute procedure createRbacRolesForTestPackage();
--//


-- ============================================================================
--changeset test-package-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the package main table which maps the identifying name
    (in this case, actually the column `name`) to the objectUuid.
 */
drop view if exists test_package_iv;
create or replace view test_package_iv as
select distinct target.uuid, target.name as idName
    from test_package as target;
-- TODO: Is it ok that everybody has access to this information?
grant all privileges on test_package_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case, actually the column `name`).
 */
create or replace function test_packageUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from test_package_iv iv where iv.idName = test_packageUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the name).
 */
create or replace function test_packageIdNameByUuid(uuid uuid)
    returns varchar
    stable leakproof
    language sql
    strict as $$
select idName from test_package_iv iv where iv.uuid = test_packageIdNameByUuid.uuid;
$$;
--//


-- ============================================================================
--changeset test-package-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the customer main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
drop view if exists test_package_rv;
create or replace view test_package_rv as
select target.*
    from test_package as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'test_package', currentSubjectsUuids()))
    order by target.name;
grant all privileges on test_package_rv to restricted;
--//
