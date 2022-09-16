--liquibase formatted sql

-- ============================================================================
--changeset hs-office-contact-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_contact');
--//


-- ============================================================================
--changeset hs-office-contact-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeContact', 'hs_office_contact');
--//


-- ============================================================================
--changeset hs-office-contact-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles and their assignments for a new contact for the AFTER INSERT TRIGGER.
 */

create or replace function createRbacRolesForHsOfficeContact()
    returns trigger
    language plpgsql
    strict as $$
declare
    ownerRole uuid;
    adminRole uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    -- the owner role with full access for the creator assigned to the current user
    ownerRole := createRole(
        hsOfficeContactOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(globalAdmin()),
        withoutSubRoles(),
        withUser(currentUser()), -- TODO.spec: Who is owner of a new contact?
        grantedByRole(globalAdmin())
        );

    -- the tenant role for those related users who can view the data
    adminRole := createRole(
            hsOfficeContactAdmin(NEW),
            grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit']),
            beneathRole(ownerRole)
        );

    -- the tenant role for those related users who can view the data
    perform createRole(
        hsOfficeContactTenant(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view']),
        beneathRole(adminRole)
        );

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */

create trigger createRbacRolesForHsOfficeContact_Trigger
    after insert
    on hs_office_contact
    for each row
execute procedure createRbacRolesForHsOfficeContact();
--//


-- ============================================================================
--changeset hs-office-contact-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the contact main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
create or replace view hs_office_contact_iv as
select target.uuid, cleanIdentifier(target.label) as idName
    from hs_office_contact as target;
-- TODO.spec: Is it ok that everybody has access to this information?
grant all privileges on hs_office_contact_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case the prefix).
 */
create or replace function hs_office_contactUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from hs_office_contact_iv iv where iv.idName = hs_office_contactUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the label).
 */
create or replace function hs_office_contactIdNameByUuid(uuid uuid)
    returns varchar
    language sql
    strict as $$
select idName from hs_office_contact_iv iv where iv.uuid = hs_office_contactIdNameByUuid.uuid;
$$;
--//


-- ============================================================================
--changeset hs-office-contact-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the contact main table with row-level limitation
    based on the 'view' permission of the current user or assumed roles.
 */
set session session authorization default;
drop view if exists hs_office_contact_rv;
create or replace view hs_office_contact_rv as
select target.*
    from hs_office_contact as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'hs_office_contact', currentSubjectsUuids()));
grant all privileges on hs_office_contact_rv to restricted;
--//


-- ============================================================================
--changeset hs-office-contact-rbac-INSTEAD-OF-INSERT-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of insert trigger function for hs_office_contact_rv.
 */
create or replace function insertHsOfficeContact()
    returns trigger
    language plpgsql as $$
declare
    newUser hs_office_contact;
begin
    insert
        into hs_office_contact
        values (new.*)
        returning * into newUser;
    return newUser;
end;
$$;

/*
    Creates an instead of insert trigger for the hs_office_contact_rv view.
 */
create trigger insertHsOfficeContact_Trigger
    instead of insert
    on hs_office_contact_rv
    for each row
execute function insertHsOfficeContact();
--//

-- ============================================================================
--changeset hs-office-contact-rbac-INSTEAD-OF-DELETE-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of delete trigger function for hs_office_contact_rv.

    Checks if the current subject (user / assumed role) has the permission to delete the row.
 */
create or replace function deleteHsOfficeContact()
    returns trigger
    language plpgsql as $$
begin
    if hasGlobalRoleGranted(currentUserUuid()) or
       old.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('delete', 'hs_office_contact', currentSubjectsUuids())) then
        delete from hs_office_contact c where c.uuid = old.uuid;
        return old;
    end if;
    raise exception '[403] User % not allowed to delete contact uuid %', currentUser(), old.uuid;
end; $$;

/*
    Creates an instead of delete trigger for the hs_office_contact_rv view.
 */
create trigger deleteHsOfficeContact_Trigger
    instead of delete
    on hs_office_contact_rv
    for each row
execute function deleteHsOfficeContact();
--/

-- ============================================================================
--changeset hs-office-contact-rbac-NEW-CONTACT:1 endDelimiter:--//
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
create or replace function addHsOfficeContactNotAllowedForCurrentSubjects()
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
create trigger hs_office_contact_insert_trigger
    before insert
    on hs_office_contact
    for each row
    -- TODO.spec: who is allowed to create new contacts
    when ( not hasAssumedRole() )
execute procedure addHsOfficeContactNotAllowedForCurrentSubjects();
--//

