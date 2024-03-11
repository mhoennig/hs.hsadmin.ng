--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator at 2024-03-11T11:29:11.584886824.

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
            userUuids => array[currentUserUuid()],
            incomingSuperRoles => array[globalAdmin(unassumed())]
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

/**
    Checks if the user or assumed roles are allowed to insert a row to test_customer.
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
    -- As there is no explicit INSERT grant specified for this table,
    -- only global admins are allowed to insert any rows.
    when ( not isGlobalAdmin() )
        execute procedure test_customer_insert_permission_missing_tf();

--//
-- ============================================================================
--changeset test-customer-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('test_customer', $idName$
    prefix
    $idName$);

--//
-- ============================================================================
--changeset test-customer-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('test_customer',
    'reference',
    $updates$
        reference = new.reference,
        prefix = new.prefix,
        adminUserName = new.adminUserName
    $updates$);
--//


