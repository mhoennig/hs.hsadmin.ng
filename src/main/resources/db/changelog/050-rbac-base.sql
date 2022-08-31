--liquibase formatted sql

-- ============================================================================
--changeset rbac-base-REFERENCE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*

 */
create type ReferenceType as enum ('RbacUser', 'RbacRole', 'RbacPermission');

create table RbacReference
(
    uuid uuid unique default uuid_generate_v4(),
    type ReferenceType not null
);

create or replace function assertReferenceType(argument varchar, referenceId uuid, expectedType ReferenceType)
    returns ReferenceType
    language plpgsql as $$
declare
    actualType ReferenceType;
begin
    actualType = (select type from RbacReference where uuid = referenceId);
    if (actualType <> expectedType) then
        raise exception '% must reference a %, but got a %', argument, expectedType, actualType;
    end if;
    return expectedType;
end; $$;
--//

-- ============================================================================
--changeset rbac-base-USER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*

 */
create table RbacUser
(
    uuid uuid primary key references RbacReference (uuid) on delete cascade,
    name varchar(63) not null unique
);

call create_journal('RbacUser');

create or replace function createRbacUser(userName varchar)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    objectId uuid;
begin
    insert
        into RbacReference (type)
        values ('RbacUser')
        returning uuid into objectId;
    insert
        into RbacUser (uuid, name)
        values (objectid, userName);
    return objectId;
end;
$$;

create or replace function createRbacUser(refUuid uuid, userName varchar)
    returns uuid
    called on null input
    language plpgsql as $$
begin
    insert
        into RbacReference as r (uuid, type)
        values (coalesce(refUuid, uuid_generate_v4()), 'RbacUser')
        returning r.uuid into refUuid;
    insert
        into RbacUser (uuid, name)
        values (refUuid, userName);
    return refUuid;
end;
$$;

create or replace function findRbacUserId(userName varchar)
    returns uuid
    returns null on null input
    language sql as $$
select uuid from RbacUser where name = userName
$$;

create type RbacWhenNotExists as enum ('fail', 'create');

create or replace function getRbacUserId(userName varchar, whenNotExists RbacWhenNotExists)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    userUuid uuid;
begin
    userUuid = findRbacUserId(userName);
    if (userUuid is null) then
        if (whenNotExists = 'fail') then
            raise exception 'RbacUser with name="%" not found', userName;
        end if;
        if (whenNotExists = 'create') then
            userUuid = createRbacUser(userName);
        end if;
    end if;
    return userUuid;
end;
$$;

--//

-- ============================================================================
--changeset rbac-base-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*

 */
create table RbacObject
(
    uuid        uuid primary key default uuid_generate_v4(),
    objectTable varchar(64) not null,
    unique (objectTable, uuid)
);

call create_journal('RbacObject');

create or replace function createRbacObject()
    returns trigger
    language plpgsql
    strict as $$
declare
    objectUuid uuid;
begin
    if TG_OP = 'INSERT' then
        if NEW.uuid is null then
            insert
                into RbacObject (objectTable)
                values (TG_TABLE_NAME)
                returning uuid into objectUuid;
            NEW.uuid = objectUuid;
        else
            insert
                into RbacObject (uuid, objectTable)
                values (NEW.uuid, TG_TABLE_NAME)
                returning uuid into objectUuid;
        end if;
        return NEW;
    else
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;
end; $$;
--//

-- ============================================================================
--changeset rbac-base-ROLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*

 */

create type RbacRoleType as enum ('owner', 'admin', 'tenant');

create table RbacRole
(
    uuid       uuid primary key references RbacReference (uuid) on delete cascade,
    objectUuid uuid references RbacObject (uuid) not null,
    roleType   RbacRoleType                      not null,
    unique (objectUuid, roleType)
);

call create_journal('RbacRole');

create type RbacRoleDescriptor as
(
    objectTable varchar(63), -- TODO: needed? remove?
    objectUuid  uuid,
    roleType    RbacRoleType
);

create or replace function roleDescriptor(objectTable varchar(63), objectUuid uuid, roleType RbacRoleType)
    returns RbacRoleDescriptor
    returns null on null input
    stable leakproof
    language sql as $$
select objectTable, objectUuid, roleType::RbacRoleType;
$$;

create or replace function createRole(roleDescriptor RbacRoleDescriptor)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    referenceId uuid;
begin
    insert
        into RbacReference (type)
        values ('RbacRole')
        returning uuid into referenceId;
    insert
        into RbacRole (uuid, objectUuid, roleType)
        values (referenceId, roleDescriptor.objectUuid, roleDescriptor.roleType);
    return referenceId;
end;
$$;


create or replace procedure deleteRole(roleUUid uuid)
    language plpgsql as $$
begin
    delete from RbacRole where uuid = roleUUid;
end;
$$;

create or replace function findRoleId(roleIdName varchar)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    roleParts                 text;
    roleTypeFromRoleIdName    RbacRoleType;
    objectNameFromRoleIdName  text;
    objectTableFromRoleIdName text;
    objectUuidOfRole          uuid;
    roleUuid                  uuid;
begin
    -- TODO: extract function toRbacRoleDescriptor(roleIdName varchar) + find other occurrences
    roleParts = overlay(roleIdName placing '#' from length(roleIdName) + 1 - strpos(reverse(roleIdName), '.'));
    objectTableFromRoleIdName = split_part(roleParts, '#', 1);
    objectNameFromRoleIdName = split_part(roleParts, '#', 2);
    roleTypeFromRoleIdName = split_part(roleParts, '#', 3);
    objectUuidOfRole = findObjectUuidByIdName(objectTableFromRoleIdName, objectNameFromRoleIdName);

    raise notice $sql$findObjectUuidByIdName('%', '%') = %;$sql$, objectTableFromRoleIdName, objectNameFromRoleIdName, objectUuidOfRole;
    raise notice 'finding %, % (%), %', objectTableFromRoleIdName, objectNameFromRoleIdName, objectUuidOfRole, roleTypeFromRoleIdName;

    select uuid
        from RbacRole
        where objectUuid = objectUuidOfRole
          and roleType = roleTypeFromRoleIdName
        into roleUuid;
    return roleUuid;
end; $$;

create or replace function findRoleId(roleDescriptor RbacRoleDescriptor)
    returns uuid
    returns null on null input
    language sql as $$
select uuid from RbacRole where objectUuid = roleDescriptor.objectUuid and roleType = roleDescriptor.roleType;
$$;

create or replace function getRoleId(roleDescriptor RbacRoleDescriptor, whenNotExists RbacWhenNotExists)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    roleUuid uuid;
begin
    roleUuid = findRoleId(roleDescriptor);
    if (roleUuid is null) then
        if (whenNotExists = 'fail') then
            raise exception 'RbacRole "%#%.%" not found', roleDescriptor.objectTable, roleDescriptor.objectUuid, roleDescriptor.roleType;
        end if;
        if (whenNotExists = 'create') then
            roleUuid = createRole(roleDescriptor);
        end if;
    end if;
    return roleUuid;
end;
$$;

-- ============================================================================
--changeset rbac-base-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*

 */
create domain RbacOp as varchar(67)
    check (
                VALUE = '*'
            or VALUE = 'delete'
            or VALUE = 'edit'
            or VALUE = 'view'
            or VALUE = 'assume'
            or VALUE ~ '^add-[a-z]+$'
        );

create table RbacPermission
(
    uuid       uuid primary key references RbacReference (uuid) on delete cascade,
    objectUuid uuid   not null references RbacObject,
    op         RbacOp not null,
    unique (objectUuid, op)
);

call create_journal('RbacPermission');

create or replace function permissionExists(forObjectUuid uuid, forOp RbacOp)
    returns bool
    language sql as $$
select exists(
           select op
               from RbacPermission p
               where p.objectUuid = forObjectUuid
                 and p.op in ('*', forOp)
           );
$$;

create or replace function createPermissions(forObjectUuid uuid, permitOps RbacOp[])
    returns uuid[]
    language plpgsql as $$
declare
    refId         uuid;
    permissionIds uuid[] = array []::uuid[];
begin
    raise notice 'createPermission for: % %', forObjectUuid, permitOps;
    if (forObjectUuid is null) then
        raise exception 'forObjectUuid must not be null';
    end if;
    if (array_length(permitOps, 1) > 1 and '*' = any (permitOps)) then
        raise exception '"*" operation must not be assigned along with other operations: %', permitOps;
    end if;

    for i in array_lower(permitOps, 1)..array_upper(permitOps, 1)
        loop
            refId = (select uuid from RbacPermission where objectUuid = forObjectUuid and op = permitOps[i]);
            if (refId is null) then
                raise notice 'createPermission: % %', forObjectUuid, permitOps[i];
                insert
                    into RbacReference ("type")
                    values ('RbacPermission')
                    returning uuid into refId;
                insert
                    into RbacPermission (uuid, objectUuid, op)
                    values (refId, forObjectUuid, permitOps[i]);
            end if;
            raise notice 'addPermission: %', refId;
            permissionIds = permissionIds || refId;
        end loop;

    raise notice 'createPermissions returning: %', permissionIds;
    return permissionIds;
end;
$$;

create or replace function findPermissionId(forObjectUuid uuid, forOp RbacOp)
    returns uuid
    returns null on null input
    stable leakproof
    language sql as $$
select uuid
    from RbacPermission p
    where p.objectUuid = forObjectUuid
      and p.op in ('*', forOp)
$$;

--//

-- ============================================================================
--changeset rbac-base-GRANTS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Table to store grants / role- or permission assignments to users or roles.
 */
create table RbacGrants
(
    uuid                uuid primary key default uuid_generate_v4(),
    grantedByRoleUuid   uuid references RbacRole (uuid) on delete cascade,
    ascendantUuid       uuid references RbacReference (uuid) on delete cascade,
    descendantUuid      uuid references RbacReference (uuid) on delete cascade,
    assumed             boolean not null default true,  -- auto assumed (true) vs. needs assumeRoles (false)
    unique (ascendantUuid, descendantUuid)
);
create index on RbacGrants (ascendantUuid);
create index on RbacGrants (descendantUuid);

call create_journal('RbacGrants');

create or replace function findGrantees(grantedId uuid)
    returns setof RbacReference
    returns null on null input
    language sql as $$
select reference.*
    from (with recursive grants as (select descendantUuid,
                                           ascendantUuid
                                        from RbacGrants
                                        where descendantUuid = grantedId
                                    union all
                                    select "grant".descendantUuid,
                                           "grant".ascendantUuid
                                        from RbacGrants "grant"
                                                 inner join grants recur on recur.ascendantUuid = "grant".descendantUuid)
          select ascendantUuid
              from grants) as grantee
             join RbacReference reference on reference.uuid = grantee.ascendantUuid;
$$;

create or replace function isGranted(granteeId uuid, grantedId uuid)
    returns bool
    returns null on null input
    language sql as $$
select granteeId = grantedId or granteeId in (with recursive grants as (select descendantUuid, ascendantUuid
                                                                            from RbacGrants
                                                                            where descendantUuid = grantedId
                                                                        union all
                                                                        select "grant".descendantUuid, "grant".ascendantUuid
                                                                            from RbacGrants "grant"
                                                                                     inner join grants recur on recur.ascendantUuid = "grant".descendantUuid)
                                              select ascendantUuid
                                                  from grants);
$$;

create or replace function isGranted(granteeIds uuid[], grantedId uuid)
    returns bool
    returns null on null input
    language plpgsql as $$
declare
    granteeId uuid;
begin
    -- TODO: needs optimization
    foreach granteeId in array granteeIds
        loop
            if isGranted(granteeId, grantedId) then
                return true;
            end if;
        end loop;
    return false;
end; $$;

create or replace function isPermissionGrantedToSubject(permissionId uuid, subjectId uuid)
    returns BOOL
    stable leakproof
    language sql as $$
select exists(
           select *
               from RbacUser
               where uuid in (with recursive grants as (select descendantUuid,
                                                               ascendantUuid
                                                            from RbacGrants g
                                                            where g.descendantUuid = permissionId
                                                        union all
                                                        select g.descendantUuid,
                                                               g.ascendantUuid
                                                            from RbacGrants g
                                                                     inner join grants recur on recur.ascendantUuid = g.descendantUuid)
                              select ascendantUuid
                                  from grants
                                  where ascendantUuid = subjectId)
           );
$$;

create or replace function hasGlobalRoleGranted(userUuid uuid)
    returns bool
    stable leakproof
    language sql as $$
select exists(
           select r.uuid
               from RbacGrants as g
                        join RbacRole as r on r.uuid = g.descendantuuid
                        join RbacObject as o on o.uuid = r.objectuuid
               where g.ascendantuuid = userUuid
                 and o.objecttable = 'global'
           );
$$;

create or replace procedure grantPermissionsToRole(roleUuid uuid, permissionIds uuid[])
    language plpgsql as $$
begin
    raise notice 'grantPermissionsToRole: % -> %', roleUuid, permissionIds;
    if cardinality(permissionIds) = 0 then return; end if;

    for i in array_lower(permissionIds, 1)..array_upper(permissionIds, 1)
        loop
            perform assertReferenceType('roleId (ascendant)', roleUuid, 'RbacRole');
            perform assertReferenceType('permissionId (descendant)', permissionIds[i], 'RbacPermission');

            insert
                into RbacGrants (ascendantUuid, descendantUuid, assumed)
                values (roleUuid, permissionIds[i], true)
            on conflict do nothing; -- allow granting multiple times
        end loop;
end;
$$;

create or replace procedure grantRoleToRole(subRoleId uuid, superRoleId uuid, doAssume bool = true)
    language plpgsql as $$
begin
    perform assertReferenceType('superRoleId (ascendant)', superRoleId, 'RbacRole');
    perform assertReferenceType('subRoleId (descendant)', subRoleId, 'RbacRole');

    if isGranted(subRoleId, superRoleId) then
        raise exception '[400] Cyclic role grant detected between % and %', subRoleId, superRoleId;
    end if;

    insert
        into RbacGrants (ascendantuuid, descendantUuid, assumed)
        values (superRoleId, subRoleId, doAssume)
    on conflict do nothing; -- allow granting multiple times
end; $$;

create or replace procedure revokeRoleFromRole(subRoleId uuid, superRoleId uuid)
    language plpgsql as $$
begin
    perform assertReferenceType('superRoleId (ascendant)', superRoleId, 'RbacRole');
    perform assertReferenceType('subRoleId (descendant)', subRoleId, 'RbacRole');

    if (isGranted(subRoleId, superRoleId)) then
        delete from RbacGrants where ascendantUuid = superRoleId and descendantUuid = subRoleId;
    end if;
end; $$;

-- ============================================================================
--changeset rbac-base-QUERY-ACCESSIBLE-OBJECT-UUIDS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*

 */
create or replace function queryAccessibleObjectUuidsOfSubjectIds(
    requiredOp RbacOp,
    forObjectTable varchar, -- reduces the result set, but is not really faster when used in restricted view
    subjectIds uuid[],
    maxObjects integer = 8000)
    returns setof uuid
    returns null on null input
    language plpgsql as $$
declare
    foundRows bigint;
begin
    return query select distinct perm.objectUuid
                     from (with recursive grants as (select descendantUuid, ascendantUuid, 1 as level
                                                         from RbacGrants
                                                         where assumed
                                                           and ascendantUuid = any (subjectIds)
                                                     union
                                                     distinct
                                                     select "grant".descendantUuid, "grant".ascendantUuid, level + 1 as level
                                                         from RbacGrants "grant"
                                                                  inner join grants recur on recur.descendantUuid = "grant".ascendantUuid
                                                         where assumed)
                           select descendantUuid
                               from grants) as granted
                              join RbacPermission perm
                                   on granted.descendantUuid = perm.uuid and perm.op in ('*', requiredOp)
                              join RbacObject obj on obj.uuid = perm.objectUuid and obj.objectTable = forObjectTable
                     limit maxObjects + 1;

    foundRows = lastRowCount();
    if foundRows > maxObjects then
        raise exception '[400] Too many accessible objects, limit is %, found %.', maxObjects, foundRows
            using
                errcode = 'P0003',
                hint = 'Please assume a sub-role and try again.';
    end if;
end;
$$;

--//

-- ============================================================================
--changeset rbac-base-QUERY-GRANTED-PERMISSIONS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns all permissions accessible to the given subject UUID (user or role).
 */
create or replace function queryPermissionsGrantedToSubjectId(subjectId uuid)
    returns setof RbacPermission
    strict
    language sql as $$
    -- @formatter:off
select *
    from RbacPermission
    where uuid in (
            with recursive grants as (
                select distinct descendantUuid, ascendantUuid
                    from RbacGrants
                    where ascendantUuid = subjectId
                union all
                select "grant".descendantUuid, "grant".ascendantUuid
                    from RbacGrants "grant"
                    inner join grants recur on recur.descendantUuid = "grant".ascendantUuid
            )
            select descendantUuid
                from grants
        );
-- @formatter:on
$$;
--//

-- ============================================================================
--changeset rbac-base-QUERY-USERS-WITH-PERMISSION-FOR-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns all user UUIDs which have any permission for the given object UUID.
 */

create or replace function queryAllRbacUsersWithPermissionsFor(objectId uuid)
    returns setof RbacUser
    returns null on null input
    language sql as $$
select *
    from RbacUser
    where uuid in (
        -- @formatter:off
        with recursive grants as (
            select descendantUuid, ascendantUuid
                from RbacGrants
                where descendantUuid = objectId
            union all
            select "grant".descendantUuid, "grant".ascendantUuid
                from RbacGrants "grant"
                inner join grants recur on recur.ascendantUuid = "grant".descendantUuid
        )
        -- @formatter:on
        select ascendantUuid
            from grants);
$$;
--//


-- ============================================================================
--changeset rbac-base-PGSQL-ROLES:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create role admin;
grant all privileges on all tables in schema public to admin;

create role restricted;
grant all privileges on all tables in schema public to restricted;

--//
