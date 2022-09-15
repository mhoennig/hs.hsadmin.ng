--liquibase formatted sql


-- ============================================================================
--changeset hs-office-person-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_person');
--//


-- ============================================================================
--changeset hs-office-person-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function hsOfficePersonOwner(person hs_office_person)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_office_person', person.uuid, 'owner');
end; $$;

create or replace function hsOfficePersonAdmin(person hs_office_person)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_office_person', person.uuid, 'admin');
end; $$;

create or replace function hsOfficePersonTenant(person hs_office_person)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_office_person', person.uuid, 'tenant');
end; $$;
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

/*
    Creates a view to the person main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
create or replace view hs_office_person_iv as
select target.uuid, cleanIdentifier(concat(target.tradeName, target.familyName, target.givenName)) as idName
    from hs_office_person as target;
-- TODO.spec: Is it ok that everybody has access to this information?
grant all privileges on hs_office_person_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case the prefix).
 */
create or replace function hs_office_personUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from hs_office_person_iv iv where iv.idName = hs_office_personUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the label).
 */
create or replace function hs_office_personIdNameByUuid(uuid uuid)
    returns varchar
    language sql
    strict as $$
select idName from hs_office_person_iv iv where iv.uuid = hs_office_personIdNameByUuid.uuid;
$$;
--//


-- ============================================================================
--changeset hs-office-person-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the person main table with row-level limitation
    based on the 'view' permission of the current user or assumed roles.
 */
set session session authorization default;
drop view if exists hs_office_person_rv;
create or replace view hs_office_person_rv as
select target.*
    from hs_office_person as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'hs_office_person', currentSubjectsUuids()));
grant all privileges on hs_office_person_rv to restricted;
--//


-- ============================================================================
--changeset hs-office-person-rbac-INSTEAD-OF-INSERT-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of insert trigger function for hs_office_person_rv.
 */
create or replace function insertHsOfficePerson()
    returns trigger
    language plpgsql as $$
declare
    newUser hs_office_person;
begin
    insert
        into hs_office_person
        values (new.*)
        returning * into newUser;
    return newUser;
end;
$$;

/*
    Creates an instead of insert trigger for the hs_office_person_rv view.
 */
create trigger insertHsOfficePerson_Trigger
    instead of insert
    on hs_office_person_rv
    for each row
execute function insertHsOfficePerson();
--//

-- ============================================================================
--changeset hs-office-person-rbac-INSTEAD-OF-DELETE-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of delete trigger function for hs_office_person_rv.

    Checks if the current subject (user / assumed role) has the permission to delete the row.
 */
create or replace function deleteHsOfficePerson()
    returns trigger
    language plpgsql as $$
begin
    if hasGlobalRoleGranted(currentUserUuid()) or
       old.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('delete', 'hs_office_person', currentSubjectsUuids())) then
        delete from hs_office_person c where c.uuid = old.uuid;
        return old;
    end if;
    raise exception '[403] User % not allowed to delete person uuid %', currentUser(), old.uuid;
end; $$;

/*
    Creates an instead of delete trigger for the hs_office_person_rv view.
 */
create trigger deleteHsOfficePerson_Trigger
    instead of delete
    on hs_office_person_rv
    for each row
execute function deleteHsOfficePerson();
--/

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

