--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-coopassettx-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office.coopassettx');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-coopassettx-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_office.coopassettx');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-coopassettx-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_office.coopassettx_build_rbac_system(
    NEW hs_office.coopassettx
)
    language plpgsql as $$

declare
    newMembership hs_office.membership;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office.membership WHERE uuid = NEW.membershipUuid    INTO newMembership;
    assert newMembership.uuid is not null, format('newMembership must not be null for NEW.membershipUuid = %s of hs_office.coopassettx', NEW.membershipUuid);

    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'SELECT'), hs_office.membership_AGENT(newMembership));
    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'UPDATE'), hs_office.membership_ADMIN(newMembership));

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office.coopassettx row.
 */

create or replace function hs_office.coopassettx_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.coopassettx_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on hs_office.coopassettx
    for each row
execute procedure hs_office.coopassettx_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-coopassettx-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to hs_office.membership ----------------------------

/*
    Grants INSERT INTO hs_office.coopassettx permissions to specified role of pre-existing hs_office.membership rows.
 */
do language plpgsql $$
    declare
        row hs_office.membership;
    begin
        call base.defineContext('create INSERT INTO hs_office.coopassettx permissions for pre-exising hs_office.membership rows');

        FOR row IN SELECT * FROM hs_office.membership
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office.coopassettx'),
                        hs_office.membership_ADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_office.coopassettx INSERT permission to specified role of new membership rows.
*/
create or replace function hs_office.coopassettx_grants_insert_to_membership_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office.coopassettx'),
            hs_office.membership_ADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger coopassettx_z_grants_after_insert_tg
    after insert on hs_office.membership
    for each row
execute procedure hs_office.coopassettx_grants_insert_to_membership_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-coopassettx-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office.coopassettx.
*/
create or replace function hs_office.coopassettx_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via direct foreign key: NEW.membershipUuid
    if rbac.hasInsertPermission(NEW.membershipUuid, 'hs_office.coopassettx') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office.coopassettx values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger coopassettx_insert_permission_check_tg
    before insert on hs_office.coopassettx
    for each row
        execute procedure hs_office.coopassettx_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-coopassettx-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office.coopassettx',
    $idName$
        reference
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-coopassettx-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office.coopassettx',
    $orderBy$
        reference
    $orderBy$,
    $updates$
        comment = new.comment
    $updates$);
--//


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-office-coopassettx-rbac-rebuild endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_office.coopassettx after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_office.coopassettx', null, <<insert executing global admin user here>>);
--  call hs_office.coopassettx_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_office.coopassettx.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_office.coopassettx.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_office.coopassettx_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_office.coopassettx;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grants;

    FOR row IN SELECT * FROM hs_office.coopassettx LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grants g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_office.coopassettx_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grants;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

