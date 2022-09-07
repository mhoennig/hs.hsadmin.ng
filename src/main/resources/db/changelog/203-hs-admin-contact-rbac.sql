--liquibase formatted sql

-- ============================================================================
--changeset hs-admin-contact-rbac-CREATE-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the related RbacObject through a BEFORE INSERT TRIGGER.
 */
create trigger createRbacObjectForHsAdminCustomer_Trigger
    before insert
    on hs_admin_contact
    for each row
execute procedure createRbacObject();
--//

-- ============================================================================
--changeset hs-admin-contact-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function hsAdminContactOwner(contact hs_admin_contact)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_admin_contact', contact.uuid, 'owner');
end; $$;

create or replace function hsAdminContactOwner(contact hs_admin_contact)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_admin_contact', contact.uuid, 'admin');
end; $$;

create or replace function hsAdminContactTenant(contact hs_admin_contact)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_admin_contact', contact.uuid, 'tenant');
end; $$;
--//


-- ============================================================================
--changeset hs-admin-contact-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles and their assignments for a new contact for the AFTER INSERT TRIGGER.
 */

create or replace function createRbacRolesForHsAdminContact()
    returns trigger
    language plpgsql
    strict as $$
declare
    ownerRole uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    -- the owner role with full access for the creator assigned to the current user
    ownerRole = createRole(
        hsAdminContactOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(globalAdmin()),
        withoutSubRoles(),
        withUser(currentUser()), -- TODO.spec: Who is owner of a new contact?
        grantedByRole(globalAdmin())
        );

    -- the tenant role for those related users who can view the data
    perform createRole(
        hsAdminContactTenant(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view']),
        beneathRole(ownerRole)
        );

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */

create trigger createRbacRolesForHsAdminContact_Trigger
    after insert
    on hs_admin_contact
    for each row
execute procedure createRbacRolesForHsAdminContact();
--//


-- ============================================================================
--changeset hs-admin-contact-rbac-ROLES-REMOVAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Deletes the roles and their assignments of a deleted contact for the BEFORE DELETE TRIGGER.
 */
create or replace function deleteRbacRulesForHsAdminContact()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        call deleteRole(findRoleId(hsAdminContactOwner(OLD)));
        call deleteRole(findRoleId(hsAdminContactTenant(OLD)));
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
    return old;
end; $$;

/*
    An BEFORE DELETE TRIGGER which deletes the role structure of a contact.
 */
create trigger deleteRbacRulesForTestContact_Trigger
    before delete
    on hs_admin_contact
    for each row
execute procedure deleteRbacRulesForHsAdminContact();
--//

-- ============================================================================
--changeset hs-admin-contact-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the contact main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
create or replace view hs_admin_contact_iv as
select target.uuid, cleanIdentifier(target.label) as idName
    from hs_admin_contact as target;
-- TODO.spec: Is it ok that everybody has access to this information?
grant all privileges on hs_admin_contact_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case the prefix).
 */
create or replace function hs_admin_contactUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from hs_admin_contact_iv iv where iv.idName = hs_admin_contactUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the label).
 */
create or replace function hs_admin_contactIdNameByUuid(uuid uuid)
    returns varchar
    language sql
    strict as $$
select idName from hs_admin_contact_iv iv where iv.uuid = hs_admin_contactIdNameByUuid.uuid;
$$;
--//


-- ============================================================================
--changeset hs-admin-contact-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the contact main table with row-level limitation
    based on the 'view' permission of the current user or assumed roles.
 */
set session session authorization default;
drop view if exists hs_admin_contact_rv;
create or replace view hs_admin_contact_rv as
select target.*
    from hs_admin_contact as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'hs_admin_contact', currentSubjectsUuids()));
grant all privileges on hs_admin_contact_rv to restricted;
--//


-- ============================================================================
--changeset hs-admin-contact-rbac-INSTEAD-OF-INSERT-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of insert trigger function for hs_admin_contact_rv.
 */
create or replace function insertHsAdminContact()
    returns trigger
    language plpgsql as $$
declare
    newUser hs_admin_contact;
begin
    insert
        into hs_admin_contact
        values (new.*)
        returning * into newUser;
    return newUser;
end;
$$;

/*
    Creates an instead of insert trigger for the hs_admin_contact_rv view.
 */
create trigger insertHsAdminContact_Trigger
    instead of insert
    on hs_admin_contact_rv
    for each row
execute function insertHsAdminContact();
--//

-- ============================================================================
--changeset hs-admin-contact-rbac-INSTEAD-OF-DELETE-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of delete trigger function for hs_admin_contact_rv.
 */
create or replace function deleteHsAdminContact()
    returns trigger
    language plpgsql as $$
begin
    if true or hasGlobalRoleGranted(currentUserUuid()) or
       old.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('delete', 'hs_admin_contact', currentSubjectsUuids())) then
        delete from hs_admin_contact c where c.uuid = old.uuid;
        return old;
    end if;
    raise exception '[403] User % not allowed to delete contact uuid %', currentUser(), old.uuid;
end; $$;

/*
    Creates an instead of delete trigger for the hs_admin_contact_rv view.
 */
create trigger deleteHsAdminContact_Trigger
    instead of delete
    on hs_admin_contact_rv
    for each row
execute function deleteHsAdminContact();
--/

-- ============================================================================
--changeset hs-admin-contact-rbac-NEW-CONTACT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-contact and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions  uuid[];
        globalObjectUuid        uuid;
        globalAdminRoleUuid         uuid ;
    begin
        call defineContext('granting global new-contact permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-contact']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addHsAdminContactNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-contact not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new customer.
 */
create trigger hs_admin_contact_insert_trigger
    before insert
    on hs_admin_contact
    for each row
    -- TODO.spec: who is allowed to create new contacts
    when ( not hasAssumedRole() )
execute procedure addHsAdminContactNotAllowedForCurrentSubjects();
--//

