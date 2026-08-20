--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-hosting-asset-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_hosting.asset');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-hosting-asset-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_hosting.asset');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-hosting-asset-rbac-insert-trigger runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_hosting.asset_build_rbac_system(
    NEW hs_hosting.asset
)
    language plpgsql as $$

declare
    newBookingItem hs_booking.item;
    newAssignedToAsset hs_hosting.asset;
    newAlarmContact hs_office.contact;
    newParentAsset hs_hosting.asset;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_booking.item WHERE uuid = NEW.bookingItemUuid    INTO newBookingItem;

    SELECT * FROM hs_hosting.asset WHERE uuid = NEW.assignedToAssetUuid    INTO newAssignedToAsset;

    SELECT * FROM hs_office.contact WHERE uuid = NEW.alarmContactUuid    INTO newAlarmContact;

    SELECT * FROM hs_hosting.asset WHERE uuid = NEW.parentAssetUuid    INTO newParentAsset;

    perform rbac.defineRoleWithGrants(
        hs_hosting.asset_OWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[
            	hs_booking.item_ADMIN(newBookingItem),
            	hs_hosting.asset_ADMIN(newParentAsset),
            	rbac.global_ADMIN(rbac.unassumed())]
    );

    perform rbac.defineRoleWithGrants(
        hs_hosting.asset_ADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[
            	hs_booking.item_AGENT(newBookingItem),
            	hs_hosting.asset_AGENT(newParentAsset),
            	hs_hosting.asset_OWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hs_hosting.asset_AGENT(NEW),
            incomingSuperRoles => array[
            	hs_hosting.asset_ADMIN(NEW),
            	hs_hosting.asset_AGENT(newAssignedToAsset)],
            outgoingSubRoles => array[
            	hs_hosting.asset_TENANT(newAssignedToAsset),
            	hs_office.contact_REFERRER(newAlarmContact)]
    );

    perform rbac.defineRoleWithGrants(
        hs_hosting.asset_TENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hs_hosting.asset_AGENT(NEW),
            	hs_office.contact_ADMIN(newAlarmContact)],
            outgoingSubRoles => array[
            	hs_booking.item_TENANT(newBookingItem),
            	hs_hosting.asset_TENANT(newParentAsset)]
    );

    IF NEW.type = 'DOMAIN_SETUP' THEN
    END IF;

    call rbac.grantRoleToCurrentContext(hs_hosting.asset_OWNER(NEW));

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_hosting.asset row.
 */

create or replace function hs_hosting.asset_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_hosting.asset_build_rbac_system(NEW);
    return NEW;
end; $$;

create or replace trigger build_rbac_system_after_insert_tg
    after insert on hs_hosting.asset
    for each row
execute procedure hs_hosting.asset_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-hosting-asset-rbac-update-trigger runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure hs_hosting.asset_update_rbac_system(
    OLD hs_hosting.asset,
    NEW hs_hosting.asset
)
    language plpgsql as $$
begin

    if NEW.assignedToAssetUuid is distinct from OLD.assignedToAssetUuid
    or NEW.alarmContactUuid is distinct from OLD.alarmContactUuid then
        delete from rbac.grant g where g.grantedbytriggerof = OLD.uuid;
        call hs_hosting.asset_build_rbac_system(NEW);
    end if;
end; $$;

/*
    AFTER UPDATE TRIGGER to re-wire the grant structure for a new hs_hosting.asset row.
 */

create or replace function hs_hosting.asset_update_rbac_system_after_update_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_hosting.asset_update_rbac_system(OLD, NEW);
    return NEW;
end; $$;

create or replace trigger update_rbac_system_after_update_tg
    after update on hs_hosting.asset
    for each row
execute procedure hs_hosting.asset_update_rbac_system_after_update_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-hosting-asset-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO hs_hosting.asset permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO hs_hosting.asset permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_hosting.asset'),
                        rbac.global_ADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_hosting.asset INSERT permission to specified role of new global rows.
*/
create or replace function hs_hosting.asset_grants_insert_to_global_admin_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_hosting.asset'),
            rbac.global_ADMIN());
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_asset_z_grants_admin_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure hs_hosting.asset_grants_insert_to_global_admin_tf();

-- granting INSERT permission to hs_hosting.asset ----------------------------

/*
    Grants INSERT INTO hs_hosting.asset permissions to specified role of pre-existing hs_hosting.asset rows.
 */
do language plpgsql $$
    declare
        row hs_hosting.asset;
    begin
        call base.defineContext('create INSERT INTO hs_hosting.asset permissions for pre-exising hs_hosting.asset rows');

        FOR row IN SELECT * FROM hs_hosting.asset
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_hosting.asset'),
                        hs_hosting.asset_ADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_hosting.asset INSERT permission to specified role of new asset rows.
*/
create or replace function hs_hosting.asset_grants_insert_to_asset_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_hosting.asset'),
            hs_hosting.asset_ADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_asset_z_grants_after_insert_tg
    after insert on hs_hosting.asset
    for each row
execute procedure hs_hosting.asset_grants_insert_to_asset_tf();

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO hs_hosting.asset permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO hs_hosting.asset permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_hosting.asset'),
                        rbac.global_GUEST());
            END LOOP;
    end;
$$;

/**
    Grants hs_hosting.asset INSERT permission to specified role of new global rows.
*/
create or replace function hs_hosting.asset_grants_insert_to_global_guest_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_hosting.asset'),
            rbac.global_GUEST());
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_asset_z_grants_guest_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure hs_hosting.asset_grants_insert_to_global_guest_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-hosting-asset-rbac-CHECKING-INSERT-PERMISSION runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_hosting.asset.
*/
create or replace function hs_hosting.asset_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission if rbac.global ADMIN
    if rbac.hasGlobalAdminRole() then
        return NEW;
    end if;
    -- check INSERT permission via direct foreign key: NEW.parentAssetUuid
    if rbac.hasInsertPermission(NEW.parentAssetUuid, 'hs_hosting.asset') then
        return NEW;
    end if;
    -- check INSERT permission for rbac.global anyone
    if NEW.type in ('DOMAIN_SETUP') and true then
        return NEW;
    end if;

    raise exception '[403] insert into hs_hosting.asset values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create or replace trigger asset_insert_permission_check_tg
    before insert on hs_hosting.asset
    for each row
        execute procedure hs_hosting.asset_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-hosting-asset-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_hosting.asset',
    $idName$
        identifier
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-hosting-asset-rbac-RESTRICTED-VIEW runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------
-- generator version: 2
call rbac.generateRbacRestrictedView('hs_hosting.asset',
    $orderBy$
        identifier
$orderBy$,
    $updates$
        version = new.version,
        caption = new.caption,
        config = new.config,
        assignedToAssetUuid = new.assignedToAssetUuid,
        alarmContactUuid = new.alarmContactUuid
$updates$
);
--//


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-hosting-asset-rbac-rebuild runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_hosting.asset after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_hosting.asset', null, <<insert executing global admin user here>>);
--  call hs_hosting.asset_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_hosting.asset.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_hosting.asset.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_hosting.asset_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_hosting.asset;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grant;

    FOR row IN SELECT * FROM hs_hosting.asset LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grant g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_hosting.asset_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grant;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

