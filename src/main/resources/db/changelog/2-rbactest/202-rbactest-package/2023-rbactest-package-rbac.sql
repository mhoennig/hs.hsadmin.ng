--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:rbactest-package-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('rbactest.package');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:rbactest-package-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('rbactest.package');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:rbactest-package-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure rbactest.package_build_rbac_system(
    NEW rbactest.package
)
    language plpgsql as $$

declare
    newCustomer rbactest.customer;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM rbactest.customer WHERE uuid = NEW.customerUuid    INTO newCustomer;
    assert newCustomer.uuid is not null, format('newCustomer must not be null for NEW.customerUuid = %s of package', NEW.customerUuid);


    perform rbac.defineRoleWithGrants(
        rbactest.package_OWNER(NEW),
            permissions => array['DELETE', 'UPDATE'],
            incomingSuperRoles => array[rbactest.customer_ADMIN(newCustomer)]
    );

    perform rbac.defineRoleWithGrants(
        rbactest.package_ADMIN(NEW),
            incomingSuperRoles => array[rbactest.package_OWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        rbactest.package_TENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[rbactest.package_ADMIN(NEW)],
            outgoingSubRoles => array[rbactest.customer_TENANT(newCustomer)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new rbactest.package row.
 */

create or replace function rbactest.package_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call rbactest.package_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on rbactest.package
    for each row
execute procedure rbactest.package_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:rbactest-package-rbac-update-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure rbactest.package_update_rbac_system(
    OLD rbactest.package,
    NEW rbactest.package
)
    language plpgsql as $$

declare
    oldCustomer rbactest.customer;
    newCustomer rbactest.customer;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM rbactest.customer WHERE uuid = OLD.customerUuid    INTO oldCustomer;
    assert oldCustomer.uuid is not null, format('oldCustomer must not be null for OLD.customerUuid = %s of package', OLD.customerUuid);

    SELECT * FROM rbactest.customer WHERE uuid = NEW.customerUuid    INTO newCustomer;
    assert newCustomer.uuid is not null, format('newCustomer must not be null for NEW.customerUuid = %s of package', NEW.customerUuid);


    if NEW.customerUuid <> OLD.customerUuid then

        call rbac.revokeRoleFromRole(rbactest.package_OWNER(OLD), rbactest.customer_ADMIN(oldCustomer));
        call rbac.grantRoleToRole(rbactest.package_OWNER(NEW), rbactest.customer_ADMIN(newCustomer));

        call rbac.revokeRoleFromRole(rbactest.customer_TENANT(oldCustomer), rbactest.package_TENANT(OLD));
        call rbac.grantRoleToRole(rbactest.customer_TENANT(newCustomer), rbactest.package_TENANT(NEW));

    end if;

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER UPDATE TRIGGER to re-wire the grant structure for a new rbactest.package row.
 */

create or replace function rbactest.package_update_rbac_system_after_update_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call rbactest.package_update_rbac_system(OLD, NEW);
    return NEW;
end; $$;

create trigger update_rbac_system_after_update_tg
    after update on rbactest.package
    for each row
execute procedure rbactest.package_update_rbac_system_after_update_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:rbactest-package-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbactest.customer ----------------------------

/*
    Grants INSERT INTO rbactest.package permissions to specified role of pre-existing rbactest.customer rows.
 */
do language plpgsql $$
    declare
        row rbactest.customer;
    begin
        call base.defineContext('create INSERT INTO rbactest.package permissions for pre-exising rbactest.customer rows');

        FOR row IN SELECT * FROM rbactest.customer
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'rbactest.package'),
                        rbactest.customer_ADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants rbactest.package INSERT permission to specified role of new customer rows.
*/
create or replace function rbactest.package_grants_insert_to_customer_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'rbactest.package'),
            rbactest.customer_ADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger package_z_grants_after_insert_tg
    after insert on rbactest.customer
    for each row
execute procedure rbactest.package_grants_insert_to_customer_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:rbactest-package-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to rbactest.package.
*/
create or replace function rbactest.package_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via direct foreign key: NEW.customerUuid
    if rbac.hasInsertPermission(NEW.customerUuid, 'rbactest.package') then
        return NEW;
    end if;

    raise exception '[403] insert into rbactest.package values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger package_insert_permission_check_tg
    before insert on rbactest.package
    for each row
        execute procedure rbactest.package_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:rbactest-package-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('rbactest.package',
    $idName$
        name
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:rbactest-package-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('rbactest.package',
    $orderBy$
        name
    $orderBy$,
    $updates$
        version = new.version,
        customerUuid = new.customerUuid,
        description = new.description
    $updates$);
--//

