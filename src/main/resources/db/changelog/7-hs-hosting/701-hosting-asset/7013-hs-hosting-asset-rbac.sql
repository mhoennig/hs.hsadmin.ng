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
    newParentServer hs_hosting_asset;
    newBookingItem hs_booking_item;

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_hosting_asset WHERE uuid = NEW.parentAssetUuid    INTO newParentServer;

    SELECT * FROM hs_booking_item WHERE uuid = NEW.bookingItemUuid    INTO newBookingItem;
    assert newBookingItem.uuid is not null, format('newBookingItem must not be null for NEW.bookingItemUuid = %s', NEW.bookingItemUuid);


    perform createRoleWithGrants(
        hsHostingAssetOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[hsBookingItemADMIN(newBookingItem)]
    );

    perform createRoleWithGrants(
        hsHostingAssetADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsHostingAssetOWNER(NEW)]
    );

    perform createRoleWithGrants(
        hsHostingAssetTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsHostingAssetADMIN(NEW)],
            outgoingSubRoles => array[hsBookingItemTENANT(newBookingItem)]
    );

    IF NEW.type = 'CLOUD_SERVER' THEN
    ELSIF NEW.type = 'MANAGED_SERVER' THEN
    ELSIF NEW.type = 'MANAGED_WEBSPACE' THEN
    END IF;

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
--changeset hs-hosting-asset-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO hs_hosting_asset permissions for the related hs_booking_item rows.
 */
do language plpgsql $$
    declare
        row hs_booking_item;
    begin
        call defineContext('create INSERT INTO hs_hosting_asset permissions for the related hs_booking_item rows');

        FOR row IN SELECT * FROM hs_booking_item
            LOOP
                call grantPermissionToRole(
                    createPermission(row.uuid, 'INSERT', 'hs_hosting_asset'),
                    hsBookingItemAGENT(row));
            END LOOP;
    END;
$$;

/**
    Adds hs_hosting_asset INSERT permission to specified role of new hs_booking_item rows.
*/
create or replace function hs_hosting_asset_hs_booking_item_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_hosting_asset'),
            hsBookingItemAGENT(NEW));
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_hs_hosting_asset_hs_booking_item_insert_tg
    after insert on hs_booking_item
    for each row
execute procedure hs_hosting_asset_hs_booking_item_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_hosting_asset,
    where the check is performed by a direct role.

    A direct role is a role depending on a foreign key directly available in the NEW row.
*/
create or replace function hs_hosting_asset_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into hs_hosting_asset not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_hosting_asset_insert_permission_check_tg
    before insert on hs_hosting_asset
    for each row
    when ( not hasInsertPermission(NEW.bookingItemUuid, 'INSERT', 'hs_hosting_asset') )
        execute procedure hs_hosting_asset_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset hs-hosting-asset-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

    call generateRbacIdentityViewFromQuery('hs_hosting_asset',
        $idName$
            SELECT asset.uuid as uuid, bookingItemIV.idName || '-' || cleanIdentifier(asset.identifier) as idName
            FROM hs_hosting_asset asset
            JOIN hs_booking_item_iv bookingItemIV ON bookingItemIV.uuid = asset.bookingItemUuid
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
        config = new.config
    $updates$);
--//

