--liquibase formatted sql

-- ============================================================================
--changeset hs-office-person-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_person');
--//


-- ============================================================================
--changeset hs-office-person-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficePerson', 'hs_office_person');
--//


-- ============================================================================
--changeset hs-office-person-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the roles and their assignments for a new person for the AFTER INSERT TRIGGER.
 */
create or replace function createRbacRolesForHsOfficePerson()
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
            hsOfficePersonOwner(NEW),
            grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
            beneathRole(globalAdmin()),
            withoutSubRoles(),
            withUser(currentUser()), -- TODO.spec: Who is owner of a new person?
            grantedByRole(globalAdmin())
        );

    -- the tenant role for those related users who can view the data
    adminRole := createRole(
            hsOfficePersonAdmin(NEW),
            grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit']),
            beneathRole(ownerRole)
        );

    -- the tenant role for those related users who can view the data
    perform createRole(
            hsOfficePersonTenant(NEW),
            grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view']),
            beneathRole(adminRole)
        );

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */

create trigger createRbacRolesForHsOfficePerson_Trigger
    after insert
    on hs_office_person
    for each row
execute procedure createRbacRolesForHsOfficePerson();
--//


-- ============================================================================
--changeset hs-office-person-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('hs_office_person', $idName$
    concat(target.tradeName, target.familyName, target.givenName)
    $idName$);
--//


-- ============================================================================
--changeset hs-office-person-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_person', 'concat(target.tradeName, target.familyName, target.givenName)',
    $updates$
        personType = new.personType,
        tradeName = new.tradeName,
        givenName = new.givenName,
        familyName = new.familyName
    $updates$);
--//


-- ============================================================================
--changeset hs-office-person-rbac-NEW-PERSON:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-person and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions uuid[];
        globalObjectUuid       uuid;
        globalAdminRoleUuid    uuid ;
    begin
        call defineContext('granting global new-person permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-person']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addHsOfficePersonNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-person not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new customer.
 */
create trigger hs_office_person_insert_trigger
    before insert
    on hs_office_person
    for each row
    -- TODO.spec: who is allowed to create new persons
    when ( not hasAssumedRole() )
execute procedure addHsOfficePersonNotAllowedForCurrentSubjects();
--//

