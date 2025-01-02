--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:rbactest-customer-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('rbactest.customer');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:rbactest-customer-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('rbactest.customer');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:rbactest-customer-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure rbactest.customer_build_rbac_system(
    NEW rbactest.customer
)
    language plpgsql as $$

declare

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    perform rbac.defineRoleWithGrants(
        rbactest.customer_OWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.global_ADMIN(rbac.unassumed())],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        rbactest.customer_ADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[rbactest.customer_OWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        rbactest.customer_TENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[rbactest.customer_ADMIN(NEW)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new rbactest.customer row.
 */

create or replace function rbactest.customer_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call rbactest.customer_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on rbactest.customer
    for each row
execute procedure rbactest.customer_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:rbactest-customer-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO rbactest.customer permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO rbactest.customer permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'rbactest.customer'),
                        rbac.global_ADMIN());
            END LOOP;
    end;
$$;

/**
    Grants rbactest.customer INSERT permission to specified role of new global rows.
*/
create or replace function rbactest.customer_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'rbactest.customer'),
            rbac.global_ADMIN());
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger customer_z_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure rbactest.customer_grants_insert_to_global_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:rbactest-customer-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to rbactest.customer.
*/
create or replace function rbactest.customer_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission if rbac.global ADMIN
    if rbac.isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into rbactest.customer values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger customer_insert_permission_check_tg
    before insert on rbactest.customer
    for each row
        execute procedure rbactest.customer_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:rbactest-customer-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('rbactest.customer',
    $idName$
        prefix
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:rbactest-customer-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('rbactest.customer',
    $orderBy$
        reference
    $orderBy$,
    $updates$
        reference = new.reference,
        prefix = new.prefix,
        adminUserName = new.adminUserName
    $updates$);
--//


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:rbactest-customer-rbac-rebuild endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table rbactest.customer after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table rbactest.customer', null, <<insert executing global admin user here>>);
--  call rbactest.customer_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `rbactest.customer.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`rbactest.customer.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure rbactest.customer_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row rbactest.customer;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grants;

    FOR row IN SELECT * FROM rbactest.customer LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grants g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL rbactest.customer_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grants;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

