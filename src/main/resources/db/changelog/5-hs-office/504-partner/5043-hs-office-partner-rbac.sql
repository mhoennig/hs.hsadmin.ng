--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-partner-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office.partner');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-partner-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_office.partner');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-partner-rbac-insert-trigger runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_office.partner_build_rbac_system(
    NEW hs_office.partner
)
    language plpgsql as $$

declare
    newPartnerRel hs_office.relation;
    newPartnerDetails hs_office.partner_details;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office.relation WHERE uuid = NEW.partnerRelUuid    INTO newPartnerRel;
    assert newPartnerRel.uuid is not null, format('newPartnerRel must not be null for NEW.partnerRelUuid = %s of hs_office.partner', NEW.partnerRelUuid);

    SELECT * FROM hs_office.partner_details WHERE uuid = NEW.detailsUuid    INTO newPartnerDetails;
    assert newPartnerDetails.uuid is not null, format('newPartnerDetails must not be null for NEW.detailsUuid = %s of hs_office.partner', NEW.detailsUuid);

    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'DELETE'), hs_office.relation_OWNER(newPartnerRel));
    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'SELECT'), hs_office.relation_TENANT(newPartnerRel));
    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'UPDATE'), hs_office.relation_ADMIN(newPartnerRel));
    call rbac.grantPermissionToRole(rbac.createPermission(newPartnerDetails.uuid, 'DELETE'), hs_office.relation_OWNER(newPartnerRel));
    call rbac.grantPermissionToRole(rbac.createPermission(newPartnerDetails.uuid, 'SELECT'), hs_office.relation_AGENT(newPartnerRel));
    call rbac.grantPermissionToRole(rbac.createPermission(newPartnerDetails.uuid, 'UPDATE'), hs_office.relation_AGENT(newPartnerRel));

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office.partner row.
 */

create or replace function hs_office.partner_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.partner_build_rbac_system(NEW);
    return NEW;
end; $$;

create or replace trigger build_rbac_system_after_insert_tg
    after insert on hs_office.partner
    for each row
execute procedure hs_office.partner_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-partner-rbac-update-trigger runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure hs_office.partner_update_rbac_system(
    OLD hs_office.partner,
    NEW hs_office.partner
)
    language plpgsql as $$

declare
    oldPartnerRel hs_office.relation;
    newPartnerRel hs_office.relation;
    oldPartnerDetails hs_office.partner_details;
    newPartnerDetails hs_office.partner_details;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office.relation WHERE uuid = OLD.partnerRelUuid    INTO oldPartnerRel;
    assert oldPartnerRel.uuid is not null, format('oldPartnerRel must not be null for OLD.partnerRelUuid = %s of hs_office.partner', OLD.partnerRelUuid);

    SELECT * FROM hs_office.relation WHERE uuid = NEW.partnerRelUuid    INTO newPartnerRel;
    assert newPartnerRel.uuid is not null, format('newPartnerRel must not be null for NEW.partnerRelUuid = %s of hs_office.partner', NEW.partnerRelUuid);

    SELECT * FROM hs_office.partner_details WHERE uuid = OLD.detailsUuid    INTO oldPartnerDetails;
    assert oldPartnerDetails.uuid is not null, format('oldPartnerDetails must not be null for OLD.detailsUuid = %s of hs_office.partner', OLD.detailsUuid);

    SELECT * FROM hs_office.partner_details WHERE uuid = NEW.detailsUuid    INTO newPartnerDetails;
    assert newPartnerDetails.uuid is not null, format('newPartnerDetails must not be null for NEW.detailsUuid = %s of hs_office.partner', NEW.detailsUuid);


    if NEW.partnerRelUuid <> OLD.partnerRelUuid then

        call rbac.revokePermissionFromRole(rbac.getPermissionId(OLD.uuid, 'DELETE'), hs_office.relation_OWNER(oldPartnerRel));
        call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'DELETE'), hs_office.relation_OWNER(newPartnerRel));

        call rbac.revokePermissionFromRole(rbac.getPermissionId(OLD.uuid, 'UPDATE'), hs_office.relation_ADMIN(oldPartnerRel));
        call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'UPDATE'), hs_office.relation_ADMIN(newPartnerRel));

        call rbac.revokePermissionFromRole(rbac.getPermissionId(OLD.uuid, 'SELECT'), hs_office.relation_TENANT(oldPartnerRel));
        call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'SELECT'), hs_office.relation_TENANT(newPartnerRel));

        call rbac.revokePermissionFromRole(rbac.getPermissionId(oldPartnerDetails.uuid, 'DELETE'), hs_office.relation_OWNER(oldPartnerRel));
        call rbac.grantPermissionToRole(rbac.createPermission(newPartnerDetails.uuid, 'DELETE'), hs_office.relation_OWNER(newPartnerRel));

        call rbac.revokePermissionFromRole(rbac.getPermissionId(oldPartnerDetails.uuid, 'UPDATE'), hs_office.relation_AGENT(oldPartnerRel));
        call rbac.grantPermissionToRole(rbac.createPermission(newPartnerDetails.uuid, 'UPDATE'), hs_office.relation_AGENT(newPartnerRel));

        call rbac.revokePermissionFromRole(rbac.getPermissionId(oldPartnerDetails.uuid, 'SELECT'), hs_office.relation_AGENT(oldPartnerRel));
        call rbac.grantPermissionToRole(rbac.createPermission(newPartnerDetails.uuid, 'SELECT'), hs_office.relation_AGENT(newPartnerRel));

    end if;

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER UPDATE TRIGGER to re-wire the grant structure for a new hs_office.partner row.
 */

create or replace function hs_office.partner_update_rbac_system_after_update_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.partner_update_rbac_system(OLD, NEW);
    return NEW;
end; $$;

create or replace trigger update_rbac_system_after_update_tg
    after update on hs_office.partner
    for each row
execute procedure hs_office.partner_update_rbac_system_after_update_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-partner-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO hs_office.partner permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO hs_office.partner permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office.partner'),
                        rbac.global_ADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office.partner INSERT permission to specified role of new global rows.
*/
create or replace function hs_office.partner_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office.partner'),
            rbac.global_ADMIN());
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger partner_z_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure hs_office.partner_grants_insert_to_global_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-partner-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office.partner.
*/
create or replace function hs_office.partner_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission if rbac.global ADMIN
    if rbac.isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office.partner values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger partner_insert_permission_check_tg
    before insert on hs_office.partner
    for each row
        execute procedure hs_office.partner_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-partner-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office.partner',
    $idName$
        'P-' || partnerNumber
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-partner-rbac-RESTRICTED-VIEW runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office.partner',
    $orderBy$
        'P-' || partnerNumber
    $orderBy$,
    $updates$
        partnerRelUuid = new.partnerRelUuid
    $updates$);
--//


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-office-partner-rbac-rebuild runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_office.partner after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_office.partner', null, <<insert executing global admin user here>>);
--  call hs_office.partner_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_office.partner.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_office.partner.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_office.partner_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_office.partner;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grant;

    FOR row IN SELECT * FROM hs_office.partner LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grant g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_office.partner_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grant;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

