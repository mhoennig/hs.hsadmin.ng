--liquibase formatted sql

-- ============================================================================
--changeset test-domain-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('test_domain');
--//


-- ============================================================================
--changeset test-domain-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('testDomain', 'test_domain');

create or replace function createTestDomainTenantRoleIfNotExists(domain test_domain)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    domainTenantRoleDesc RbacRoleDescriptor;
    domainTenantRoleUuid uuid;
begin
    domainTenantRoleDesc = testdomainTenant(domain);
    domainTenantRoleUuid = findRoleId(domainTenantRoleDesc);
    if domainTenantRoleUuid is not null then
        return domainTenantRoleUuid;
    end if;

    return createRole(
        domainTenantRoleDesc,
        grantingPermissions(forObjectUuid => domain.uuid, permitOps => array ['view']),
        beneathRole(testdomainAdmin(domain))
        );
end; $$;
--//


-- ============================================================================
--changeset test-domain-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the roles and their assignments for a new domain for the AFTER INSERT TRIGGER.
 */

create or replace function createRbacRulesForTestDomain()
    returns trigger
    language plpgsql
    strict as $$
declare
    parentPackage       test_package;
    domainOwnerRoleId uuid;
    domainAdminRoleId uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    select * from test_package where uuid = NEW.packageUuid into parentPackage;

    -- an owner role is created and assigned to the package's admin group
    domainOwnerRoleId = createRole(
        testdomainOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(testPackageAdmin(parentPackage))
        );

    -- and a domain admin role is created and assigned to the domain owner as well
    domainAdminRoleId = createRole(
        testdomainAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit']),
        beneathRole(domainOwnerRoleId),
        beingItselfA(testPackageTenant(parentPackage))
        );

    -- a tenent role is only created on demand

    return NEW;
end; $$;


/*
    An AFTER INSERT TRIGGER which creates the role structure for a new domain.
 */
drop trigger if exists createRbacRulesForTestDomain_Trigger on test_domain;
create trigger createRbacRulesForTestDomain_Trigger
    after insert
    on test_domain
    for each row
execute procedure createRbacRulesForTestDomain();
--//


-- ============================================================================
--changeset test-domain-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('test_domain', $idName$
    target.name
    $idName$);
--//


-- ============================================================================
--changeset test-domain-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the customer main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
drop view if exists test_domain_rv;
create or replace view test_domain_rv as
select target.*
    from test_domain as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'domain', currentSubjectsUuids()));
grant all privileges on test_domain_rv to restricted;
--//
