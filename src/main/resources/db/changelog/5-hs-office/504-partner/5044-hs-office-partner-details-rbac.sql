--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-partner-details-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office.partner_details');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-partner-details-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_office.partner_details');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-partner-details-rbac-insert-trigger runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_office.partner_details_build_rbac_system(
    NEW hs_office.partner_details
)
    language plpgsql as $$

declare

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office.partner_details row.
 */

create or replace function hs_office.partner_details_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.partner_details_build_rbac_system(NEW);
    return NEW;
end; $$;

create or replace trigger build_rbac_system_after_insert_tg
    after insert on hs_office.partner_details
    for each row
execute procedure hs_office.partner_details_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-partner-details-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO hs_office.partner_details permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO hs_office.partner_details permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office.partner_details'),
                        rbac.global_ADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office.partner_details INSERT permission to specified role of new global rows.
*/
create or replace function hs_office.partner_details_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office.partner_details'),
            rbac.global_ADMIN());
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger partner_details_z_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure hs_office.partner_details_grants_insert_to_global_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-partner-details-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office.partner_details.
*/
create or replace function hs_office.partner_details_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission if rbac.global ADMIN
    if rbac.isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office.partner_details values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger partner_details_insert_permission_check_tg
    before insert on hs_office.partner_details
    for each row
        execute procedure hs_office.partner_details_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-partner-details-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromQuery('hs_office.partner_details',
    $idName$
        SELECT partnerDetails.uuid as uuid, partner_iv.idName as idName
            FROM hs_office.partner_details AS partnerDetails
            JOIN hs_office.partner partner ON partner.detailsUuid = partnerDetails.uuid
            JOIN hs_office.partner_iv partner_iv ON partner_iv.uuid = partner.uuid
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-partner-details-rbac-RESTRICTED-VIEW runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office.partner_details',
    $orderBy$
        uuid
    $orderBy$,
    $updates$
        registrationOffice = new.registrationOffice,
        registrationNumber = new.registrationNumber,
        birthPlace = new.birthPlace,
        birthName = new.birthName,
        birthday = new.birthday,
        dateOfDeath = new.dateOfDeath
    $updates$);
--//


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-office-partner-details-rbac-rebuild runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_office.partner_details after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_office.partner_details', null, <<insert executing global admin user here>>);
--  call hs_office.partner_details_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_office.partner_details.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_office.partner_details.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_office.partner_details_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_office.partner_details;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grant;

    FOR row IN SELECT * FROM hs_office.partner_details LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grant g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_office.partner_details_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grant;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

