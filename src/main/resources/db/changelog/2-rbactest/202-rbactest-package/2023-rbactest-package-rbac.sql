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
call rbac.generateRbacRoleDescriptors('testPackage', 'rbactest.package');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:rbactest-package-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForTestPackage(
    NEW rbactest.package
)
    language plpgsql as $$

declare
    newCustomer rbactest.customer;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM rbactest.customer WHERE uuid = NEW.customerUuid    INTO newCustomer;
    assert newCustomer.uuid is not null, format('newCustomer must not be null for NEW.customerUuid = %s', NEW.customerUuid);


    perform rbac.defineRoleWithGrants(
        testPackageOWNER(NEW),
            permissions => array['DELETE', 'UPDATE'],
            incomingSuperRoles => array[testCustomerADMIN(newCustomer)]
    );

    perform rbac.defineRoleWithGrants(
        testPackageADMIN(NEW),
            incomingSuperRoles => array[testPackageOWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        testPackageTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[testPackageADMIN(NEW)],
            outgoingSubRoles => array[testCustomerTENANT(newCustomer)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new rbactest.package row.
 */

create or replace function insertTriggerForTestPackage_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForTestPackage(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForTestPackage_tg
    after insert on rbactest.package
    for each row
execute procedure insertTriggerForTestPackage_tf();
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:rbactest-package-rbac-update-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure updateRbacRulesForTestPackage(
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
    assert oldCustomer.uuid is not null, format('oldCustomer must not be null for OLD.customerUuid = %s', OLD.customerUuid);

    SELECT * FROM rbactest.customer WHERE uuid = NEW.customerUuid    INTO newCustomer;
    assert newCustomer.uuid is not null, format('newCustomer must not be null for NEW.customerUuid = %s', NEW.customerUuid);


    if NEW.customerUuid <> OLD.customerUuid then

        call rbac.revokeRoleFromRole(testPackageOWNER(OLD), testCustomerADMIN(oldCustomer));
        call rbac.grantRoleToRole(testPackageOWNER(NEW), testCustomerADMIN(newCustomer));

        call rbac.revokeRoleFromRole(testCustomerTENANT(oldCustomer), testPackageTENANT(OLD));
        call rbac.grantRoleToRole(testCustomerTENANT(newCustomer), testPackageTENANT(NEW));

    end if;

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to re-wire the grant structure for a new rbactest.package row.
 */

create or replace function updateTriggerForTestPackage_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call updateRbacRulesForTestPackage(OLD, NEW);
    return NEW;
end; $$;

create trigger updateTriggerForTestPackage_tg
    after update on rbactest.package
    for each row
execute procedure updateTriggerForTestPackage_tf();
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
                        testCustomerADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants rbactest.package INSERT permission to specified role of new customer rows.
*/
create or replace function rbactest.new_package_grants_insert_to_customer_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'rbactest.package'),
            testCustomerADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_package_grants_after_insert_tg
    after insert on rbactest.customer
    for each row
execute procedure rbactest.new_package_grants_insert_to_customer_tf();


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

