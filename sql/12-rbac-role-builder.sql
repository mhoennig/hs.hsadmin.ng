

-- ========================================================
-- Role-Hierarcy helper functions
-- --------------------------------------------------------

CREATE TYPE RbacRoleType AS ENUM ('owner', 'admin', 'tenant');

-- PERMISSIONS --------------------------------------------

-- drop type RbacPermissions;
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

-- SUPER ROLES --------------------------------------------

-- drop type RbacSuperRoles;
CREATE TYPE RbacSuperRoles AS
(
    roleUuids uuid[]
);

-- drop function beneathRoles(roleName varchar);
CREATE OR REPLACE FUNCTION beneathRoles(roleNames varchar[])
    RETURNS RbacSuperRoles
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    superRoleName varchar;
    superRoleUuids uuid[] := ARRAY[]::uuid[];
BEGIN
    FOREACH superRoleName IN ARRAY roleNames LOOP
        superRoleUuids := superRoleUuids || getRoleId(superRoleName, 'fail');
    END LOOP;

    RETURN ROW(superRoleUuids)::RbacSuperRoles;
END; $$;

-- drop function beneathRole(roleName varchar);
CREATE OR REPLACE FUNCTION beneathRole(roleName varchar)
    RETURNS RbacSuperRoles
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    RETURN beneathRoles(ARRAY[roleName]);
END; $$;

-- drop function beneathRole(roleUuid uuid);
CREATE OR REPLACE FUNCTION beneathRole(roleUuid uuid)
    RETURNS RbacSuperRoles
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    RETURN ROW(ARRAY[roleUuid]::uuid[])::RbacSuperRoles;
END; $$;

-- drop function asTopLevelRole(roleName varchar);
CREATE OR REPLACE FUNCTION asTopLevelRole()
    RETURNS RbacSuperRoles
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    RETURN ROW(ARRAY[]::uuid[])::RbacSuperRoles;
END; $$;

-- SUB ROLES ----------------------------------------------

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

-- drop FUNCTION beingItselfA(roleName varchar)
CREATE OR REPLACE FUNCTION beingItselfA(roleName varchar)
    RETURNS RbacSubRoles
    LANGUAGE plpgsql STRICT AS $$
BEGIN
    RETURN beingItselfA(getRoleId(roleName, 'fail'));
END; $$;

-- USERS --------------------------------------------------

-- drop type RbacUsers;
CREATE TYPE RbacUsers AS
(
    userUuids uuid[]
);

-- drop function withUsers(userNames varchar);
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


-- DROP FUNCTION withUser(userName varchar, whenNotExists RbacWhenNotExists);
CREATE OR REPLACE FUNCTION withUser(userName varchar, whenNotExists RbacWhenNotExists = 'fail')
    RETURNS RbacUsers
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
BEGIN
    RETURN ROW(ARRAY[getRbacUserId(userName, whenNotExists )]);
END; $$;

-- ROLE NAME BUILDER --------------------------------------

CREATE OR REPLACE FUNCTION roleName(objectTable varchar, objectName varchar, roleType RbacRoleType )
    RETURNS varchar
    RETURNS NULL ON NULL INPUT
    STABLE LEAKPROOF
    LANGUAGE plpgsql AS $$
BEGIN
    RETURN objectTable || '#' || objectName || '.' || roleType;
END; $$;


-- CREATE ROLE MAIN FUNCTION ------------------------------

CREATE OR REPLACE FUNCTION createRole(
    roleName varchar,
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
    RAISE NOTICE 'creating role: %', roleName;
    roleUuid = createRole(roleName);

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
    roleName varchar,
    permissions RbacPermissions,
    users RbacUsers = null
)
    RETURNS uuid
    CALLED ON NULL INPUT
    LANGUAGE plpgsql AS $$
BEGIN
    RETURN createRole(roleName, permissions, null, null, users);
END; $$;

CREATE OR REPLACE FUNCTION createRole(
    roleName varchar,
    permissions RbacPermissions,
    subRoles RbacSubRoles,
    users RbacUsers = null
)
    RETURNS uuid
    CALLED ON NULL INPUT
    LANGUAGE plpgsql AS $$
BEGIN
    RETURN createRole(roleName, permissions, null, subRoles, users);
END; $$;


