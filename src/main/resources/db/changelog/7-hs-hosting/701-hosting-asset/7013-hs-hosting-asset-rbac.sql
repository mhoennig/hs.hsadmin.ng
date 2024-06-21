--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-hosting-asset-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_hosting_asset');
--//


-- ============================================================================
--changeset hs-hosting-asset-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsHostingAsset', 'hs_hosting_asset');
--//


-- ============================================================================
--changeset hs-hosting-asset-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsHostingAsset(
    NEW hs_hosting_asset
)
    language plpgsql as $$

declare
    newBookingItem hs_booking_item;
    newAssignedToAsset hs_hosting_asset;
    newAlarmContact hs_office_contact;
    newParentAsset hs_hosting_asset;

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_booking_item WHERE uuid = NEW.bookingItemUuid    INTO newBookingItem;

    SELECT * FROM hs_hosting_asset WHERE uuid = NEW.assignedToAssetUuid    INTO newAssignedToAsset;

    SELECT * FROM hs_office_contact WHERE uuid = NEW.alarmContactUuid    INTO newAlarmContact;

    SELECT * FROM hs_hosting_asset WHERE uuid = NEW.parentAssetUuid    INTO newParentAsset;

    perform createRoleWithGrants(
        hsHostingAssetOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[
            	hsBookingItemADMIN(newBookingItem),
            	hsHostingAssetADMIN(newParentAsset)]
    );

    perform createRoleWithGrants(
        hsHostingAssetADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[
            	hsBookingItemAGENT(newBookingItem),
            	hsHostingAssetAGENT(newParentAsset),
            	hsHostingAssetOWNER(NEW)]
    );

    perform createRoleWithGrants(
        hsHostingAssetAGENT(NEW),
            incomingSuperRoles => array[hsHostingAssetADMIN(NEW)],
            outgoingSubRoles => array[
            	hsHostingAssetTENANT(newAssignedToAsset),
            	hsOfficeContactREFERRER(newAlarmContact)]
    );

    perform createRoleWithGrants(
        hsHostingAssetTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hsHostingAssetAGENT(NEW),
            	hsOfficeContactADMIN(newAlarmContact)],
            outgoingSubRoles => array[
            	hsBookingItemTENANT(newBookingItem),
            	hsHostingAssetTENANT(newParentAsset)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_hosting_asset row.
 */

create or replace function insertTriggerForHsHostingAsset_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsHostingAsset(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsHostingAsset_tg
    after insert on hs_hosting_asset
    for each row
execute procedure insertTriggerForHsHostingAsset_tf();
--//


-- ============================================================================
--changeset hs-hosting-asset-rbac-update-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure updateRbacRulesForHsHostingAsset(
    OLD hs_hosting_asset,
    NEW hs_hosting_asset
)
    language plpgsql as $$
begin

    if NEW.assignedToAssetUuid is distinct from OLD.assignedToAssetUuid
    or NEW.alarmContactUuid is distinct from OLD.alarmContactUuid then
        delete from rbacgrants g where g.grantedbytriggerof = OLD.uuid;
        call buildRbacSystemForHsHostingAsset(NEW);
    end if;
end; $$;

/*
    AFTER INSERT TRIGGER to re-wire the grant structure for a new hs_hosting_asset row.
 */

create or replace function updateTriggerForHsHostingAsset_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call updateRbacRulesForHsHostingAsset(OLD, NEW);
    return NEW;
end; $$;

create trigger updateTriggerForHsHostingAsset_tg
    after update on hs_hosting_asset
    for each row
execute procedure updateTriggerForHsHostingAsset_tf();
--//


-- ============================================================================
--changeset hs-hosting-asset-rbac-GRANTING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to global ----------------------------

/*
    Grants INSERT INTO hs_hosting_asset permissions to specified role of pre-existing global rows.
 */
do language plpgsql $$
    declare
        row global;
    begin
        call defineContext('create INSERT INTO hs_hosting_asset permissions for pre-exising global rows');

        FOR row IN SELECT * FROM global
            -- unconditional for all rows in that table
            LOOP
                call grantPermissionToRole(
                        createPermission(row.uuid, 'INSERT', 'hs_hosting_asset'),
                        globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_hosting_asset INSERT permission to specified role of new global rows.
*/
create or replace function new_hs_hosting_asset_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_hosting_asset'),
            globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_hosting_asset_grants_insert_to_global_tg
    after insert on global
    for each row
execute procedure new_hs_hosting_asset_grants_insert_to_global_tf();

-- granting INSERT permission to hs_hosting_asset ----------------------------

-- Granting INSERT INTO hs_hosting_asset permissions to specified role of pre-existing hs_hosting_asset rows slipped,
-- because there cannot yet be any pre-existing rows in the same table yet.

/**
    Grants hs_hosting_asset INSERT permission to specified role of new hs_hosting_asset rows.
*/
create or replace function new_hs_hosting_asset_grants_insert_to_hs_hosting_asset_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_hosting_asset'),
            hsHostingAssetADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_hosting_asset_grants_insert_to_hs_hosting_asset_tg
    after insert on hs_hosting_asset
    for each row
execute procedure new_hs_hosting_asset_grants_insert_to_hs_hosting_asset_tf();


-- ============================================================================
--changeset hs_hosting_asset-rbac-CHECKING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_hosting_asset.
*/
create or replace function hs_hosting_asset_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT INSERT if global ADMIN
    if isGlobalAdmin() then
        return NEW;
    end if;
    -- check INSERT permission via direct foreign key: NEW.parentAssetUuid
    if hasInsertPermission(NEW.parentAssetUuid, 'hs_hosting_asset') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_hosting_asset values(%) not allowed for current subjects % (%)',
            NEW, currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_hosting_asset_insert_permission_check_tg
    before insert on hs_hosting_asset
    for each row
        execute procedure hs_hosting_asset_insert_permission_check_tf();
--//


-- ============================================================================
--changeset hs-hosting-asset-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_hosting_asset',
    $idName$
        identifier
    $idName$);
--//


-- ============================================================================
--changeset hs-hosting-asset-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_hosting_asset',
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

