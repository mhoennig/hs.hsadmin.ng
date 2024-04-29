--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


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

    SELECT * FROM test_customer WHERE uuid = NEW.customerUuid    INTO newCustomer;
    assert newCustomer.uuid is not null, format('newCustomer must not be null for NEW.customerUuid = %s', NEW.customerUuid);


    perform createRoleWithGrants(
        testPackageOWNER(NEW),
            permissions => array['DELETE', 'UPDATE'],
            incomingSuperRoles => array[testCustomerADMIN(newCustomer)]
    );

    perform createRoleWithGrants(
        testPackageADMIN(NEW),
            incomingSuperRoles => array[testPackageOWNER(NEW)]
    );

    perform createRoleWithGrants(
        testPackageTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[testPackageADMIN(NEW)],
            outgoingSubRoles => array[testCustomerTENANT(newCustomer)]
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

    SELECT * FROM test_customer WHERE uuid = OLD.customerUuid    INTO oldCustomer;
    assert oldCustomer.uuid is not null, format('oldCustomer must not be null for OLD.customerUuid = %s', OLD.customerUuid);

    SELECT * FROM test_customer WHERE uuid = NEW.customerUuid    INTO newCustomer;
    assert newCustomer.uuid is not null, format('newCustomer must not be null for NEW.customerUuid = %s', NEW.customerUuid);


    if NEW.customerUuid <> OLD.customerUuid then

        call revokeRoleFromRole(testPackageOWNER(OLD), testCustomerADMIN(oldCustomer));
        call grantRoleToRole(testPackageOWNER(NEW), testCustomerADMIN(newCustomer));

        call revokeRoleFromRole(testCustomerTENANT(oldCustomer), testPackageTENANT(OLD));
        call grantRoleToRole(testCustomerTENANT(newCustomer), testPackageTENANT(NEW));

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
--changeset test-package-rbac-GRANTING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to test_customer ----------------------------

/*
    Grants INSERT INTO test_package permissions to specified role of pre-existing test_customer rows.
 */
do language plpgsql $$
    declare
        row test_customer;
    begin
        call defineContext('create INSERT INTO test_package permissions for pre-exising test_customer rows');

        FOR row IN SELECT * FROM test_customer
            -- unconditional for all rows in that table
            LOOP
                call grantPermissionToRole(
                        createPermission(row.uuid, 'INSERT', 'test_package'),
                        testCustomerADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants test_package INSERT permission to specified role of new test_customer rows.
*/
create or replace function new_test_package_grants_insert_to_test_customer_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'test_package'),
            testCustomerADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_test_package_grants_insert_to_test_customer_tg
    after insert on test_customer
    for each row
execute procedure new_test_package_grants_insert_to_test_customer_tf();


-- ============================================================================
--changeset test_package-rbac-CHECKING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to test_package.
*/
create or replace function test_package_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via direct foreign key: NEW.customerUuid
    if hasInsertPermission(NEW.customerUuid, 'test_package') then
        return NEW;
    end if;

    raise exception '[403] insert into test_package not allowed for current subjects % (%)',
            currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger test_package_insert_permission_check_tg
    before insert on test_package
    for each row
        execute procedure test_package_insert_permission_check_tf();
--//


-- ============================================================================
--changeset test-package-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('test_package',
    $idName$
        name
    $idName$);
--//


-- ============================================================================
--changeset test-package-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('test_package',
    $orderBy$
        name
    $orderBy$,
    $updates$
        version = new.version,
        customerUuid = new.customerUuid,
        description = new.description
    $updates$);
--//

