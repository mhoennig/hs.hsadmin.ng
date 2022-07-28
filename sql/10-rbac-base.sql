
-- ========================================================
-- RBAC
-- --------------------------------------------------------

SET SESSION SESSION AUTHORIZATION DEFAULT;

-- https://arctype.com/blog/postgres-uuid/#creating-a-uuid-primary-key-using-uuid-osp-postgresql-example
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TABLE IF EXISTS "RbacPermission";
DROP TABLE IF EXISTS "RbacGrants";
DROP TABLE IF EXISTS "RbacUser";
DROP TABLE IF EXISTS RbacReference CASCADE;
DROP TYPE IF EXISTS RbacOp CASCADE;
DROP TYPE IF EXISTS ReferenceType CASCADE;

CREATE TYPE ReferenceType AS ENUM ('RbacUser', 'RbacRole', 'RbacPermission');

CREATE TABLE RbacReference
(
    uuid uuid UNIQUE DEFAULT uuid_generate_v4(),
    type ReferenceType not null
);

CREATE TABLE RbacUser
(
    uuid uuid primary key references RbacReference (uuid) ON DELETE CASCADE,
    name varchar(63) not null unique
);

-- DROP TABLE IF EXISTS RbacObject;
CREATE TABLE RbacObject
(
    uuid uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    objectTable varchar(64) not null,
    unique (objectTable, uuid)
);

CREATE TYPE RbacRoleType AS ENUM ('owner', 'admin', 'tenant');

CREATE TABLE RbacRole
(
    uuid uuid primary key references RbacReference (uuid) ON DELETE CASCADE,
    objectUuid uuid references RbacObject(uuid) not null,
    roleType RbacRoleType not null
);

CREATE TABLE RbacGrants
(
    ascendantUuid uuid references RbacReference (uuid) ON DELETE CASCADE,
    descendantUuid uuid references RbacReference (uuid) ON DELETE CASCADE,
    follow boolean not null default true,
    primary key (ascendantUuid, descendantUuid)
);
CREATE INDEX ON RbacGrants (ascendantUuid);
CREATE INDEX ON RbacGrants (descendantUuid);

-- DROP DOMAIN IF EXISTS RbacOp CASCADE;
CREATE DOMAIN RbacOp AS VARCHAR(67)
    CHECK(
          VALUE = '*'
       OR VALUE = 'delete'
       OR VALUE = 'edit'
       OR VALUE = 'view'
       OR VALUE = 'assume'
       OR VALUE ~ '^add-[a-z]+$'
        );

CREATE OR REPLACE FUNCTION createRbacObject()
    RETURNS trigger
    LANGUAGE plpgsql STRICT AS $$
DECLARE
    objectUuid uuid;
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO RbacObject (objectTable) VALUES (TG_TABLE_NAME) RETURNING uuid INTO objectUuid;
        NEW.uuid = objectUuid;
        RETURN NEW;
    ELSE
        RAISE EXCEPTION 'invalid usage of TRIGGER AFTER INSERT';
    END IF;
END; $$;


-- DROP TABLE IF EXISTS RbacPermission;
CREATE TABLE RbacPermission
(   uuid uuid primary key references RbacReference (uuid) ON DELETE CASCADE,
    objectUuid uuid not null references RbacObject,
    op RbacOp not null,
    unique (objectUuid, op)
);

-- SET SESSION SESSION AUTHORIZATION DEFAULT;
-- alter table rbacpermission add constraint rbacpermission_objectuuid_fkey foreign key (objectUuid) references rbacobject(uuid);
-- alter table rbacpermission drop constraint rbacpermission_objectuuid;

CREATE OR REPLACE FUNCTION hasPermission(forObjectUuid uuid, forOp RbacOp)
    RETURNS bool
    LANGUAGE sql AS $$
        SELECT EXISTS (
            SELECT op
              FROM RbacPermission p
             WHERE p.objectUuid=forObjectUuid AND p.op in ('*', forOp)
            );
    $$;

CREATE OR REPLACE FUNCTION createRbacUser(userName varchar)
    RETURNS uuid
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
declare
    objectId uuid;
BEGIN
    INSERT INTO RbacReference (type) VALUES ('RbacUser') RETURNING uuid INTO objectId;
    INSERT INTO RbacUser (uuid, name) VALUES (objectid, userName);
    return objectId;
END;
$$;

CREATE OR REPLACE FUNCTION findRbacUserId(userName varchar)
    RETURNS uuid
    RETURNS NULL ON NULL INPUT
    LANGUAGE sql AS $$
        SELECT uuid FROM RbacUser WHERE name = userName
$$;

CREATE TYPE RbacWhenNotExists AS ENUM ('fail', 'create');

CREATE OR REPLACE FUNCTION getRbacUserId(userName varchar, whenNotExists RbacWhenNotExists)
    RETURNS uuid
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
DECLARE
    userUuid uuid;
BEGIN
    userUuid = findRbacUserId(userName);
    IF ( userUuid IS NULL ) THEN
        IF ( whenNotExists = 'fail') THEN
            RAISE EXCEPTION 'RbacUser with name="%" not found', userName;
        END IF;
        IF ( whenNotExists = 'create') THEN
            userUuid = createRbacUser(userName);
        END IF;
    END IF;
    return userUuid;
END;
$$;

CREATE TYPE RbacRoleDescriptor AS
    (
      objectTable     varchar(63), -- TODO: needed? remove?
      objectUuid      uuid,
      roleType        RbacRoleType
  );

-- TODO: this ...
CREATE OR REPLACE FUNCTION roleDescriptor(objectTable varchar(63), objectUuid uuid, roleType RbacRoleType )
    RETURNS RbacRoleDescriptor
    RETURNS NULL ON NULL INPUT
    STABLE LEAKPROOF
    LANGUAGE plpgsql AS $$
BEGIN
    RETURN ROW(objectTable, objectUuid, roleType);
END; $$;

CREATE FUNCTION new_emp() RETURNS emp AS $$
SELECT text 'None' AS name,
       1000.0 AS salary,
       25 AS age,
       point '(2,2)' AS cubicle;
$$ LANGUAGE SQL;

DO LANGUAGE plpgsql $$
DECLARE
    roleDesc RbacRoleDescriptor;
BEGIN
    select * FROM roleDescriptor('global', gen_random_uuid(), 'admin') into roleDesc;
    RAISE NOTICE 'result: % % %', roleDesc.objecttable, roleDesc.objectuuid, roleDesc.roletype;
END; $$;

-- TODO: ... or that?
CREATE OR REPLACE FUNCTION roleDescriptor(objectTable varchar(63), objectUuid uuid, roleType RbacRoleType )
    RETURNS RbacRoleDescriptor
    RETURNS NULL ON NULL INPUT
    -- STABLE LEAKPROOF
    LANGUAGE sql AS $$
        SELECT objectTable, objectUuid, roleType::RbacRoleType;
    $$;




CREATE OR REPLACE FUNCTION createRole(roleDescriptor RbacRoleDescriptor)
    RETURNS uuid
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
declare
    referenceId uuid;
BEGIN
    INSERT INTO RbacReference (type) VALUES ('RbacRole') RETURNING uuid INTO referenceId;
    INSERT INTO RbacRole (uuid, objectUuid, roleType) VALUES (referenceId, roleDescriptor.objectUuid, roleDescriptor.roleType);
    return referenceId;
END;
$$;


CREATE OR REPLACE PROCEDURE deleteRole(roleUUid uuid)
    LANGUAGE plpgsql AS $$
BEGIN
    DELETE FROM RbacRole WHERE uuid=roleUUid;
END;
$$;

CREATE OR REPLACE FUNCTION findRoleId(roleDescriptor RbacRoleDescriptor)
    RETURNS uuid
    RETURNS NULL ON NULL INPUT
    LANGUAGE sql AS $$
        SELECT uuid FROM RbacRole WHERE objectUuid = roleDescriptor.objectUuid AND roleType = roleDescriptor.roleType;
$$;

CREATE OR REPLACE FUNCTION getRoleId(roleDescriptor RbacRoleDescriptor, whenNotExists RbacWhenNotExists)
    RETURNS uuid
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
DECLARE
    roleUuid uuid;
BEGIN
    roleUuid = findRoleId(roleDescriptor);
    IF ( roleUuid IS NULL ) THEN
        IF ( whenNotExists = 'fail') THEN
            RAISE EXCEPTION 'RbacRole "%#%.%" not found', roleDescriptor.objectTable, roleDescriptor.objectUuid, roleDescriptor.roleType;
        END IF;
        IF ( whenNotExists = 'create') THEN
            roleUuid = createRole(roleDescriptor);
        END IF;
    END IF;
    return roleUuid;
END;
$$;

-- select getRoleId('hostmaster', 'create');

CREATE OR REPLACE FUNCTION createPermissions(forObjectUuid uuid, permitOps RbacOp[])
    RETURNS uuid[]
    LANGUAGE plpgsql AS $$
DECLARE
    refId uuid;
    permissionIds uuid[] = ARRAY[]::uuid[];
BEGIN
    RAISE NOTICE 'createPermission for: % %', forObjectUuid, permitOps;
    IF ( forObjectUuid IS NULL ) THEN
        RAISE EXCEPTION 'forObjectUuid must not be null';
    END IF;
    IF ( array_length(permitOps, 1) > 1 AND '*' = any(permitOps) ) THEN
        RAISE EXCEPTION '"*" operation must not be assigned along with other operations: %', permitOps;
    END IF;

    FOR i IN array_lower(permitOps, 1)..array_upper(permitOps, 1) LOOP
        refId = (SELECT uuid FROM RbacPermission WHERE objectUuid=forObjectUuid AND op=permitOps[i]);
        IF (refId IS NULL) THEN
            RAISE NOTICE 'createPermission: % %', forObjectUuid, permitOps[i];
            INSERT INTO RbacReference ("type") VALUES ('RbacPermission') RETURNING uuid INTO refId;
            INSERT INTO RbacPermission (uuid, objectUuid, op) VALUES (refId, forObjectUuid, permitOps[i]);
        END IF;
        RAISE NOTICE 'addPermission: %', refId;
        permissionIds = permissionIds || refId;
    END LOOP;

    RAISE NOTICE 'createPermissions returning: %', permissionIds;
    return permissionIds;
END;
$$;

CREATE OR REPLACE FUNCTION findPermissionId(forObjectUuid uuid, forOp RbacOp)
    RETURNS uuid
    RETURNS NULL ON NULL INPUT
    STABLE LEAKPROOF
    LANGUAGE sql AS $$
        SELECT uuid FROM RbacPermission p
         WHERE p.objectUuid=forObjectUuid AND p.op in ('*', forOp)
$$;

CREATE OR REPLACE FUNCTION assertReferenceType(argument varchar, referenceId uuid, expectedType ReferenceType)
    RETURNS ReferenceType
    LANGUAGE plpgsql AS $$
DECLARE
    actualType ReferenceType;
BEGIN
    actualType = (SELECT type FROM RbacReference WHERE uuid=referenceId);
    IF ( actualType <> expectedType ) THEN
        RAISE EXCEPTION '% must reference a %, but got a %', argument, expectedType, actualType;
    end if;
    RETURN expectedType;
END; $$;

CREATE OR REPLACE PROCEDURE grantPermissionsToRole(roleUuid uuid, permissionIds uuid[])
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE NOTICE 'grantPermissionsToRole: % -> %', roleUuid, permissionIds;
    FOR i IN array_lower(permissionIds, 1)..array_upper(permissionIds, 1) LOOP
        perform assertReferenceType('roleId (ascendant)', roleUuid, 'RbacRole');
        perform assertReferenceType('permissionId (descendant)',  permissionIds[i], 'RbacPermission');

        -- INSERT INTO RbacGrants (ascendantUuid, descendantUuid, apply) VALUES (roleId, permissionIds[i], true); -- assumeV1
        INSERT INTO RbacGrants (ascendantUuid, descendantUuid) VALUES (roleUuid, permissionIds[i]);
    END LOOP;
END;
$$;

CREATE OR REPLACE PROCEDURE grantRoleToRole(subRoleId uuid, superRoleId uuid, doFollow bool = true )
    LANGUAGE plpgsql AS $$
BEGIN
    perform assertReferenceType('superRoleId (ascendant)', superRoleId, 'RbacRole');
    perform assertReferenceType('subRoleId (descendant)',  subRoleId, 'RbacRole');

    IF ( isGranted(subRoleId, superRoleId) ) THEN
        RAISE EXCEPTION 'Cyclic role grant detected between % and %', subRoleId, superRoleId;
    END IF;

    -- INSERT INTO RbacGrants (ascendantUuid, descendantUuid, apply) VALUES (superRoleId, subRoleId, doapply); -- assumeV1
    INSERT INTO RbacGrants (ascendantUuid, descendantUuid, follow) VALUES (superRoleId, subRoleId, doFollow)
    ON CONFLICT DO NOTHING ; -- TODO: remove?
END; $$;

CREATE OR REPLACE PROCEDURE revokeRoleFromRole(subRoleId uuid, superRoleId uuid)
    LANGUAGE plpgsql AS $$
BEGIN
    perform assertReferenceType('superRoleId (ascendant)', superRoleId, 'RbacRole');
    perform assertReferenceType('subRoleId (descendant)',  subRoleId, 'RbacRole');

    IF ( isGranted(subRoleId, superRoleId) ) THEN
        DELETE FROM RbacGrants WHERE ascendantUuid=superRoleId AND descendantUuid=subRoleId;
    END IF;
END; $$;

CREATE OR REPLACE PROCEDURE grantRoleToUser(roleId uuid, userId uuid)
    LANGUAGE plpgsql AS $$
BEGIN
    perform assertReferenceType('roleId (ascendant)', roleId, 'RbacRole');
    perform assertReferenceType('userId (descendant)',  userId, 'RbacUser');

    -- INSERT INTO RbacGrants (ascendantUuid, descendantUuid, apply) VALUES (userId, roleId, true); -- assumeV1
    INSERT INTO RbacGrants (ascendantUuid, descendantUuid) VALUES (userId, roleId)
        ON CONFLICT DO NOTHING ; -- TODO: remove?
END; $$;

abort;
set local session authorization default;

CREATE OR REPLACE FUNCTION queryAccessibleObjectUuidsOfSubjectIds(
            requiredOp RbacOp,
            forObjectTable varchar, -- reduces the result set, but is not really faster when used in restricted view
            subjectIds uuid[],
            maxObjects integer = 8000)
    RETURNS SETOF uuid
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
    DECLARE
        foundRows     bigint;
    BEGIN
        RETURN QUERY SELECT DISTINCT perm.objectUuid
          FROM (
               WITH RECURSIVE grants AS (
                   SELECT descendantUuid, ascendantUuid, 1 AS level
                       FROM RbacGrants
                       WHERE follow AND ascendantUuid = ANY(subjectIds)
                   UNION DISTINCT
                   SELECT "grant".descendantUuid, "grant".ascendantUuid, level+1 AS level
                        FROM RbacGrants "grant"
                        INNER JOIN grants recur ON recur.descendantUuid = "grant".ascendantUuid
                        WHERE follow
               ) SELECT descendantUuid
               FROM grants
           ) as granted
         JOIN RbacPermission perm
             ON granted.descendantUuid=perm.uuid AND perm.op IN ('*', requiredOp)
        JOIN RbacObject obj ON obj.uuid=perm.objectUuid AND obj.objectTable=forObjectTable
        LIMIT maxObjects+1;

         foundRows = lastRowCount();
         IF foundRows > maxObjects THEN
             RAISE EXCEPTION 'Too many accessible objects, limit is %, found %.', maxObjects, foundRows
                 USING
                     ERRCODE = 'P0003',
                     HINT = 'Please assume a sub-role and try again.';
         END IF;
    END;
$$;

SET SESSION AUTHORIZATION DEFAULT;
CREATE ROLE admin;
GRANT USAGE ON SCHEMA public TO admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin;
CREATE ROLE restricted;
GRANT USAGE ON SCHEMA public TO restricted;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO restricted;

abort;
set local session authorization restricted;
begin transaction;
set local statement_timeout TO '15s';
select count(*)
  from queryAccessibleObjectUuidsOfSubjectIds('view', 'customer', ARRAY[findRbacUserId('mike@hostsharing.net')],  10000);
end transaction;

---

abort;
set local session authorization default;
CREATE OR REPLACE FUNCTION queryRequiredPermissionsOfSubjectIds(requiredOp RbacOp, subjectIds uuid[])
    RETURNS SETOF RbacPermission
    STRICT
    LANGUAGE sql AS $$
        SELECT DISTINCT *
          FROM RbacPermission
         WHERE op = '*' OR op = requiredOp
           AND uuid IN (
                WITH RECURSIVE grants AS (
                    SELECT DISTINCT
                        descendantUuid,
                        ascendantUuid
                    FROM RbacGrants
                    WHERE
                            ascendantUuid = ANY(subjectIds)
                    UNION ALL
                    SELECT
                        "grant".descendantUuid,
                        "grant".ascendantUuid
                    FROM RbacGrants "grant"
                             INNER JOIN grants recur ON recur.descendantUuid = "grant".ascendantUuid
                ) SELECT
                    descendantUuid
                FROM grants
            );
$$;

abort;
set local session authorization restricted;
begin transaction;
-- set local statement_timeout TO '5s';
set local statement_timeout TO '5min';
select count(*) from queryRequiredPermissionsOfSubjectIds('view', ARRAY[findRbacUserId('mike@hostsharing.net')]);
end transaction;

---

abort;
set local session authorization default;
CREATE OR REPLACE FUNCTION queryAllPermissionsOfSubjectIds(subjectIds uuid[])
    RETURNS SETOF RbacPermission
    STRICT
    LANGUAGE sql AS $$
        SELECT DISTINCT * FROM RbacPermission WHERE uuid IN (
            WITH RECURSIVE grants AS (
                SELECT DISTINCT
                    descendantUuid,
                    ascendantUuid
                FROM RbacGrants
                WHERE
                        ascendantUuid = ANY(subjectIds)
                UNION ALL
                SELECT
                    "grant".descendantUuid,
                    "grant".ascendantUuid
                FROM RbacGrants "grant"
                         INNER JOIN grants recur ON recur.descendantUuid = "grant".ascendantUuid
            ) SELECT
                descendantUuid
            FROM grants
        );
        $$;

abort;
set local session authorization restricted;
begin transaction;
    set local statement_timeout TO '5s';
    select count(*) from queryAllPermissionsOfSubjectIds(ARRAY[findRbacUserId('mike@hostsharing.net')]);
end transaction;

---
/*
CREATE OR REPLACE FUNCTION queryAllPermissionsOfSubjectId(subjectId uuid) -- TODO: remove?
    RETURNS SETOF RbacPermission
    RETURNS NULL ON NULL INPUT
    LANGUAGE sql AS $$
        SELECT * FROM RbacPermission WHERE uuid IN (
        WITH RECURSIVE grants AS (
            SELECT
                descendantUuid,
                ascendantUuid
            FROM
                RbacGrants
            WHERE
                ascendantUuid = subjectId
            UNION ALL
            SELECT
                "grant".descendantUuid,
                "grant".ascendantUuid
            FROM RbacGrants "grant"
                 INNER JOIN grants recur ON recur.descendantUuid = "grant".ascendantUuid
        ) SELECT
            descendantUuid
        FROM
            grants
    );
$$;*/

---

CREATE OR REPLACE FUNCTION queryAllRbacUsersWithPermissionsFor(objectId uuid)
    RETURNS SETOF RbacUser
    RETURNS NULL ON NULL INPUT
    LANGUAGE sql AS $$
SELECT * FROM RbacUser WHERE uuid IN (
    WITH RECURSIVE grants AS (
        SELECT
            descendantUuid,
            ascendantUuid
        FROM
            RbacGrants
        WHERE
            descendantUuid = objectId
        UNION ALL
        SELECT
            "grant".descendantUuid,
            "grant".ascendantUuid
        FROM
            RbacGrants "grant"
                INNER JOIN grants recur ON recur.ascendantUuid = "grant".descendantUuid
    ) SELECT
        ascendantUuid
    FROM
        grants
);
$$;


CREATE OR REPLACE FUNCTION findGrantees(grantedId uuid)
    RETURNS SETOF RbacReference
    RETURNS NULL ON NULL INPUT
    LANGUAGE sql AS $$
    SELECT reference.*
      FROM (
        WITH RECURSIVE grants AS (
            SELECT
                descendantUuid,
                ascendantUuid
            FROM
                RbacGrants
            WHERE
                    descendantUuid = grantedId
            UNION ALL
            SELECT
                "grant".descendantUuid,
                "grant".ascendantUuid
            FROM
                RbacGrants "grant"
                    INNER JOIN grants recur ON recur.ascendantUuid = "grant".descendantUuid
        ) SELECT
            ascendantUuid
        FROM
            grants
      ) as grantee
    JOIN RbacReference reference ON reference.uuid=grantee.ascendantUuid;
$$;

CREATE OR REPLACE FUNCTION isGranted(granteeId uuid, grantedId uuid)
    RETURNS bool
    RETURNS NULL ON NULL INPUT
    LANGUAGE sql AS $$
        SELECT granteeId=grantedId OR granteeId IN (
            WITH RECURSIVE grants AS (
                SELECT descendantUuid, ascendantUuid
                FROM RbacGrants
                WHERE descendantUuid = grantedId
                UNION ALL
                SELECT "grant".descendantUuid, "grant".ascendantUuid
                FROM RbacGrants "grant"
                INNER JOIN grants recur ON recur.ascendantUuid = "grant".descendantUuid
            ) SELECT
                ascendantUuid
            FROM
                grants
        );
$$;

CREATE OR REPLACE FUNCTION isPermissionGrantedToSubject(permissionId uuid, subjectId uuid)
    RETURNS BOOL
    STABLE LEAKPROOF
    LANGUAGE sql AS $$
    SELECT EXISTS (
        SELECT * FROM RbacUser WHERE uuid IN (
            WITH RECURSIVE grants AS (
                SELECT
                    descendantUuid,
                    ascendantUuid
                FROM
                    RbacGrants g
                WHERE
                    g.descendantUuid = permissionId
                UNION ALL
                SELECT
                    g.descendantUuid,
                    g.ascendantUuid
                FROM
                    RbacGrants g
                        INNER JOIN grants recur ON recur.ascendantUuid = g.descendantUuid
            ) SELECT
                ascendantUuid
            FROM
                grants
            WHERE ascendantUuid=subjectId
        )
    );
$$;

-- ========================================================
-- current user + assumed roles
-- --------------------------------------------------------

CREATE OR REPLACE FUNCTION currentUser()
    RETURNS varchar(63)
    STABLE LEAKPROOF
    LANGUAGE plpgsql AS $$
DECLARE
    currentUser VARCHAR(63);
BEGIN
    BEGIN
        currentUser := current_setting('hsadminng.currentUser');
    EXCEPTION WHEN OTHERS THEN
        currentUser := NULL;
    END;
    IF (currentUser IS NULL OR currentUser = '') THEN
        RAISE EXCEPTION 'hsadminng.currentUser must be defined, please use "SET LOCAL ...;"';
    END IF;
    RETURN currentUser;
END; $$;

SET SESSION AUTHORIZATION DEFAULT;
CREATE OR REPLACE FUNCTION currentUserId()
    RETURNS uuid
    STABLE LEAKPROOF
    LANGUAGE plpgsql AS $$
DECLARE
    currentUser VARCHAR(63);
    currentUserId uuid;
BEGIN
    currentUser := currentUser();
    currentUserId = (SELECT uuid FROM RbacUser WHERE name = currentUser);
    RETURN currentUserId;
END; $$;


CREATE OR REPLACE FUNCTION assumedRoles()
    RETURNS varchar(63)[]
    STABLE LEAKPROOF
    LANGUAGE plpgsql AS $$
DECLARE
    currentSubject VARCHAR(63);
BEGIN
    BEGIN
        currentSubject := current_setting('hsadminng.assumedRoles');
        EXCEPTION WHEN OTHERS THEN
            RETURN ARRAY[]::varchar[];
        END;
        IF (currentSubject = '') THEN
            RETURN ARRAY[]::varchar[];
        END IF;
        RETURN string_to_array(currentSubject, ';');
    END; $$;

CREATE OR REPLACE FUNCTION findUuidByIdName(objectTable varchar, objectIdName varchar)
    RETURNS uuid
    RETURNS NULL ON NULL INPUT
    LANGUAGE plpgsql AS $$
DECLARE

BEGIN
    /*sql = 'E ' || baseTable || '_historicize' ||
                       ' AFTER INSERT OR DELETE OR UPDATE ON ' || baseTable ||
                       '   FOR EACH ROW EXECUTE PROCEDURE historicize()';
    RAISE NOTICE 'sql: %', createTriggerSQL;
    EXECUTE createTriggerSQ*/

    RETURN customerUuidByIdName(objectIdName);
END; $$;

ROLLBACK;
SET SESSION AUTHORIZATION DEFAULT;
CREATE OR REPLACE FUNCTION currentSubjectIds()
    RETURNS uuid[]
    STABLE LEAKPROOF
    LANGUAGE plpgsql AS $$
DECLARE
    currentUserId           uuid;
    roleNames               VARCHAR(63)[];
    roleName                VARCHAR(63);
    objectTableToAssume     VARCHAR(63);
    objectNameToAssume      VARCHAR(63);
    objectUuidToAssume      uuid;
    roleTypeToAssume        RbacRoleType;
    roleIdsToAssume         uuid[];
    roleUuidToAssume        uuid;
BEGIN
    currentUserId := currentUserId();
    roleNames := assumedRoles();
    IF ( CARDINALITY(roleNames) = 0 ) THEN
        RETURN ARRAY[currentUserId];
    END IF;

    RAISE NOTICE 'assuming roles: %', roleNames;

    FOREACH roleName IN ARRAY roleNames LOOP
        roleName = overlay(roleName placing '#' from length(roleName) + 1 - strpos(reverse(roleName), '.'));
        objectTableToAssume = split_part(roleName, '#', 1);
        objectNameToAssume = split_part(roleName, '#', 2);
        roleTypeToAssume = split_part(roleName, '#', 3);

        objectUuidToAssume = findUuidByIdName(objectTableToAssume, objectNameToAssume);

        -- TODO: either the result needs to be cached at least per transaction or we need to get rid of SELCT in a loop
        SELECT uuid AS roleuuidToAssume
          FROM RbacRole r
         WHERE r.objectUuid=objectUuidToAssume AND r.roleType=roleTypeToAssume INTO roleUuidToAssume;
        IF ( NOT isGranted(currentUserId, roleUuidToAssume) ) THEN
            RAISE EXCEPTION 'user % has no permission to assume role %', currentUser(), roleUuidToAssume;
        END IF;
        roleIdsToAssume := roleIdsToAssume || roleUuidToAssume;
    END LOOP;

    RETURN roleIdsToAssume;
END; $$;
