--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-booking-item-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_booking_item');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-booking-item-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsBookingItem', 'hs_booking_item');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-booking-item-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_booking_item_build_rbac_system(
    NEW hs_booking_item
)
    language plpgsql as $$

declare
    newProject hs_booking_project;
    newParentItem hs_booking_item;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_booking_project WHERE uuid = NEW.projectUuid    INTO newProject;

    SELECT * FROM hs_booking_item WHERE uuid = NEW.parentItemUuid    INTO newParentItem;

    perform rbac.defineRoleWithGrants(
        hsBookingItemOWNER(NEW),
            incomingSuperRoles => array[
            	hsBookingItemAGENT(newParentItem),
            	hsBookingProjectAGENT(newProject)]
    );

    perform rbac.defineRoleWithGrants(
        hsBookingItemADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsBookingItemOWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hsBookingItemAGENT(NEW),
            incomingSuperRoles => array[hsBookingItemADMIN(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hsBookingItemTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsBookingItemAGENT(NEW)],
            outgoingSubRoles => array[
            	hsBookingItemTENANT(newParentItem),
            	hsBookingProjectTENANT(newProject)]
    );



    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'DELETE'), rbac.globalAdmin());

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_booking_item row.
 */

create or replace function hs_booking_item_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_booking_item_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on hs_booking_item
    for each row
execute procedure hs_booking_item_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-booking-item-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO hs_booking_item permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO hs_booking_item permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_booking_item'),
                        rbac.globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_booking_item INSERT permission to specified role of new global rows.
*/
create or replace function new_hsbk_item_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_booking_item'),
            rbac.globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_booking_item_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure new_hsbk_item_grants_insert_to_global_tf();

-- granting INSERT permission to hs_booking_project ----------------------------

/*
    Grants INSERT INTO hs_booking_item permissions to specified role of pre-existing hs_booking_project rows.
 */
do language plpgsql $$
    declare
        row hs_booking_project;
    begin
        call base.defineContext('create INSERT INTO hs_booking_item permissions for pre-exising hs_booking_project rows');

        FOR row IN SELECT * FROM hs_booking_project
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_booking_item'),
                        hsBookingProjectADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_booking_item INSERT permission to specified role of new hs_booking_project rows.
*/
create or replace function new_hsbk_item_grants_insert_to_hsbk_project_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_booking_item'),
            hsBookingProjectADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_booking_item_grants_after_insert_tg
    after insert on hs_booking_project
    for each row
execute procedure new_hsbk_item_grants_insert_to_hsbk_project_tf();

-- granting INSERT permission to hs_booking_item ----------------------------

-- Granting INSERT INTO hs_hosting_asset permissions to specified role of pre-existing hs_hosting_asset rows slipped,
-- because there cannot yet be any pre-existing rows in the same table yet.

/**
    Grants hs_booking_item INSERT permission to specified role of new hs_booking_item rows.
*/
create or replace function new_hsbk_item_grants_insert_to_hsbk_item_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_booking_item'),
            hsBookingItemADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_booking_item_grants_after_insert_tg
    after insert on hs_booking_item
    for each row
execute procedure new_hsbk_item_grants_insert_to_hsbk_item_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-booking-item-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_booking_item.
*/
create or replace function hs_booking_item_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission if rbac.global ADMIN
    if rbac.isGlobalAdmin() then
        return NEW;
    end if;
    -- check INSERT permission via direct foreign key: NEW.projectUuid
    if rbac.hasInsertPermission(NEW.projectUuid, 'hs_booking_item') then
        return NEW;
    end if;
    -- check INSERT permission via direct foreign key: NEW.parentItemUuid
    if rbac.hasInsertPermission(NEW.parentItemUuid, 'hs_booking_item') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_booking_item values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger hs_booking_item_insert_permission_check_tg
    before insert on hs_booking_item
    for each row
        execute procedure hs_booking_item_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-booking-item-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_booking_item',
    $idName$
        caption
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-booking-item-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_booking_item',
    $orderBy$
        validity
    $orderBy$,
    $updates$
        version = new.version,
        caption = new.caption,
        validity = new.validity,
        resources = new.resources
    $updates$);
--//

