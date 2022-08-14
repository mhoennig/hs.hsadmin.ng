--liquibase formatted sql

-- ============================================================================
--changeset hs-package-rbac-CREATE-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the related RbacObject through a BEFORE INSERT TRIGGER.
 */
drop trigger if exists createRbacObjectForUnixUser_Trigger on UnixUser;
create trigger createRbacObjectForUnixUser_Trigger
    before insert
    on UnixUser
    for each row
execute procedure createRbacObject();
--//


-- ============================================================================
--changeset hs-unixuser-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function unixUserOwner(uu UnixUser)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('unixuser', uu.uuid, 'owner');
end; $$;

create or replace function unixUserAdmin(uu UnixUser)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('unixuser', uu.uuid, 'admin');
end; $$;

create or replace function unixUserTenant(uu UnixUser)
    returns RbacRoleDescriptor
    returns null on null input
    language plpgsql as $$
begin
    return roleDescriptor('unixuser', uu.uuid, 'tenant');
end; $$;

create or replace function createUnixUserTenantRoleIfNotExists(unixUser UnixUser)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    unixUserTenantRoleDesc RbacRoleDescriptor;
    unixUserTenantRoleUuid uuid;
begin
    unixUserTenantRoleDesc = unixUserTenant(unixUser);
    unixUserTenantRoleUuid = findRoleId(unixUserTenantRoleDesc);
    if unixUserTenantRoleUuid is not null then
        return unixUserTenantRoleUuid;
    end if;

    return createRole(
        unixUserTenantRoleDesc,
        grantingPermissions(forObjectUuid => unixUser.uuid, permitOps => array ['view']),
        beneathRole(unixUserAdmin(unixUser))
        );
end; $$;
--//


-- ============================================================================
--changeset hs-unixuser-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates the roles and their assignments for a new UnixUser for the AFTER INSERT TRIGGER.
 */

create or replace function createRbacRulesForUnixUser()
    returns trigger
    language plpgsql
    strict as $$
declare
    parentPackage       package;
    unixuserOwnerRoleId uuid;
    unixuserAdminRoleId uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    select * from package where uuid = NEW.packageUuid into parentPackage;

    -- an owner role is created and assigned to the package's admin group
    unixuserOwnerRoleId = createRole(
        unixUserOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
        beneathRole(packageAdmin(parentPackage))
        );

    -- and a unixuser admin role is created and assigned to the unixuser owner as well
    unixuserAdminRoleId = createRole(
        unixUserAdmin(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit']),
        beneathRole(unixuserOwnerRoleId),
        beingItselfA(packageTenant(parentPackage))
        );

    -- a tenent role is only created on demand

    return NEW;
end; $$;


/*
    An AFTER INSERT TRIGGER which creates the role structure for a new UnixUser.
 */
drop trigger if exists createRbacRulesForUnixUser_Trigger on UnixUser;
create trigger createRbacRulesForUnixUser_Trigger
    after insert
    on UnixUser
    for each row
execute procedure createRbacRulesForUnixUser();
--//


-- ============================================================================
--changeset hs-unixuser-rbac-ROLES-REMOVAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Deletes the roles and their assignments of a deleted UnixUser for the BEFORE DELETE TRIGGER.
 */

create or replace function deleteRbacRulesForUnixUser()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        call deleteRole(findRoleId(unixUserOwner(OLD)));
        call deleteRole(findRoleId(unixUserAdmin(OLD)));
        call deleteRole(findRoleId(unixUserTenant(OLD)));
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
end; $$;

/*
    An BEFORE DELETE TRIGGER which deletes the role structure of a UnixUser.
 */

drop trigger if exists deleteRbacRulesForUnixUser_Trigger on package;
create trigger deleteRbacRulesForUnixUser_Trigger
    before delete
    on UnixUser
    for each row
execute procedure deleteRbacRulesForUnixUser();
--//


-- ============================================================================
--changeset hs-unixuser-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the UnixUser main table which maps the identifying name
    (in this case, actually the column `name`) to the objectUuid.
 */
drop view if exists UnixUser_iv;
create or replace view UnixUser_iv as
select distinct target.uuid, target.name as idName
    from UnixUser as target;
-- TODO: Is it ok that everybody has access to this information?
grant all privileges on UnixUser_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case, actually the column `name`).
 */
create or replace function unixUserUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from UnixUser_iv iv where iv.idName = unixUserUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the name).
 */
create or replace function unixUserIdNameByUuid(uuid uuid)
    returns varchar
    stable leakproof
    language sql
    strict as $$
select idName from UnixUser_iv iv where iv.uuid = unixUserIdNameByUuid.uuid;
$$;
--//


-- ============================================================================
--changeset hs-package-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the customer main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
drop view if exists unixuser_rv;
create or replace view unixuser_rv as
select target.*
    from unixuser as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'unixuser', currentSubjectIds()));
grant all privileges on unixuser_rv to restricted;
--//
