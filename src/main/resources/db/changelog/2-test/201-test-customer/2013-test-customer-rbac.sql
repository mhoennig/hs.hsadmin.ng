--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:test-customer-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('test_customer');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:test-customer-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('testCustomer', 'test_customer');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:test-customer-rbac-insert-trigger endDelimiter:--//
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
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    perform rbac.defineRoleWithGrants(
        testCustomerOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.globalADMIN(rbac.unassumed())],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        testCustomerADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[testCustomerOWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        testCustomerTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[testCustomerADMIN(NEW)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
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
--changeset InsertTriggerGenerator:test-customer-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO test_customer permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO test_customer permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'test_customer'),
                        rbac.globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants test_customer INSERT permission to specified role of new global rows.
*/
create or replace function rbac.new_test_customer_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'test_customer'),
            rbac.globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_test_customer_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure rbac.new_test_customer_grants_insert_to_global_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:test_customer-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to test_customer.
*/
create or replace function test_customer_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT INSERT if rbac.global ADMIN
    if rbac.isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into test_customer values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger test_customer_insert_permission_check_tg
    before insert on test_customer
    for each row
        execute procedure test_customer_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:test-customer-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('test_customer',
    $idName$
        prefix
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:test-customer-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('test_customer',
    $orderBy$
        reference
    $orderBy$,
    $updates$
        reference = new.reference,
        prefix = new.prefix,
        adminUserName = new.adminUserName
    $updates$);
--//

