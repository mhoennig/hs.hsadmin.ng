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


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-office-contact-rbac-rebuild endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_office.contact after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_office.contact', null, <<insert executing global admin user here>>);
--  call hs_office.contact_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_office.contact.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_office.contact.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_office.contact_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_office.contact;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grants;

    FOR row IN SELECT * FROM hs_office.contact LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grants g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_office.contact_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grants;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

