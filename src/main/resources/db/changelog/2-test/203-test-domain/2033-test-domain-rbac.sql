--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:test-domain-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('test_domain');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:test-domain-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('testDomain', 'test_domain');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:test-domain-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForTestDomain(
    NEW test_domain
)
    language plpgsql as $$

declare
    newPackage test_package;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM test_package WHERE uuid = NEW.packageUuid    INTO newPackage;
    assert newPackage.uuid is not null, format('newPackage must not be null for NEW.packageUuid = %s', NEW.packageUuid);


    perform rbac.defineRoleWithGrants(
        testDomainOWNER(NEW),
            permissions => array['DELETE', 'UPDATE'],
            incomingSuperRoles => array[testPackageADMIN(newPackage)],
            outgoingSubRoles => array[testPackageTENANT(newPackage)]
    );

    perform rbac.defineRoleWithGrants(
        testDomainADMIN(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[testDomainOWNER(NEW)],
            outgoingSubRoles => array[testPackageTENANT(newPackage)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new test_domain row.
 */

create or replace function insertTriggerForTestDomain_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForTestDomain(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForTestDomain_tg
    after insert on test_domain
    for each row
execute procedure insertTriggerForTestDomain_tf();
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:test-domain-rbac-update-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure updateRbacRulesForTestDomain(
    OLD test_domain,
    NEW test_domain
)
    language plpgsql as $$

declare
    oldPackage test_package;
    newPackage test_package;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM test_package WHERE uuid = OLD.packageUuid    INTO oldPackage;
    assert oldPackage.uuid is not null, format('oldPackage must not be null for OLD.packageUuid = %s', OLD.packageUuid);

    SELECT * FROM test_package WHERE uuid = NEW.packageUuid    INTO newPackage;
    assert newPackage.uuid is not null, format('newPackage must not be null for NEW.packageUuid = %s', NEW.packageUuid);


    if NEW.packageUuid <> OLD.packageUuid then

        call rbac.revokeRoleFromRole(testDomainOWNER(OLD), testPackageADMIN(oldPackage));
        call rbac.grantRoleToRole(testDomainOWNER(NEW), testPackageADMIN(newPackage));

        call rbac.revokeRoleFromRole(testPackageTENANT(oldPackage), testDomainOWNER(OLD));
        call rbac.grantRoleToRole(testPackageTENANT(newPackage), testDomainOWNER(NEW));

        call rbac.revokeRoleFromRole(testPackageTENANT(oldPackage), testDomainADMIN(OLD));
        call rbac.grantRoleToRole(testPackageTENANT(newPackage), testDomainADMIN(NEW));

    end if;

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to re-wire the grant structure for a new test_domain row.
 */

create or replace function updateTriggerForTestDomain_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call updateRbacRulesForTestDomain(OLD, NEW);
    return NEW;
end; $$;

create trigger updateTriggerForTestDomain_tg
    after update on test_domain
    for each row
execute procedure updateTriggerForTestDomain_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:test-domain-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to test_package ----------------------------

/*
    Grants INSERT INTO test_domain permissions to specified role of pre-existing test_package rows.
 */
do language plpgsql $$
    declare
        row test_package;
    begin
        call base.defineContext('create INSERT INTO test_domain permissions for pre-exising test_package rows');

        FOR row IN SELECT * FROM test_package
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'test_domain'),
                        testPackageADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants test_domain INSERT permission to specified role of new test_package rows.
*/
create or replace function new_test_domain_grants_insert_to_test_package_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'test_domain'),
            testPackageADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_test_domain_grants_after_insert_tg
    after insert on test_package
    for each row
execute procedure new_test_domain_grants_insert_to_test_package_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:test_domain-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to test_domain.
*/
create or replace function test_domain_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via direct foreign key: NEW.packageUuid
    if rbac.hasInsertPermission(NEW.packageUuid, 'test_domain') then
        return NEW;
    end if;

    raise exception '[403] insert into test_domain values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger test_domain_insert_permission_check_tg
    before insert on test_domain
    for each row
        execute procedure test_domain_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:test-domain-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('test_domain',
    $idName$
        name
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:test-domain-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('test_domain',
    $orderBy$
        name
    $orderBy$,
    $updates$
        version = new.version,
        packageUuid = new.packageUuid,
        description = new.description
    $updates$);
--//

