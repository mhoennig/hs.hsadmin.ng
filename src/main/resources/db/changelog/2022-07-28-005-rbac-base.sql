--liquibase formatted sql

--changeset rbac-base-reference:1 endDelimiter:--//
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

--changeset rbac-base-user:1 endDelimiter:--//
/*

 */
create table RbacUser
(
    uuid uuid primary key references RbacReference (uuid) on delete cascade,
    name varchar(63) not null unique
);

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

--changeset rbac-base-object:1 endDelimiter:--//
/*

 */
create table RbacObject
(
    uuid        uuid primary key default uuid_generate_v4(),
    objectTable varchar(64) not null,
    unique (objectTable, uuid)
);

create or replace function createRbacObject()
    returns trigger
    language plpgsql
    strict as $$
declare
    objectUuid uuid;
begin
    if TG_OP = 'INSERT' then
        insert
            into RbacObject (objectTable)
            values (TG_TABLE_NAME)
            returning uuid into objectUuid;
        NEW.uuid = objectUuid;
        return NEW;
    else
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;
end; $$;


--//

--changeset rbac-base-role:1 endDelimiter:--//
/*

 */

create type RbacRoleType as enum ('owner', 'admin', 'tenant');

create table RbacRole
(
    uuid       uuid primary key references RbacReference (uuid) on delete cascade,
    objectUuid uuid references RbacObject (uuid) not null,
    roleType   RbacRoleType                      not null
);

create type RbacRoleDescriptor as
(
    objectTable varchar(63), -- TODO: needed? remove?
    objectUuid  uuid,
    roleType    RbacRoleType
);

create or replace function roleDescriptor(objectTable varchar(63), objectUuid uuid, roleType RbacRoleType)
    returns RbacRoleDescriptor
    returns null on null input
    -- STABLE LEAKPROOF
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

--changeset rbac-base-permission:1 endDelimiter:--//
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

-- DROP TABLE IF EXISTS RbacPermission;
create table RbacPermission
(
    uuid       uuid primary key references RbacReference (uuid) on delete cascade,
    objectUuid uuid   not null references RbacObject,
    op         RbacOp not null,
    unique (objectUuid, op)
);

-- SET SESSION SESSION AUTHORIZATION DEFAULT;
-- alter table rbacpermission add constraint rbacpermission_objectuuid_fkey foreign key (objectUuid) references rbacobject(uuid);
-- alter table rbacpermission drop constraint rbacpermission_objectuuid;

create or replace function hasPermission(forObjectUuid uuid, forOp RbacOp)
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

--changeset rbac-base-grants:1 endDelimiter:--//
/*

 */
create table RbacGrants
(
    ascendantUuid  uuid references RbacReference (uuid) on delete cascade,
    descendantUuid uuid references RbacReference (uuid) on delete cascade,
    follow         boolean not null default true,
    primary key (ascendantUuid, descendantUuid)
);
create index on RbacGrants (ascendantUuid);
create index on RbacGrants (descendantUuid);


--//

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
                into RbacGrants (ascendantUuid, descendantUuid, follow)
                values (roleUuid, permissionIds[i], true);
        end loop;
end;
$$;

create or replace procedure grantRoleToRole(subRoleId uuid, superRoleId uuid, doFollow bool = true)
    language plpgsql as $$
begin
    perform assertReferenceType('superRoleId (ascendant)', superRoleId, 'RbacRole');
    perform assertReferenceType('subRoleId (descendant)', subRoleId, 'RbacRole');

    if (isGranted(subRoleId, superRoleId)) then
        raise exception 'Cyclic role grant detected between % and %', subRoleId, superRoleId;
    end if;

    insert
        into RbacGrants (ascendantUuid, descendantUuid, follow)
        values (superRoleId, subRoleId, doFollow)
    on conflict do nothing; -- TODO: remove?
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

create or replace procedure grantRoleToUser(roleId uuid, userId uuid)
    language plpgsql as $$
begin
    perform assertReferenceType('roleId (ascendant)', roleId, 'RbacRole');
    perform assertReferenceType('userId (descendant)', userId, 'RbacUser');

    insert
        into RbacGrants (ascendantUuid, descendantUuid, follow)
        values (userId, roleId, true)
    on conflict do nothing; -- TODO: remove?
end; $$;
--//

--changeset rbac-base-query-accessible-object-uuids:1 endDelimiter:--//
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
                                                         where follow
                                                           and ascendantUuid = any (subjectIds)
                                                     union
                                                     distinct
                                                     select "grant".descendantUuid, "grant".ascendantUuid, level + 1 as level
                                                         from RbacGrants "grant"
                                                                  inner join grants recur on recur.descendantUuid = "grant".ascendantUuid
                                                         where follow)
                           select descendantUuid
                               from grants) as granted
                              join RbacPermission perm
                                   on granted.descendantUuid = perm.uuid and perm.op in ('*', requiredOp)
                              join RbacObject obj on obj.uuid = perm.objectUuid and obj.objectTable = forObjectTable
                     limit maxObjects + 1;

    foundRows = lastRowCount();
    if foundRows > maxObjects then
        raise exception 'Too many accessible objects, limit is %, found %.', maxObjects, foundRows
            using
                errcode = 'P0003',
                hint = 'Please assume a sub-role and try again.';
    end if;
end;
$$;

--//

--changeset rbac-base-query-granted-permissions:1 endDelimiter:--//
/*

 */
create or replace function queryGrantedPermissionsOfSubjectIds(requiredOp RbacOp, subjectIds uuid[])
    returns setof RbacPermission
    strict
    language sql as $$
select distinct *
    from RbacPermission
    where op = '*'
       or op = requiredOp
        and uuid in (with recursive grants as (select distinct descendantUuid,
                                                               ascendantUuid
                                                   from RbacGrants
                                                   where ascendantUuid = any (subjectIds)
                                               union all
                                               select "grant".descendantUuid,
                                                      "grant".ascendantUuid
                                                   from RbacGrants "grant"
                                                            inner join grants recur on recur.descendantUuid = "grant".ascendantUuid)
                     select descendantUuid
                         from grants);
$$;

--//

--changeset rbac-base-query-users-with-permission-for-object:1 endDelimiter:--//
/*

 */

create or replace function queryAllRbacUsersWithPermissionsFor(objectId uuid)
    returns setof RbacUser
    returns null on null input
    language sql as $$
select *
    from RbacUser
    where uuid in (with recursive grants as (select descendantUuid,
                                                    ascendantUuid
                                                 from RbacGrants
                                                 where descendantUuid = objectId
                                             union all
                                             select "grant".descendantUuid,
                                                    "grant".ascendantUuid
                                                 from RbacGrants "grant"
                                                          inner join grants recur on recur.ascendantUuid = "grant".descendantUuid)
                   select ascendantUuid
                       from grants);
$$;

--//

--changeset rbac-current-user:1 endDelimiter:--//
/*

 */
create or replace function currentUser()
    returns varchar(63)
    stable leakproof
    language plpgsql as $$
declare
    currentUser varchar(63);
begin
    begin
        currentUser := current_setting('hsadminng.currentUser');
    exception
        when others then
            currentUser := null;
    end;
    if (currentUser is null or currentUser = '') then
        raise exception 'hsadminng.currentUser must be defined, please use "SET LOCAL ...;"';
    end if;
    return currentUser;
end; $$;

create or replace function currentUserId()
    returns uuid
    stable leakproof
    language plpgsql as $$
declare
    currentUser   varchar(63);
    currentUserId uuid;
begin
    currentUser := currentUser();
    currentUserId = (select uuid from RbacUser where name = currentUser);
    return currentUserId;
end; $$;


--//

--changeset rbac-assumed-roles:1 endDelimiter:--//
/*

 */
create or replace function assumedRoles()
    returns varchar(63)[]
    stable leakproof
    language plpgsql as $$
declare
    currentSubject varchar(63);
begin
    begin
        currentSubject := current_setting('hsadminng.assumedRoles');
    exception
        when others then
            return array []::varchar[];
    end;
    if (currentSubject = '') then
        return array []::varchar[];
    end if;
    return string_to_array(currentSubject, ';');
end; $$;

create or replace function pureIdentifier(rawIdentifier varchar)
    returns uuid
    returns null on null input
    language plpgsql as $$
begin
    return regexp_replace(rawIdentifier, '\W+', '');
end; $$;

create or replace function findUuidByIdName(objectTable varchar, objectIdName varchar)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    sql varchar;
begin
    objectTable := pureIdentifier(objectTable);
    objectIdName := pureIdentifier(objectIdName);
    sql := objectTable || 'UuidByIdName(' || objectIdName || ');';
    execute sql;
end; $$;

create or replace function currentSubjectIds()
    returns uuid[]
    stable leakproof
    language plpgsql as $$
declare
    currentUserId       uuid;
    roleNames           varchar(63)[];
    roleName            varchar(63);
    objectTableToAssume varchar(63);
    objectNameToAssume  varchar(63);
    objectUuidToAssume  uuid;
    roleTypeToAssume    RbacRoleType;
    roleIdsToAssume     uuid[];
    roleUuidToAssume    uuid;
begin
    currentUserId := currentUserId();
    roleNames := assumedRoles();
    if (cardinality(roleNames) = 0) then
        return array [currentUserId];
    end if;

    raise notice 'assuming roles: %', roleNames;

    foreach roleName in array roleNames
        loop
            roleName = overlay(roleName placing '#' from length(roleName) + 1 - strpos(reverse(roleName), '.'));
            objectTableToAssume = split_part(roleName, '#', 1);
            objectNameToAssume = split_part(roleName, '#', 2);
            roleTypeToAssume = split_part(roleName, '#', 3);

            objectUuidToAssume = findUuidByIdName(objectTableToAssume, objectNameToAssume);

            -- TODO: either the result needs to be cached at least per transaction or we need to get rid of SELCT in a loop
            select uuid as roleuuidToAssume
                from RbacRole r
                where r.objectUuid = objectUuidToAssume
                  and r.roleType = roleTypeToAssume
                into roleUuidToAssume;
            if (not isGranted(currentUserId, roleUuidToAssume)) then
                raise exception 'user % has no permission to assume role %', currentUser(), roleUuidToAssume;
            end if;
            roleIdsToAssume := roleIdsToAssume || roleUuidToAssume;
        end loop;

    return roleIdsToAssume;
end; $$;
--//


-- ============================================================================
-- PGSQL-ROLES
--changeset rbac-base-pgsql-roles:1 endDelimiter:--//
-- ------------------------------------------------------------------

create role admin;
grant all privileges on all tables in schema public to admin;

create role restricted;
grant all privileges on all tables in schema public to restricted;

--//
