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
call rbac.generateRbacRoleDescriptors('testCustomer', 'rbactest.customer');
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
                        rbac.globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants rbactest.customer INSERT permission to specified role of new global rows.
*/
create or replace function rbactest.new_customer_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'rbactest.customer'),
            rbac.globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_customer_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure rbactest.new_customer_grants_insert_to_global_tf();


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

