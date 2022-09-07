--liquibase formatted sql

-- ============================================================================
--changeset test-package-rbac-CREATE-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the related RbacObject through a BEFORE INSERT TRIGGER.
 */
drop trigger if exists createRbacObjectFortest_domain_Trigger on test_domain;
create trigger createRbacObjectFortest_domain_Trigger
    before insert
    on test_domain
    for each row
execute procedure createRbacObject();
--//


-- ============================================================================
--changeset test-domain-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function testdomainOwner(uu test_domain)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('test_domain', uu.uuid, 'owner');
end; $$;

create or replace function testdomainAdmin(uu test_domain)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('test_domain', uu.uuid, 'admin');
end; $$;

create or replace function testdomainTenant(uu test_domain)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('test_domain', uu.uuid, 'tenant');
end; $$;

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
--changeset test-domain-rbac-ROLES-REMOVAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Deletes the roles and their assignments of a deleted domain for the BEFORE DELETE TRIGGER.
 */

create or replace function deleteRbacRulesForTestDomain()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        call deleteRole(findRoleId(testdomainOwner(OLD)));
        call deleteRole(findRoleId(testdomainAdmin(OLD)));
        call deleteRole(findRoleId(testdomainTenant(OLD)));
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
end; $$;

/*
    An BEFORE DELETE TRIGGER which deletes the role structure of a domain.
 */

drop trigger if exists deleteRbacRulesForTestDomain_Trigger on test_package;
create trigger deleteRbacRulesForTestDomain_Trigger
    before delete
    on test_domain
    for each row
execute procedure deleteRbacRulesForTestDomain();
--//


-- ============================================================================
--changeset test-domain-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the domain main table which maps the identifying name
    (in this case, actually the column `name`) to the objectUuid.
 */
drop view if exists test_domain_iv;
create or replace view test_domain_iv as
select distinct target.uuid, target.name as idName
    from test_domain as target;
-- TODO.spec: Is it ok that everybody has access to this information?
grant all privileges on test_domain_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case, actually the column `name`).
 */
create or replace function test_domainUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from test_domain_iv iv where iv.idName = test_domainUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the name).
 */
create or replace function test_domainIdNameByUuid(uuid uuid)
    returns varchar
    stable leakproof
    language sql
    strict as $$
select idName from test_domain_iv iv where iv.uuid = test_domainIdNameByUuid.uuid;
$$;
--//


-- ============================================================================
--changeset test-package-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
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
