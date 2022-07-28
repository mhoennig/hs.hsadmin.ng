--liquibase formatted sql

-- ==================================================================
-- PERMISSIONS
--changeset rbac-role-builder-permissions:1 endDelimiter:--//
-- ------------------------------------------------------------------

/*

 */

CREATE TYPE RbacPermissions AS
(
    permissionUuids uuid[]
);

CREATE OR REPLACE FUNCTION grantingPermissions(forObjectUuid uuid, permitOps RbacOp[])
    RETURNS RbacPermissions
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    RETURN ROW(createPermissions(forObjectUuid, permitOps))::RbacPermissions;
END; $$;

--//

--changeset rbac-role-builder-super-roles:1 endDelimiter:--//

/*

 */
CREATE TYPE RbacSuperRoles AS
(
    roleUuids uuid[]
);

CREATE OR REPLACE FUNCTION beneathRoles(roleDescriptors RbacRoleDescriptor[])
    RETURNS RbacSuperRoles
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    superRoleDescriptor RbacRoleDescriptor;
    superRoleUuids uuid[] := ARRAY[]::uuid[];
BEGIN
    FOREACH superRoleDescriptor IN ARRAY roleDescriptors LOOP
        superRoleUuids := superRoleUuids || getRoleId(superRoleDescriptor, 'fail');
    END LOOP;

    RETURN ROW(superRoleUuids)::RbacSuperRoles;
END; $$;

CREATE OR REPLACE FUNCTION beneathRole(roleDescriptor RbacRoleDescriptor)
    RETURNS RbacSuperRoles
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    RETURN beneathRoles(ARRAY[roleDescriptor]);
END; $$;

CREATE OR REPLACE FUNCTION beneathRole(roleUuid uuid)
    RETURNS RbacSuperRoles
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    RETURN ROW(ARRAY[roleUuid]::uuid[])::RbacSuperRoles;
END; $$;

CREATE OR REPLACE FUNCTION asTopLevelRole()
    RETURNS RbacSuperRoles
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    RETURN ROW(ARRAY[]::uuid[])::RbacSuperRoles;
END; $$;

--//

-- =================================================================
-- SUB ROLES
--changeset rbac-role-builder-sub-roles:1 endDelimiter:--//
-- -----------------------------------------------------------------

/*

 */
CREATE TYPE RbacSubRoles AS
(
    roleUuids uuid[]
);

-- drop FUNCTION beingItselfA(roleUuid uuid)
CREATE OR REPLACE FUNCTION beingItselfA(roleUuid uuid)
    RETURNS RbacSubRoles
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    RETURN ROW(ARRAY[roleUuid]::uuid[])::RbacSubRoles;
END; $$;

-- drop FUNCTION beingItselfA(roleDescriptor RbacRoleDescriptor)
CREATE OR REPLACE FUNCTION beingItselfA(roleDescriptor RbacRoleDescriptor)
    RETURNS RbacSubRoles
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    RETURN beingItselfA(getRoleId(roleDescriptor, 'fail'));
END; $$;

--//

-- =================================================================
-- USERS
--changeset rbac-role-builder-users:1 endDelimiter:--//
-- -----------------------------------------------------------------

/*
*/
CREATE TYPE RbacUsers AS
(
    userUuids uuid[]
);

CREATE OR REPLACE FUNCTION withUsers(userNames varchar[])
    RETURNS RbacUsers
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    userName varchar;
    userUuids uuid[] := ARRAY[]::uuid[];
BEGIN
    FOREACH userName IN ARRAY userNames LOOP
        userUuids := userUuids || getRbacUserId(userName, 'fail');
    END LOOP;

    RETURN ROW(userUuids)::RbacUsers;
END; $$;


CREATE OR REPLACE FUNCTION withUser(userName varchar, whenNotExists RbacWhenNotExists = 'fail')
    RETURNS RbacUsers
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
BEGIN
    RETURN ROW(ARRAY[getRbacUserId(userName, whenNotExists )]);
END; $$;

--//

-- =================================================================
-- CREATE ROLE
--changeset rbac-role-builder-create-role:1 endDelimiter:--//
-- -----------------------------------------------------------------

/*
*/
CREATE OR REPLACE FUNCTION createRole(
    roleDescriptor RbacRoleDescriptor,
    permissions RbacPermissions,
    superRoles RbacSuperRoles,
    subRoles RbacSubRoles = null,
    users RbacUsers = null
)
    RETURNS uuid
    CALLED ON NULL INPUT
    LANGUAGE plpgsql AS $$
DECLARE
    roleUuid uuid;
    superRoleUuid uuid;
    subRoleUuid uuid;
    userUuid uuid;
BEGIN
    RAISE NOTICE 'will createRole for %', roleDescriptor;
    RAISE NOTICE 'will createRole for % % %', roleDescriptor.objecttable, roleDescriptor.objectuuid, roleDescriptor.roletype;
    roleUuid = createRole(roleDescriptor);

    call grantPermissionsToRole(roleUuid, permissions.permissionUuids);

    IF superRoles IS NOT NULL THEN
        FOREACH superRoleUuid IN ARRAY superRoles.roleuUids LOOP
            call grantRoleToRole(roleUuid, superRoleUuid);
        END LOOP;
    END IF;

    IF subRoles IS NOT NULL THEN
        FOREACH subRoleUuid IN ARRAY subRoles.roleuUids LOOP
            call grantRoleToRole(subRoleUuid, roleUuid);
        END LOOP;
    END IF;

    IF users IS NOT NULL THEN
        FOREACH userUuid IN ARRAY users.useruUids LOOP
            call grantRoleToUser(roleUuid, userUuid);
        END LOOP;
    END IF;

    RETURN roleUuid;
END; $$;

CREATE OR REPLACE FUNCTION createRole(
    roleDescriptor RbacRoleDescriptor,
    permissions RbacPermissions,
    users RbacUsers = null
)
    RETURNS uuid
    CALLED ON NULL INPUT
    LANGUAGE plpgsql AS $$
BEGIN
    RETURN createRole(roleDescriptor, permissions, null, null, users);
END; $$;

CREATE OR REPLACE FUNCTION createRole(
    roleDescriptor RbacRoleDescriptor,
    permissions RbacPermissions,
    subRoles RbacSubRoles,
    users RbacUsers = null
)
    RETURNS uuid
    CALLED ON NULL INPUT
    LANGUAGE plpgsql AS $$
BEGIN
    RETURN createRole(roleDescriptor, permissions, null, subRoles, users);
END; $$;

--//
