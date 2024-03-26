--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset test-customer-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('test_customer');
--//


-- ============================================================================
--changeset test-customer-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('testCustomer', 'test_customer');
--//


-- ============================================================================
--changeset test-customer-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForTestCustomer(
    NEW test_customer
)
    language plpgsql as $$

declare

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    perform createRoleWithGrants(
        testCustomerOwner(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalAdmin(unassumed())],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        testCustomerAdmin(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[testCustomerOwner(NEW)]
    );

    perform createRoleWithGrants(
        testCustomerTenant(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[testCustomerAdmin(NEW)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new test_customer row.
 */

create or replace function insertTriggerForTestCustomer_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForTestCustomer(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForTestCustomer_tg
    after insert on test_customer
    for each row
execute procedure insertTriggerForTestCustomer_tf();
--//


-- ============================================================================
--changeset test-customer-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO test_customer permissions for the related global rows.
 */
do language plpgsql $$
    declare
        row global;
        permissionUuid uuid;
        roleUuid uuid;
    begin
        call defineContext('create INSERT INTO test_customer permissions for the related global rows');

        FOR row IN SELECT * FROM global
            LOOP
                roleUuid := findRoleId(globalAdmin());
                permissionUuid := createPermission(row.uuid, 'INSERT', 'test_customer');
                call grantPermissionToRole(permissionUuid, roleUuid);
            END LOOP;
    END;
$$;

/**
    Adds test_customer INSERT permission to specified role of new global rows.
*/
create or replace function test_customer_global_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'test_customer'),
            globalAdmin());
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_test_customer_global_insert_tg
    after insert on global
    for each row
execute procedure test_customer_global_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to test_customer,
    where only global-admin has that permission.
*/
create or replace function test_customer_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into test_customer not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger test_customer_insert_permission_check_tg
    before insert on test_customer
    for each row
    when ( not isGlobalAdmin() )
        execute procedure test_customer_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset test-customer-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('test_customer',
    $idName$
        prefix
    $idName$);
--//

-- ============================================================================
--changeset test-customer-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('test_customer',
    $orderBy$
        reference
    $orderBy$,
    $updates$
        reference = new.reference,
        prefix = new.prefix,
        adminUserName = new.adminUserName
    $updates$);
--//

