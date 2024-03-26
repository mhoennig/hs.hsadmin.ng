--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


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
--changeset hs-office-person-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficePerson(
    NEW hs_office_person
)
    language plpgsql as $$

declare

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    perform createRoleWithGrants(
        hsOfficePersonOwner(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalAdmin()],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficePersonAdmin(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficePersonOwner(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficePersonReferrer(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsOfficePersonAdmin(NEW)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_person row.
 */

create or replace function insertTriggerForHsOfficePerson_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficePerson(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficePerson_tg
    after insert on hs_office_person
    for each row
execute procedure insertTriggerForHsOfficePerson_tf();
--//


-- ============================================================================
--changeset hs-office-person-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_person,
    where only global-admin has that permission.
*/
create or replace function hs_office_person_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into hs_office_person not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_person_insert_permission_check_tg
    before insert on hs_office_person
    for each row
    when ( not isGlobalAdmin() )
        execute procedure hs_office_person_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset hs-office-person-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_person',
    $idName$
        concat(tradeName, familyName, givenName)
    $idName$);
--//

-- ============================================================================
--changeset hs-office-person-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_person',
    $orderBy$
        concat(tradeName, familyName, givenName)
    $orderBy$,
    $updates$
        personType = new.personType,
        tradeName = new.tradeName,
        givenName = new.givenName,
        familyName = new.familyName
    $updates$);
--//

