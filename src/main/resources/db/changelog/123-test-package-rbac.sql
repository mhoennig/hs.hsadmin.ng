--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator at 2024-03-11T11:29:11.625353859.

-- ============================================================================
--changeset test-package-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('test_package');
--//


-- ============================================================================
--changeset test-package-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('testPackage', 'test_package');
--//


-- ============================================================================
--changeset test-package-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForTestPackage(
    NEW test_package
)
    language plpgsql as $$

declare
    newCustomer test_customer;

begin
    call enterTriggerForObjectUuid(NEW.uuid);
    SELECT * FROM test_customer c
        WHERE c.uuid= NEW.customerUuid
     into newCustomer;

    perform createRoleWithGrants(
        testPackageOwner(NEW),
            permissions => array['DELETE', 'UPDATE'],
            incomingSuperRoles => array[testCustomerAdmin(newCustomer)]
    );

    perform createRoleWithGrants(
        testPackageAdmin(NEW),
            incomingSuperRoles => array[testPackageOwner(NEW)]
    );

    perform createRoleWithGrants(
        testPackageTenant(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[testPackageAdmin(NEW)],
            outgoingSubRoles => array[testCustomerTenant(newCustomer)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new test_package row.
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
    after insert on test_package
    for each row
execute procedure insertTriggerForTestPackage_tf();

--//

-- ============================================================================
--changeset test-package-rbac-update-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure updateRbacRulesForTestPackage(
    OLD test_package,
    NEW test_package
)
    language plpgsql as $$

declare
    oldCustomer test_customer;
    newCustomer test_customer;

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM test_customer c
        WHERE c.uuid= OLD.customerUuid
     into oldCustomer;
    SELECT * FROM test_customer c
        WHERE c.uuid= NEW.customerUuid
     into newCustomer;

    if NEW.customerUuid <> OLD.customerUuid then

        call revokePermissionFromRole(findPermissionId(OLD.uuid, 'INSERT'), testCustomerAdmin(oldCustomer));

        call revokeRoleFromRole(testPackageOwner(OLD), testCustomerAdmin(oldCustomer));
        call grantRoleToRole(testPackageOwner(NEW), testCustomerAdmin(newCustomer));

        call revokeRoleFromRole(testCustomerTenant(oldCustomer), testPackageTenant(OLD));
        call grantRoleToRole(testCustomerTenant(newCustomer), testPackageTenant(NEW));

    end if;

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to re-wire the grant structure for a new test_package row.
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
    after update on test_package
    for each row
execute procedure updateTriggerForTestPackage_tf();

--//

-- ============================================================================
--changeset test-package-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO test_package permissions for the related test_customer rows.
 */
do language plpgsql $$
    declare
        row test_customer;
        permissionUuid uuid;
        roleUuid uuid;
    begin
        call defineContext('create INSERT INTO test_package permissions for the related test_customer rows');

        FOR row IN SELECT * FROM test_customer
            LOOP
                roleUuid := findRoleId(testCustomerAdmin(row));
                permissionUuid := createPermission(row.uuid, 'INSERT', 'test_package');
                call grantPermissionToRole(roleUuid, permissionUuid);
            END LOOP;
    END;
$$;

/**
    Adds test_package INSERT permission to specified role of new test_customer rows.
*/
create or replace function test_package_test_customer_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            testCustomerAdmin(NEW),
            createPermission(NEW.uuid, 'INSERT', 'test_package'));
    return NEW;
end; $$;

create trigger test_package_test_customer_insert_tg
    after insert on test_customer
    for each row
execute procedure test_package_test_customer_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to test_package.
*/
create or replace function test_package_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into test_package not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger test_package_insert_permission_check_tg
    before insert on test_package
    for each row
    when ( not hasInsertPermission(NEW.customerUuid, 'INSERT', 'test_package') )
        execute procedure test_package_insert_permission_missing_tf();

--//
-- ============================================================================
--changeset test-package-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('test_package', $idName$
    name
    $idName$);

--//
-- ============================================================================
--changeset test-package-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('test_package',
    'name',
    $updates$
        version = new.version,
        customerUuid = new.customerUuid,
        description = new.description
    $updates$);
--//


