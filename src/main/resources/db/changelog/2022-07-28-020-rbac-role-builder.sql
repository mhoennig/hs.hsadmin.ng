--liquibase formatted sql

-- ============================================================================
-- PERMISSIONS
--changeset rbac-role-builder-permissions:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*

 */

create type RbacPermissions as
(
    permissionUuids uuid[]
);

create or replace function grantingPermissions(forObjectUuid uuid, permitOps RbacOp[])
    returns RbacPermissions
    language plpgsql
    strict as $$
begin
    return row (createPermissions(forObjectUuid, permitOps))::RbacPermissions;
end; $$;

create or replace function withoutPermissions()
    returns RbacPermissions
    language plpgsql
    strict as $$
begin
    return row (array[]::uuid[]);
end; $$;

--//

--changeset rbac-role-builder-super-roles:1 endDelimiter:--//

/*

 */
create type RbacSuperRoles as
(
    roleUuids uuid[]
);

create or replace function beneathRoles(roleDescriptors RbacRoleDescriptor[])
    returns RbacSuperRoles
    language plpgsql
    strict as $$
declare
    superRoleDescriptor RbacRoleDescriptor;
    superRoleUuids      uuid[] := array []::uuid[];
begin
    foreach superRoleDescriptor in array roleDescriptors
        loop
            superRoleUuids := superRoleUuids || getRoleId(superRoleDescriptor, 'fail');
        end loop;

    return row (superRoleUuids)::RbacSuperRoles;
end; $$;

create or replace function beneathRole(roleDescriptor RbacRoleDescriptor)
    returns RbacSuperRoles
    language plpgsql
    strict as $$
begin
    return beneathRoles(array [roleDescriptor]);
end; $$;

create or replace function beneathRole(roleUuid uuid)
    returns RbacSuperRoles
    language plpgsql
    strict as $$
begin
    return row (array [roleUuid]::uuid[])::RbacSuperRoles;
end; $$;

create or replace function asTopLevelRole()
    returns RbacSuperRoles
    language plpgsql
    strict as $$
begin
    return row (array []::uuid[])::RbacSuperRoles;
end; $$;

--//

-- =================================================================
-- SUB ROLES
--changeset rbac-role-builder-sub-roles:1 endDelimiter:--//
-- -----------------------------------------------------------------

/*

 */
create type RbacSubRoles as
(
    roleUuids uuid[]
);

-- drop FUNCTION beingItselfA(roleUuid uuid)
create or replace function beingItselfA(roleUuid uuid)
    returns RbacSubRoles
    language plpgsql
    strict as $$
begin
    return row (array [roleUuid]::uuid[])::RbacSubRoles;
end; $$;

-- drop FUNCTION beingItselfA(roleDescriptor RbacRoleDescriptor)
create or replace function beingItselfA(roleDescriptor RbacRoleDescriptor)
    returns RbacSubRoles
    language plpgsql
    strict as $$
begin
    return beingItselfA(getRoleId(roleDescriptor, 'fail'));
end; $$;

--//

-- =================================================================
-- USERS
--changeset rbac-role-builder-users:1 endDelimiter:--//
-- -----------------------------------------------------------------

/*
*/
create type RbacUsers as
(
    userUuids uuid[]
);

create or replace function withUsers(userNames varchar[])
    returns RbacUsers
    language plpgsql
    strict as $$
declare
    userName  varchar;
    userUuids uuid[] := array []::uuid[];
begin
    foreach userName in array userNames
        loop
            userUuids := userUuids || getRbacUserId(userName, 'fail');
        end loop;

    return row (userUuids)::RbacUsers;
end; $$;


create or replace function withUser(userName varchar, whenNotExists RbacWhenNotExists = 'fail')
    returns RbacUsers
    returns null on null input
    language plpgsql as $$
begin
    return row (array [getRbacUserId(userName, whenNotExists)]);
end; $$;

--//

-- =================================================================
-- CREATE ROLE
--changeset rbac-role-builder-create-role:1 endDelimiter:--//
-- -----------------------------------------------------------------

/*
*/
create or replace function createRole(
    roleDescriptor RbacRoleDescriptor,
    permissions RbacPermissions,
    superRoles RbacSuperRoles,
    subRoles RbacSubRoles = null,
    users RbacUsers = null
)
    returns uuid
    called on null input
    language plpgsql as $$
declare
    roleUuid      uuid;
    superRoleUuid uuid;
    subRoleUuid   uuid;
    userUuid      uuid;
begin
    raise notice 'will createRole for %', roleDescriptor;
    raise notice 'will createRole for % % %', roleDescriptor.objecttable, roleDescriptor.objectuuid, roleDescriptor.roletype;
    roleUuid = createRole(roleDescriptor);

    call grantPermissionsToRole(roleUuid, permissions.permissionUuids);

    if superRoles is not null then
        foreach superRoleUuid in array superRoles.roleuUids
            loop
                call grantRoleToRole(roleUuid, superRoleUuid);
            end loop;
    end if;

    if subRoles is not null then
        foreach subRoleUuid in array subRoles.roleuUids
            loop
                call grantRoleToRole(subRoleUuid, roleUuid);
            end loop;
    end if;

    if users is not null then
        foreach userUuid in array users.useruUids
            loop
                call grantRoleToUser(roleUuid, userUuid);
            end loop;
    end if;

    return roleUuid;
end; $$;

create or replace function createRole(
    roleDescriptor RbacRoleDescriptor,
    permissions RbacPermissions,
    users RbacUsers = null
)
    returns uuid
    called on null input
    language plpgsql as $$
begin
    return createRole(roleDescriptor, permissions, null, null, users);
end; $$;

create or replace function createRole(
    roleDescriptor RbacRoleDescriptor,
    permissions RbacPermissions,
    subRoles RbacSubRoles,
    users RbacUsers = null
)
    returns uuid
    called on null input
    language plpgsql as $$
begin
    return createRole(roleDescriptor, permissions, null, subRoles, users);
end; $$;

--//
