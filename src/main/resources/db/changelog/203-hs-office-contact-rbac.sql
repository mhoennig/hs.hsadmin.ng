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

call generateRbacIdentityView('hs_office_contact', $idName$
    target.label
    $idName$);
--//


-- ============================================================================
--changeset hs-office-contact-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_contact', 'target.label',
    $updates$
        label = new.label,
        postalAddress = new.postalAddress,
        emailAddresses = new.emailAddresses,
        phoneNumbers = new.phoneNumbers
    $updates$);
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

