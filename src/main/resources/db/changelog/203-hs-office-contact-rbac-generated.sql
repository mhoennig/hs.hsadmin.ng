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
        hsOfficeContactOwner(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalAdmin()],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficeContactAdmin(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficeContactOwner(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficeContactReferrer(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsOfficeContactAdmin(NEW)]
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
--changeset hs-office-contact-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_contact,
    where only global-admin has that permission.
*/
create or replace function hs_office_contact_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into hs_office_contact not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_contact_insert_permission_check_tg
    before insert on hs_office_contact
    for each row
    when ( not isGlobalAdmin() )
        execute procedure hs_office_contact_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset hs-office-contact-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_contact',
    $idName$
        label
    $idName$);
--//

-- ============================================================================
--changeset hs-office-contact-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_contact',
    $orderBy$
        label
    $orderBy$,
    $updates$
        label = new.label,
        postalAddress = new.postalAddress,
        emailAddresses = new.emailAddresses,
        phoneNumbers = new.phoneNumbers
    $updates$);
--//

