--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset michael.hoennig:hs-office-contact-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office_contact');
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-contact-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsOfficeContact', 'hs_office_contact');
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-contact-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficeContact(
    NEW hs_office_contact
)
    language plpgsql as $$

declare

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    perform rbac.defineRoleWithGrants(
        hsOfficeContactOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.globalAdmin()],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hsOfficeContactADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficeContactOWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hsOfficeContactREFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsOfficeContactADMIN(NEW)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_contact row.
 */

create or replace function insertTriggerForHsOfficeContact_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficeContact(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficeContact_tg
    after insert on hs_office_contact
    for each row
execute procedure insertTriggerForHsOfficeContact_tf();
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-contact-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office_contact',
    $idName$
        caption
    $idName$);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-contact-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office_contact',
    $orderBy$
        caption
    $orderBy$,
    $updates$
        caption = new.caption,
        postalAddress = new.postalAddress,
        emailAddresses = new.emailAddresses,
        phoneNumbers = new.phoneNumbers
    $updates$);
--//

