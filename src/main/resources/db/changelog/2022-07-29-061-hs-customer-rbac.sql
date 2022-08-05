--liquibase formatted sql

-- ============================================================================
--changeset hs-customer-rbac-CREATE-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the related RbacObject through a BEFORE INSERT TRIGGER.
 */
drop trigger if exists createRbacObjectForCustomer_Trigger on customer;
create trigger createRbacObjectForCustomer_Trigger
    before insert
    on customer
    for each row
execute procedure createRbacObject();
--//

-- ============================================================================
--changeset hs-customer-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function customerOwner(customer customer)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('customer', customer.uuid, 'owner');
end; $$;

create or replace function customerAdmin(customer customer)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('customer', customer.uuid, 'admin');
end; $$;

create or replace function customerTenant(customer customer)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('customer', customer.uuid, 'tenant');
end; $$;
--//


-- ============================================================================
--changeset hs-customer-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles and their assignments for a new customer for the AFTER INSERT TRIGGER.
 */

create or replace function createRbacRolesForCustomer()
    returns trigger
    language plpgsql
    strict as $$
declare
    customerOwnerUuid uuid;
    customerAdminUuid uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    -- the owner role with full access for Hostsharing administrators
    customerOwnerUuid = createRole(
        customerOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(hostsharingAdmin())
        );

    -- the admin role for the customer's admins, who can view and add products
    customerAdminUuid = createRole(
        customerAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view', 'add-package']),
        -- NO auto follow for customer owner to avoid exploding permissions for administrators
        withUser(NEW.adminUserName, 'create') -- implicitly ignored if null
        );

    -- allow the customer owner role (thus administrators) to assume the customer admin role
    call grantRoleToRole(customerAdminUuid, customerOwnerUuid, false);

    -- the tenant role which later can be used by owners+admins of sub-objects
    perform createRole(
        customerTenant(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view'])
        );

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */

drop trigger if exists createRbacRolesForCustomer_Trigger on customer;
create trigger createRbacRolesForCustomer_Trigger
    after insert
    on customer
    for each row
execute procedure createRbacRolesForCustomer();
--//


-- ============================================================================
--changeset hs-customer-rbac-ROLES-REMOVAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Deletes the roles and their assignments of a deleted customer for the BEFORE DELETE TRIGGER.
 */

create or replace function deleteRbacRulesForCustomer()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        call deleteRole(findRoleId(customerOwner(OLD)));
        call deleteRole(findRoleId(customerAdmin(OLD)));
        call deleteRole(findRoleId(customerTenant(OLD)));
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
end; $$;

/*
    An BEFORE DELETE TRIGGER which deletes the role structure of a customer.
 */

drop trigger if exists deleteRbacRulesForCustomer_Trigger on customer;
create trigger deleteRbacRulesForCustomer_Trigger
    before delete
    on customer
    for each row
execute procedure deleteRbacRulesForCustomer();
--//

-- ============================================================================
--changeset hs-customer-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the customer main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
drop view if exists customer_iv;
create or replace view customer_iv as
select target.uuid, target.prefix as idName
    from customer as target;
-- TODO: Is it ok that everybody has access to this information?
grant all privileges on customer_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case the prefix).
 */
create or replace function customerUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from customer_iv iv where iv.idName = customerUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the prefix).
 */
create or replace function customerIdNameByUuid(uuid uuid)
    returns varchar
    language sql
    strict as $$
select idName from customer_iv iv where iv.uuid = customerIdNameByUuid.uuid;
$$;
--//


-- ============================================================================
--changeset hs-customer-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the customer main table with row-level limitation
    based on the 'view' permission of the current user or assumed roles.
 */
set session session authorization default;
drop view if exists customer_rv;
create or replace view customer_rv as
select target.*
    from customer as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'customer', currentSubjectIds()));
grant all privileges on customer_rv to restricted;
--//


-- ============================================================================
--changeset hs-customer-rbac-ADD-CUSTOMER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for add-customer and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions uuid[];
        hostsharingObjectUuid  uuid;
        hsAdminRoleUuid        uuid ;
    begin
        hsAdminRoleUuid := findRoleId(hostsharingAdmin());
        hostsharingObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(hostsharingObjectUuid, array ['add-customer']);
        call grantPermissionsToRole(hsAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addCustomerNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] add-customer not permitted for %', array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to add a new customer.
 */
create trigger customer_insert_trigger
    before insert
    on customer
    for each row
    when ( currentUser() <> 'mike@hostsharing.net' or not hasGlobalPermission('add-customer') )
execute procedure addCustomerNotAllowedForCurrentSubjects();
--//

