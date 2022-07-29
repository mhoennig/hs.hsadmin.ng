--liquibase formatted sql

-- ============================================================================
--changeset hs-package-rbac-CREATE-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the related RbacObject through a BEFORE INSERT TRIGGER.
 */
drop trigger if exists createRbacObjectForPackage_Trigger on package;
create trigger createRbacObjectForPackage_Trigger
    before insert
    on package
    for each row
execute procedure createRbacObject();
--//


-- ============================================================================
--changeset hs-package-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function packageOwner(pac package)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('package', pac.uuid, 'admin');
end; $$;

create or replace function packageAdmin(pac package)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('package', pac.uuid, 'admin');
end; $$;

create or replace function packageTenant(pac package)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('package', pac.uuid, 'tenant');
end; $$;
--//


-- ============================================================================
--changeset hs-package-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the roles and their assignments for a new package for the AFTER INSERT TRIGGER.
 */
create or replace function createRbacRolesForPackage()
    returns trigger
    language plpgsql
    strict as $$
declare
    parentCustomer       customer;
    packageOwnerRoleUuid uuid;
    packageAdminRoleUuid uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    select * from customer as c where c.uuid = NEW.customerUuid into parentCustomer;

    -- an owner role is created and assigned to the customer's admin role
    packageOwnerRoleUuid = createRole(
        packageOwner(NEW),
        withoutPermissions(),
        beneathRole(customerAdmin(parentCustomer))
        );

    -- an owner role is created and assigned to the package owner role
    packageAdminRoleUuid = createRole(
        packageAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['add-unixuser', 'add-domain']),
        beneathRole(packageOwnerRoleUuid)
        );

    -- and a package tenant role is created and assigned to the package admin as well
    perform createRole(
        packageTenant(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view']),
        beneathRole(packageAdminRoleUuid),
        beingItselfA(customerTenant(parentCustomer))
        );

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new package.
 */

drop trigger if exists createRbacRolesForPackage_Trigger on package;
create trigger createRbacRolesForPackage_Trigger
    after insert
    on package
    for each row
execute procedure createRbacRolesForPackage();
--//

-- ============================================================================
--changeset hs-package-rbac-ROLES-REMOVAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Deletes the roles and their assignments of a deleted package for the BEFORE DELETE TRIGGER.
 */

create or replace function deleteRbacRulesForPackage()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        call deleteRole(findRoleId(packageOwner(OLD)));
        call deleteRole(findRoleId(packageAdmin(OLD)));
        call deleteRole(findRoleId(packageTenant(OLD)));
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
end; $$;

/*
    An BEFORE DELETE TRIGGER which deletes the role structure of a package.
 */

drop trigger if exists deleteRbacRulesForPackage_Trigger on package;
create trigger deleteRbacRulesForPackage_Trigger
    before delete
    on package
    for each row
execute procedure deleteRbacRulesForPackage();
--//


-- ============================================================================
--changeset hs-package-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the package main table which maps the identifying name
    (in this case, actually the column `name`) to the objectUuid.
 */
drop view if exists package_iv;
create or replace view package_iv as
select distinct target.uuid, target.name as idName
    from package as target;
-- TODO: Is it ok that everybody has access to this information?
grant all privileges on package_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case, actually the column `name`).
 */
create or replace function packageUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from package_iv iv where iv.idName = packageUuidByIdName.idName;
$$;
--//


-- ============================================================================
--changeset hs-package-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the customer main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
drop view if exists package_rv;
create or replace view package_rv as
select distinct target.*
    from package as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'package', currentSubjectIds()));
grant all privileges on package_rv to restricted;
--//
