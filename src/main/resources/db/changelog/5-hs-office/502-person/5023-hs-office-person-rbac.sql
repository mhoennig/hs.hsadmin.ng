--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-person-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office.person');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-person-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_office.person');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-person-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_office.person_build_rbac_system(
    NEW hs_office.person
)
    language plpgsql as $$

declare

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    perform rbac.defineRoleWithGrants(
        hs_office.person_OWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.global_ADMIN()],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.person_ADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hs_office.person_OWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.person_REFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hs_office.person_ADMIN(NEW)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office.person row.
 */

create or replace function hs_office.person_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.person_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on hs_office.person
    for each row
execute procedure hs_office.person_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-person-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office.person',
    $idName$
        concat(tradeName, familyName, givenName)
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-person-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office.person',
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


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-office-person-rbac-rebuild endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_office.person after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_office.person', null, <<insert executing global admin user here>>);
--  call hs_office.person_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_office.person.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_office.person.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_office.person_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_office.person;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grant;

    FOR row IN SELECT * FROM hs_office.person LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grant g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_office.person_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grant;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

