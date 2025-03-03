--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-booking-item-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_booking.item');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-booking-item-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_booking.item');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-booking-item-rbac-insert-trigger runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_booking.item_build_rbac_system(
    NEW hs_booking.item
)
    language plpgsql as $$

declare
    newProject hs_booking.project;
    newParentItem hs_booking.item;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_booking.project WHERE uuid = NEW.projectUuid    INTO newProject;

    SELECT * FROM hs_booking.item WHERE uuid = NEW.parentItemUuid    INTO newParentItem;

    perform rbac.defineRoleWithGrants(
        hs_booking.item_OWNER(NEW),
            incomingSuperRoles => array[
            	hs_booking.item_AGENT(newParentItem),
            	hs_booking.project_AGENT(newProject)]
    );

    perform rbac.defineRoleWithGrants(
        hs_booking.item_ADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hs_booking.item_OWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hs_booking.item_AGENT(NEW),
            incomingSuperRoles => array[hs_booking.item_ADMIN(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hs_booking.item_TENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hs_booking.item_AGENT(NEW)],
            outgoingSubRoles => array[
            	hs_booking.item_TENANT(newParentItem),
            	hs_booking.project_TENANT(newProject)]
    );



    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'DELETE'), rbac.global_ADMIN());

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_booking.item row.
 */

create or replace function hs_booking.item_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_booking.item_build_rbac_system(NEW);
    return NEW;
end; $$;

create or replace trigger build_rbac_system_after_insert_tg
    after insert on hs_booking.item
    for each row
execute procedure hs_booking.item_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-booking-item-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO hs_booking.item permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO hs_booking.item permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_booking.item'),
                        rbac.global_ADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_booking.item INSERT permission to specified role of new global rows.
*/
create or replace function hs_booking.item_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_booking.item'),
            rbac.global_ADMIN());
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger item_z_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure hs_booking.item_grants_insert_to_global_tf();

-- granting INSERT permission to hs_booking.project ----------------------------

/*
    Grants INSERT INTO hs_booking.item permissions to specified role of pre-existing hs_booking.project rows.
 */
do language plpgsql $$
    declare
        row hs_booking.project;
    begin
        call base.defineContext('create INSERT INTO hs_booking.item permissions for pre-exising hs_booking.project rows');

        FOR row IN SELECT * FROM hs_booking.project
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_booking.item'),
                        hs_booking.project_ADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_booking.item INSERT permission to specified role of new project rows.
*/
create or replace function hs_booking.item_grants_insert_to_project_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_booking.item'),
            hs_booking.project_ADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger item_z_grants_after_insert_tg
    after insert on hs_booking.project
    for each row
execute procedure hs_booking.item_grants_insert_to_project_tf();

-- granting INSERT permission to hs_booking.item ----------------------------

-- Granting INSERT INTO hs_hosting.asset permissions to specified role of pre-existing hs_hosting.asset rows slipped,
-- because there cannot yet be any pre-existing rows in the same table yet.

/**
    Grants hs_booking.item INSERT permission to specified role of new item rows.
*/
create or replace function hs_booking.item_grants_insert_to_item_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_booking.item'),
            hs_booking.item_ADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger item_z_grants_after_insert_tg
    after insert on hs_booking.item
    for each row
execute procedure hs_booking.item_grants_insert_to_item_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-booking-item-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_booking.item.
*/
create or replace function hs_booking.item_insert_permission_check_tf()
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
    if rbac.hasInsertPermission(NEW.projectUuid, 'hs_booking.item') then
        return NEW;
    end if;
    -- check INSERT permission via direct foreign key: NEW.parentItemUuid
    if rbac.hasInsertPermission(NEW.parentItemUuid, 'hs_booking.item') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_booking.item values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger item_insert_permission_check_tg
    before insert on hs_booking.item
    for each row
        execute procedure hs_booking.item_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-booking-item-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_booking.item',
    $idName$
        caption
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-booking-item-rbac-RESTRICTED-VIEW runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_booking.item',
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


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-booking-item-rbac-rebuild runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_booking.item after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_booking.item', null, <<insert executing global admin user here>>);
--  call hs_booking.item_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_booking.item.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_booking.item.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_booking.item_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_booking.item;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grant;

    FOR row IN SELECT * FROM hs_booking.item LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grant g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_booking.item_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grant;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

