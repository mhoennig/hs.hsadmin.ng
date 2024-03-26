--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset test-domain-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('test_domain');
--//


-- ============================================================================
--changeset test-domain-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('testDomain', 'test_domain');
--//


-- ============================================================================
--changeset test-domain-rbac-insert-trigger:1 endDelimiter:--//
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
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM test_package WHERE uuid = NEW.packageUuid    INTO newPackage;
    assert newPackage.uuid is not null, format('newPackage must not be null for NEW.packageUuid = %s', NEW.packageUuid);


    perform createRoleWithGrants(
        testDomainOwner(NEW),
            permissions => array['DELETE', 'UPDATE'],
            incomingSuperRoles => array[testPackageAdmin(newPackage)],
            outgoingSubRoles => array[testPackageTenant(newPackage)]
    );

    perform createRoleWithGrants(
        testDomainAdmin(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[testDomainOwner(NEW)],
            outgoingSubRoles => array[testPackageTenant(newPackage)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
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
--changeset test-domain-rbac-update-trigger:1 endDelimiter:--//
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
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM test_package WHERE uuid = OLD.packageUuid    INTO oldPackage;
    assert oldPackage.uuid is not null, format('oldPackage must not be null for OLD.packageUuid = %s', OLD.packageUuid);

    SELECT * FROM test_package WHERE uuid = NEW.packageUuid    INTO newPackage;
    assert newPackage.uuid is not null, format('newPackage must not be null for NEW.packageUuid = %s', NEW.packageUuid);


    if NEW.packageUuid <> OLD.packageUuid then

        call revokeRoleFromRole(testDomainOwner(OLD), testPackageAdmin(oldPackage));
        call grantRoleToRole(testDomainOwner(NEW), testPackageAdmin(newPackage));

        call revokeRoleFromRole(testPackageTenant(oldPackage), testDomainOwner(OLD));
        call grantRoleToRole(testPackageTenant(newPackage), testDomainOwner(NEW));

        call revokeRoleFromRole(testPackageTenant(oldPackage), testDomainAdmin(OLD));
        call grantRoleToRole(testPackageTenant(newPackage), testDomainAdmin(NEW));

    end if;

    call leaveTriggerForObjectUuid(NEW.uuid);
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
--changeset test-domain-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO test_domain permissions for the related test_package rows.
 */
do language plpgsql $$
    declare
        row test_package;
        permissionUuid uuid;
        roleUuid uuid;
    begin
        call defineContext('create INSERT INTO test_domain permissions for the related test_package rows');

        FOR row IN SELECT * FROM test_package
            LOOP
                roleUuid := findRoleId(testPackageAdmin(row));
                permissionUuid := createPermission(row.uuid, 'INSERT', 'test_domain');
                call grantPermissionToRole(permissionUuid, roleUuid);
            END LOOP;
    END;
$$;

/**
    Adds test_domain INSERT permission to specified role of new test_package rows.
*/
create or replace function test_domain_test_package_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'test_domain'),
            testPackageAdmin(NEW));
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_test_domain_test_package_insert_tg
    after insert on test_package
    for each row
execute procedure test_domain_test_package_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to test_domain,
    where the check is performed by a direct role.

    A direct role is a role depending on a foreign key directly available in the NEW row.
*/
create or replace function test_domain_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into test_domain not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger test_domain_insert_permission_check_tg
    before insert on test_domain
    for each row
    when ( not hasInsertPermission(NEW.packageUuid, 'INSERT', 'test_domain') )
        execute procedure test_domain_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset test-domain-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('test_domain',
    $idName$
        name
    $idName$);
--//

-- ============================================================================
--changeset test-domain-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('test_domain',
    $orderBy$
        name
    $orderBy$,
    $updates$
        version = new.version,
        packageUuid = new.packageUuid,
        description = new.description
    $updates$);
--//

