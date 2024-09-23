--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-contact-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office.contact');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-contact-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_office.contact');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-contact-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_office.contact_build_rbac_system(
    NEW hs_office.contact
)
    language plpgsql as $$

declare

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    perform rbac.defineRoleWithGrants(
        hs_office.contact_OWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.global_ADMIN()],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.contact_ADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hs_office.contact_OWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.contact_REFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hs_office.contact_ADMIN(NEW)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office.contact row.
 */

create or replace function hs_office.contact_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.contact_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on hs_office.contact
    for each row
execute procedure hs_office.contact_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-contact-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office.contact',
    $idName$
        caption
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-contact-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office.contact',
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

