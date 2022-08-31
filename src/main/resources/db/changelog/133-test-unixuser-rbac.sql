--liquibase formatted sql

-- ============================================================================
--changeset test-package-rbac-CREATE-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the related RbacObject through a BEFORE INSERT TRIGGER.
 */
drop trigger if exists createRbacObjectFortest_unixuser_Trigger on test_unixuser;
create trigger createRbacObjectFortest_unixuser_Trigger
    before insert
    on test_unixuser
    for each row
execute procedure createRbacObject();
--//


-- ============================================================================
--changeset test-unixuser-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function testUnixUserOwner(uu test_unixuser)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('test_unixuser', uu.uuid, 'owner');
end; $$;

create or replace function testUnixUserAdmin(uu test_unixuser)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('test_unixuser', uu.uuid, 'admin');
end; $$;

create or replace function testUnixUserTenant(uu test_unixuser)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('test_unixuser', uu.uuid, 'tenant');
end; $$;

create or replace function createTestUnixUserTenantRoleIfNotExists(unixUser test_unixuser)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    unixUserTenantRoleDesc RbacRoleDescriptor;
    unixUserTenantRoleUuid uuid;
begin
    unixUserTenantRoleDesc = testUnixUserTenant(unixUser);
    unixUserTenantRoleUuid = findRoleId(unixUserTenantRoleDesc);
    if unixUserTenantRoleUuid is not null then
        return unixUserTenantRoleUuid;
    end if;

    return createRole(
        unixUserTenantRoleDesc,
        grantingPermissions(forObjectUuid => unixUser.uuid, permitOps => array ['view']),
        beneathRole(testUnixUserAdmin(unixUser))
        );
end; $$;
--//


-- ============================================================================
--changeset test-unixuser-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the roles and their assignments for a new UnixUser for the AFTER INSERT TRIGGER.
 */

create or replace function createRbacRulesForTestUnixUser()
    returns trigger
    language plpgsql
    strict as $$
declare
    parentPackage       test_package;
    unixuserOwnerRoleId uuid;
    unixuserAdminRoleId uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    select * from test_package where uuid = NEW.packageUuid into parentPackage;

    -- an owner role is created and assigned to the package's admin group
    unixuserOwnerRoleId = createRole(
        testUnixUserOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(testPackageAdmin(parentPackage))
        );

    -- and a unixuser admin role is created and assigned to the unixuser owner as well
    unixuserAdminRoleId = createRole(
        testUnixUserAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit']),
        beneathRole(unixuserOwnerRoleId),
        beingItselfA(testPackageTenant(parentPackage))
        );

    -- a tenent role is only created on demand

    return NEW;
end; $$;


/*
    An AFTER INSERT TRIGGER which creates the role structure for a new UnixUser.
 */
drop trigger if exists createRbacRulesForTestUnixuser_Trigger on test_unixuser;
create trigger createRbacRulesForTestUnixuser_Trigger
    after insert
    on test_unixuser
    for each row
execute procedure createRbacRulesForTestUnixUser();
--//


-- ============================================================================
--changeset test-unixuser-rbac-ROLES-REMOVAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Deletes the roles and their assignments of a deleted UnixUser for the BEFORE DELETE TRIGGER.
 */

create or replace function deleteRbacRulesForTestUnixUser()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        call deleteRole(findRoleId(testUnixUserOwner(OLD)));
        call deleteRole(findRoleId(testUnixUserAdmin(OLD)));
        call deleteRole(findRoleId(testUnixUserTenant(OLD)));
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
end; $$;

/*
    An BEFORE DELETE TRIGGER which deletes the role structure of a UnixUser.
 */

drop trigger if exists deleteRbacRulesForTestUnixUser_Trigger on test_package;
create trigger deleteRbacRulesForTestUnixUser_Trigger
    before delete
    on test_unixuser
    for each row
execute procedure deleteRbacRulesForTestUnixUser();
--//


-- ============================================================================
--changeset test-unixuser-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the UnixUser main table which maps the identifying name
    (in this case, actually the column `name`) to the objectUuid.
 */
drop view if exists test_unixuser_iv;
create or replace view test_unixuser_iv as
select distinct target.uuid, target.name as idName
    from test_unixuser as target;
-- TODO: Is it ok that everybody has access to this information?
grant all privileges on test_unixuser_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case, actually the column `name`).
 */
create or replace function test_unixUserUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from test_unixuser_iv iv where iv.idName = test_unixUserUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the name).
 */
create or replace function test_unixUserIdNameByUuid(uuid uuid)
    returns varchar
    stable leakproof
    language sql
    strict as $$
select idName from test_unixuser_iv iv where iv.uuid = test_unixUserIdNameByUuid.uuid;
$$;
--//


-- ============================================================================
--changeset test-package-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the customer main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
drop view if exists test_unixuser_rv;
create or replace view test_unixuser_rv as
select target.*
    from test_unixuser as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'unixuser', currentSubjectsUuids()));
grant all privileges on test_unixuser_rv to restricted;
--//
