--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:rbactest-domain-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('rbactest.domain');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:rbactest-domain-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('rbactest.domain');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:rbactest-domain-rbac-insert-trigger runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure rbactest.domain_build_rbac_system(
    NEW rbactest.domain
)
    language plpgsql as $$

declare
    newPackage rbactest.package;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM rbactest.package WHERE uuid = NEW.packageUuid    INTO newPackage;
    assert newPackage.uuid is not null, format('newPackage must not be null for NEW.packageUuid = %s of rbactest.domain', NEW.packageUuid);


    perform rbac.defineRoleWithGrants(
        rbactest.domain_OWNER(NEW),
            permissions => array['DELETE', 'UPDATE'],
            incomingSuperRoles => array[rbactest.package_ADMIN(newPackage)],
            outgoingSubRoles => array[rbactest.package_TENANT(newPackage)]
    );

    perform rbac.defineRoleWithGrants(
        rbactest.domain_ADMIN(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[rbactest.domain_OWNER(NEW)],
            outgoingSubRoles => array[rbactest.package_TENANT(newPackage)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new rbactest.domain row.
 */

create or replace function rbactest.domain_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call rbactest.domain_build_rbac_system(NEW);
    return NEW;
end; $$;

create or replace trigger build_rbac_system_after_insert_tg
    after insert on rbactest.domain
    for each row
execute procedure rbactest.domain_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:rbactest-domain-rbac-update-trigger runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure rbactest.domain_update_rbac_system(
    OLD rbactest.domain,
    NEW rbactest.domain
)
    language plpgsql as $$

declare
    oldPackage rbactest.package;
    newPackage rbactest.package;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM rbactest.package WHERE uuid = OLD.packageUuid    INTO oldPackage;
    assert oldPackage.uuid is not null, format('oldPackage must not be null for OLD.packageUuid = %s of rbactest.domain', OLD.packageUuid);

    SELECT * FROM rbactest.package WHERE uuid = NEW.packageUuid    INTO newPackage;
    assert newPackage.uuid is not null, format('newPackage must not be null for NEW.packageUuid = %s of rbactest.domain', NEW.packageUuid);


    if NEW.packageUuid <> OLD.packageUuid then

        call rbac.revokeRoleFromRole(rbactest.domain_OWNER(OLD), rbactest.package_ADMIN(oldPackage));
        call rbac.grantRoleToRole(rbactest.domain_OWNER(NEW), rbactest.package_ADMIN(newPackage));

        call rbac.revokeRoleFromRole(rbactest.package_TENANT(oldPackage), rbactest.domain_OWNER(OLD));
        call rbac.grantRoleToRole(rbactest.package_TENANT(newPackage), rbactest.domain_OWNER(NEW));

        call rbac.revokeRoleFromRole(rbactest.package_TENANT(oldPackage), rbactest.domain_ADMIN(OLD));
        call rbac.grantRoleToRole(rbactest.package_TENANT(newPackage), rbactest.domain_ADMIN(NEW));

    end if;

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER UPDATE TRIGGER to re-wire the grant structure for a new rbactest.domain row.
 */

create or replace function rbactest.domain_update_rbac_system_after_update_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call rbactest.domain_update_rbac_system(OLD, NEW);
    return NEW;
end; $$;

create or replace trigger update_rbac_system_after_update_tg
    after update on rbactest.domain
    for each row
execute procedure rbactest.domain_update_rbac_system_after_update_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:rbactest-domain-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbactest.package ----------------------------

/*
    Grants INSERT INTO rbactest.domain permissions to specified role of pre-existing rbactest.package rows.
 */
do language plpgsql $$
    declare
        row rbactest.package;
    begin
        call base.defineContext('create INSERT INTO rbactest.domain permissions for pre-exising rbactest.package rows');

        FOR row IN SELECT * FROM rbactest.package
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'rbactest.domain'),
                        rbactest.package_ADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants rbactest.domain INSERT permission to specified role of new package rows.
*/
create or replace function rbactest.domain_grants_insert_to_package_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'rbactest.domain'),
            rbactest.package_ADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger domain_z_grants_after_insert_tg
    after insert on rbactest.package
    for each row
execute procedure rbactest.domain_grants_insert_to_package_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:rbactest-domain-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to rbactest.domain.
*/
create or replace function rbactest.domain_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via direct foreign key: NEW.packageUuid
    if rbac.hasInsertPermission(NEW.packageUuid, 'rbactest.domain') then
        return NEW;
    end if;

    raise exception '[403] insert into rbactest.domain values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger domain_insert_permission_check_tg
    before insert on rbactest.domain
    for each row
        execute procedure rbactest.domain_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:rbactest-domain-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('rbactest.domain',
    $idName$
        name
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:rbactest-domain-rbac-RESTRICTED-VIEW runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('rbactest.domain',
    $orderBy$
        name
    $orderBy$,
    $updates$
        version = new.version,
        packageUuid = new.packageUuid,
        description = new.description
    $updates$);
--//


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:rbactest-domain-rbac-rebuild runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table rbactest.domain after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table rbactest.domain', null, <<insert executing global admin user here>>);
--  call rbactest.domain_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `rbactest.domain.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`rbactest.domain.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure rbactest.domain_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row rbactest.domain;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grant;

    FOR row IN SELECT * FROM rbactest.domain LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grant g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL rbactest.domain_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grant;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

