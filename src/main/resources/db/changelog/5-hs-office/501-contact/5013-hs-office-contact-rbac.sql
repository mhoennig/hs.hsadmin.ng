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
--changeset hs-office-contact-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO hs_office_contact permissions for the related global rows.
 */
do language plpgsql $$
    declare
        row global;
    begin
        call defineContext('create INSERT INTO hs_office_contact permissions for the related global rows');

        FOR row IN SELECT * FROM global
            LOOP
                call grantPermissionToRole(
                    createPermission(row.uuid, 'INSERT', 'hs_office_contact'),
                    globalGUEST());
            END LOOP;
    END;
$$;

/**
    Adds hs_office_contact INSERT permission to specified role of new global rows.
*/
create or replace function hs_office_contact_global_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_contact'),
            globalGUEST());
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_hs_office_contact_global_insert_tg
    after insert on global
    for each row
execute procedure hs_office_contact_global_insert_tf();
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

