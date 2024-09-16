--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-person-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office_person');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-person-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsOfficePerson', 'hs_office_person');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-person-rbac-insert-trigger endDelimiter:--//
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
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    perform rbac.defineRoleWithGrants(
        hsOfficePersonOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.globalADMIN()],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hsOfficePersonADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficePersonOWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hsOfficePersonREFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsOfficePersonADMIN(NEW)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
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
--changeset RbacIdentityViewGenerator:hs-office-person-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office_person',
    $idName$
        concat(tradeName, familyName, givenName)
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-person-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office_person',
    $orderBy$
        concat(tradeName, familyName, givenName)
    $orderBy$,
    $updates$
        personType = new.personType,
        title = new.title,
        salutation = new.salutation,
        tradeName = new.tradeName,
        givenName = new.givenName,
        familyName = new.familyName
    $updates$);
--//

