--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-hosting-asset-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_hosting_asset');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-hosting-asset-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsHostingAsset', 'hs_hosting_asset');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-hosting-asset-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_hosting_asset_build_rbac_system(
    NEW hs_hosting_asset
)
    language plpgsql as $$

declare
    newBookingItem hs_booking_item;
    newAssignedToAsset hs_hosting_asset;
    newAlarmContact hs_office.contact;
    newParentAsset hs_hosting_asset;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_booking_item WHERE uuid = NEW.bookingItemUuid    INTO newBookingItem;

    SELECT * FROM hs_hosting_asset WHERE uuid = NEW.assignedToAssetUuid    INTO newAssignedToAsset;

    SELECT * FROM hs_office.contact WHERE uuid = NEW.alarmContactUuid    INTO newAlarmContact;

    SELECT * FROM hs_hosting_asset WHERE uuid = NEW.parentAssetUuid    INTO newParentAsset;

    perform rbac.defineRoleWithGrants(
        hsHostingAssetOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[
            	hsBookingItemADMIN(newBookingItem),
            	hsHostingAssetADMIN(newParentAsset),
            	rbac.globalADMIN(rbac.unassumed())],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hsHostingAssetADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[
            	hsBookingItemAGENT(newBookingItem),
            	hsHostingAssetAGENT(newParentAsset),
            	hsHostingAssetOWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hsHostingAssetAGENT(NEW),
            incomingSuperRoles => array[
            	hsHostingAssetADMIN(NEW),
            	hsHostingAssetAGENT(newAssignedToAsset)],
            outgoingSubRoles => array[
            	hsHostingAssetTENANT(newAssignedToAsset),
            	hsOfficeContactREFERRER(newAlarmContact)]
    );

    perform rbac.defineRoleWithGrants(
        hsHostingAssetTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hsHostingAssetAGENT(NEW),
            	hsOfficeContactADMIN(newAlarmContact)],
            outgoingSubRoles => array[
            	hsBookingItemTENANT(newBookingItem),
            	hsHostingAssetTENANT(newParentAsset)]
    );

    IF NEW.type = 'DOMAIN_SETUP' THEN
    END IF;

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_hosting_asset row.
 */

create or replace function hs_hosting_asset_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_hosting_asset_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on hs_hosting_asset
    for each row
execute procedure hs_hosting_asset_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-hosting-asset-rbac-update-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure hs_hosting_asset_update_rbac_system(
    OLD hs_hosting_asset,
    NEW hs_hosting_asset
)
    language plpgsql as $$
begin

    if NEW.assignedToAssetUuid is distinct from OLD.assignedToAssetUuid
    or NEW.alarmContactUuid is distinct from OLD.alarmContactUuid then
        delete from rbac.grants g where g.grantedbytriggerof = OLD.uuid;
        call hs_hosting_asset_build_rbac_system(NEW);
    end if;
end; $$;

/*
    AFTER UPDATE TRIGGER to re-wire the grant structure for a new hs_hosting_asset row.
 */

create or replace function hs_hosting_asset_update_rbac_system_after_update_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_hosting_asset_update_rbac_system(OLD, NEW);
    return NEW;
end; $$;

create trigger update_rbac_system_after_update_tg
    after update on hs_hosting_asset
    for each row
execute procedure hs_hosting_asset_update_rbac_system_after_update_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-hosting-asset-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_hosting_asset',
    $idName$
        identifier
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-hosting-asset-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_hosting_asset',
    $orderBy$
        identifier
    $orderBy$,
    $updates$
        version = new.version,
        caption = new.caption,
        config = new.config,
        assignedToAssetUuid = new.assignedToAssetUuid,
        alarmContactUuid = new.alarmContactUuid
    $updates$);
--//

