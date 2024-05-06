--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


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
--changeset hs-office-contact-rbac-insert-trigger:1 endDelimiter:--//
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
    call enterTriggerForObjectUuid(NEW.uuid);

    perform createRoleWithGrants(
        hsOfficeContactOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalADMIN()],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficeContactADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficeContactOWNER(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficeContactREFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsOfficeContactADMIN(NEW)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
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
--changeset hs-office-contact-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_contact',
    $idName$
        caption
    $idName$);
--//


-- ============================================================================
--changeset hs-office-contact-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_contact',
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

