--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:rbac-base-REFERENCE endDelimiter:--//
-- ----------------------------------------------------------------------------

create type rbac.ReferenceType as enum ('rbac.subject', 'rbac.role', 'rbac.permission');

create table rbac.reference
(
    uuid uuid unique default uuid_generate_v4(),
    type rbac.ReferenceType not null
);

create or replace function rbac.assertReferenceType(argument varchar, referenceId uuid, expectedType rbac.ReferenceType)
    returns rbac.ReferenceType
    language plpgsql as $$
declare
    actualType rbac.ReferenceType;
begin
    if referenceId is null then
        raise exception '% must be a % and not null', argument, expectedType;
    end if;

    actualType = (select type from  rbac.reference where uuid = referenceId);
    if (actualType <> expectedType) then
        raise exception '% must reference a %, but got a %', argument, expectedType, actualType;
    end if;
    return expectedType;
end; $$;
--//

-- ============================================================================
--changeset michael.hoennig:rbac-base-SUBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
/*

 */
create table rbac.subject
(
    uuid uuid primary key references  rbac.reference (uuid) on delete cascade,
    name varchar(63) not null unique
);

call base.create_journal('rbac.subject');

create or replace function rbac.create_subject(subjectName varchar)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    objectId uuid;
begin
    insert
        into  rbac.reference (type)
        values ('rbac.subject')
        returning uuid into objectId;
    insert
        into rbac.subject (uuid, name)
        values (objectid, subjectName);
    return objectId;
end;
$$;

create or replace function rbac.create_subject(refUuid uuid, subjectName varchar)
    returns uuid
    called on null input
    language plpgsql as $$
begin
    insert
        into  rbac.reference as r (uuid, type)
        values (coalesce(refUuid, uuid_generate_v4()), 'rbac.subject')
        returning r.uuid into refUuid;
    insert
        into rbac.subject (uuid, name)
        values (refUuid, subjectName);
    return refUuid;
end;
$$;

create or replace function rbac.find_subject_id(subjectName varchar)
    returns uuid
    returns null on null input
    language sql as $$
select uuid from rbac.subject where name = subjectName
$$;
--//

-- ============================================================================
--changeset michael.hoennig:rbac-base-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
/*

 */
create table rbac.object
(
    uuid        uuid primary key default uuid_generate_v4(),
    serialId    serial, -- TODO.perf: only needed for reverse deletion of temp test data
    objectTable varchar(64) not null,
    unique (objectTable, uuid)
);

call base.create_journal('rbac.object');

--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-GENERATE-RELATED-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Inserts related rbac.object for use in the BEFORE INSERT TRIGGERs on the business objects.
 */
create or replace function rbac.insert_related_object()
    returns trigger
    language plpgsql
    strict as $$
declare
    objectUuid uuid;
    tableSchemaAndName text;
begin
    tableSchemaAndName := base.combine_table_schema_and_name(TG_TABLE_SCHEMA, TG_TABLE_NAME);
    if TG_OP = 'INSERT' then
        if NEW.uuid is null then
            insert
                into rbac.object (objectTable)
                values (tableSchemaAndName)
                returning uuid into objectUuid;
            NEW.uuid = objectUuid;
        else
            insert
                into rbac.object (uuid, objectTable)
                values (NEW.uuid, tableSchemaAndName)
                returning uuid into objectUuid;
        end if;
        return NEW;
    else
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;
end; $$;

/*
    Deletes related rbac.object for use in the BEFORE DELETE TRIGGERs on the business objects.
    Through cascades all related rbac roles and grants are going to be deleted as well.
 */
create or replace function rbac.delete_related_rbac_rules_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        delete from rbac.object where rbac.object.uuid = old.uuid;
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
    return old;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-ROLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create type rbac.RoleType as enum ('OWNER', 'ADMIN', 'AGENT', 'TENANT', 'GUEST', 'REFERRER');

create table rbac.role
(
    uuid       uuid primary key references rbac.reference (uuid) on delete cascade initially deferred, -- initially deferred
    objectUuid uuid not null references rbac.object (uuid) initially deferred,
    roleType   rbac.RoleType not null,
    unique (objectUuid, roleType)
);

call base.create_journal('rbac.role');
--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-ROLE-DESCRIPTOR endDelimiter:--//
-- ----------------------------------------------------------------------------

create type rbac.RoleDescriptor as
(
    objectTable varchar(63), -- for human readability and easier debugging
    objectUuid  uuid,
    roleType    rbac.RoleType,
    assumed     boolean
);

create or replace function rbac.assumed()
    returns boolean
    stable -- leakproof
    language sql as $$
        select true;
$$;

create or replace function rbac.unassumed()
    returns boolean
    stable -- leakproof
    language sql as $$
select false;
$$;

create or replace function rbac.roleDescriptorOf(
        objectTable varchar(63), objectUuid uuid, roleType rbac.RoleType,
        assumed boolean = true) -- just for DSL readability, belongs actually to the grant
    returns rbac.RoleDescriptor
    returns null on null input
    stable -- leakproof
    language sql as $$
        select objectTable, objectUuid, roleType::rbac.RoleType, assumed;
$$;

create or replace function rbac.createRole(roleDescriptor rbac.RoleDescriptor)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    referenceId uuid;
begin
    insert
        into  rbac.reference (type)
        values ('rbac.role')
        returning uuid into referenceId;
    insert
        into rbac.role (uuid, objectUuid, roleType)
        values (referenceId, roleDescriptor.objectUuid, roleDescriptor.roleType);
    return referenceId;
end;
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-IDNAME-FUNCTIONS endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function rbac.findObjectUuidByIdName(objectTable varchar, objectIdName varchar)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    sql  varchar;
    uuid uuid;
begin
    objectTable := base.pureIdentifier(objectTable);
    objectIdName := base.pureIdentifier(objectIdName);
    sql := format('select * from %s_uuid_by_id_name(%L);', objectTable, objectIdName);
    begin
        execute sql into uuid;
    exception
        when others then
            raise exception 'function %_uuid_by_id_name(''%'') failed: %, SQLSTATE: %. If the function itself could not be found, add identity view support to %\nSQL:%',
                objectTable, objectIdName, SQLERRM, SQLSTATE, objectTable, sql;
    end;
    if uuid is null then
        raise exception 'SQL returned null: %', sql;
    else
        return uuid;
    end if;
end ; $$;

create or replace function rbac.findIdNameByObjectUuid(objectTable varchar, objectUuid uuid)
    returns varchar
    returns null on null input
    language plpgsql as $$
declare
    sql    varchar;
    idName varchar;
begin
    objectTable := base.pureIdentifier(objectTable);
    sql := format('select * from %s_id_name_by_uuid(%L::uuid);', objectTable, objectUuid);
    begin
        execute sql into idName;
    exception
        when others then
            raise exception 'function %_id_name_by_uuid(''%'') failed: %, SQLSTATE: %. If the function itself could not be found, add identity view support to %',
                objectTable, objectUuid, SQLERRM, SQLSTATE, objectTable;
    end;
    return idName;
end ; $$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-ROLE-FUNCTIONS endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure rbac.deleteRole(roleUUid uuid)
    language plpgsql as $$
begin
    --raise exception '% deleting role uuid %', rbac.currentSubjectOrAssumedRolesUuids(), roleUUid;
    delete from rbac.role where uuid = roleUUid;
end;
$$;

create or replace function rbac.findRoleId(roleIdName varchar)
    returns uuid
    returns null on null input
    language plpgsql as $$
declare
    roleParts                 text;
    roleTypeFromRoleIdName    rbac.RoleType;
    objectNameFromRoleIdName  text;
    objectTableFromRoleIdName text;
    objectUuidOfRole          uuid;
    roleUuid                  uuid;
begin
    -- TODO.refa: extract function rbac.toRoleDescriptor(roleIdName varchar) + find other occurrences
    roleParts = overlay(roleIdName placing '#' from length(roleIdName) + 1 - strpos(reverse(roleIdName), ':'));
    objectTableFromRoleIdName = split_part(roleParts, '#', 1);
    objectNameFromRoleIdName = split_part(roleParts, '#', 2);
    roleTypeFromRoleIdName = split_part(roleParts, '#', 3);
    objectUuidOfRole = rbac.findObjectUuidByIdName(objectTableFromRoleIdName, objectNameFromRoleIdName);

    select uuid
        from rbac.role
        where objectUuid = objectUuidOfRole
          and roleType = roleTypeFromRoleIdName
        into roleUuid;
    return roleUuid;
end; $$;

create or replace function rbac.findRoleId(roleDescriptor rbac.RoleDescriptor)
    returns uuid
    returns null on null input
    language sql as $$
select uuid from rbac.role where objectUuid = roleDescriptor.objectUuid and roleType = roleDescriptor.roleType;
$$;

create or replace function rbac.getRoleId(roleDescriptor rbac.RoleDescriptor)
    returns uuid
    language plpgsql as $$
declare
    roleUuid uuid;
begin
    assert roleDescriptor is not null, 'roleDescriptor must not be null';

    roleUuid := rbac.findRoleId(roleDescriptor);
    if (roleUuid is null) then
        raise exception 'rbac.role "%#%.%" not found', roleDescriptor.objectTable, roleDescriptor.objectUuid, roleDescriptor.roleType;
    end if;
    return roleUuid;
end;
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-BEFORE-DELETE-ROLE-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    rbac.role BEFORE DELETE TRIGGER function which deletes all related roles.
 */
create or replace function rbac.delete_grants_of_role_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        delete from rbac.grant g where old.uuid in (g.grantedbyroleuuid, g.ascendantuuid, g.descendantuuid);
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
    return old;
end; $$;

/*
    Installs the rbac.role BEFORE DELETE TRIGGER.
 */
create trigger delete_grants_of_role_tg
    before delete
    on rbac.role
    for each row
execute procedure rbac.delete_grants_of_role_tf();
--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-BEFORE-DELETE-OBJECT-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    rbac.object BEFORE DELETE TRIGGER function which deletes all related roles.
 */
create or replace function rbac.delete_roles_of_object_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        delete from rbac.permission p where p.objectuuid = old.uuid;
        delete from rbac.role r where r.objectUuid = old.uuid;
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
    return old;
end; $$;

/*
    Installs the rbac.role BEFORE DELETE TRIGGER.
 */
create trigger delete_roles_of_object_tg
    before delete
    on rbac.object
    for each row
        execute procedure rbac.delete_roles_of_object_tf();
--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------
create domain rbac.RbacOp as varchar(6)
    check (
               VALUE = 'DELETE'
            or VALUE = 'UPDATE'
            or VALUE = 'SELECT'
            or VALUE = 'INSERT'
            or VALUE = 'ASSUME'
        );

create table rbac.permission
(
    uuid        uuid primary key references  rbac.reference (uuid) on delete cascade,
    objectUuid  uuid not null references rbac.object,
    op          rbac.RbacOp not null,
    opTableName varchar(60)
);
-- TODO.perf: check if these indexes are really useful
create index on rbac.permission (objectUuid, op);
create index on rbac.permission (opTableName, op);

ALTER TABLE rbac.permission
    ADD CONSTRAINT unique_including_null_values UNIQUE NULLS NOT DISTINCT (objectUuid, op, opTableName);

call base.create_journal('rbac.permission');

create or replace function rbac.createPermission(forObjectUuid uuid, forOp rbac.RbacOp, forOpTableName text = null)
    returns uuid
    language plpgsql as $$
declare
    permissionUuid uuid;
begin
    if (forObjectUuid is null) then
        raise exception 'forObjectUuid must not be null';
    end if;
    if (forOp = 'INSERT' and forOpTableName is null) then
        raise exception 'INSERT permissions needs forOpTableName';
    end if;
    if (forOp <> 'INSERT' and forOpTableName is not null) then
        raise exception 'forOpTableName must only be specified for ops: [INSERT]'; -- currently no other
    end if;

    permissionUuid := (
        select uuid from rbac.permission
         where objectUuid = forObjectUuid
           and op = forOp and opTableName is not distinct from forOpTableName);
    if (permissionUuid is null) then
        insert into  rbac.reference ("type")
            values ('rbac.permission')
            returning uuid into permissionUuid;
        begin
            insert into rbac.permission (uuid, objectUuid, op, opTableName)
                values (permissionUuid, forObjectUuid, forOp, forOpTableName);
        exception
            when others then
                raise exception 'insert into rbac.permission (uuid, objectUuid, op, opTableName)
                values (%, %, %, %);', permissionUuid, forObjectUuid, forOp, forOpTableName;
        end;
    end if;
    return permissionUuid;
end; $$;

create or replace function rbac.findEffectivePermissionId(forObjectUuid uuid, forOp rbac.RbacOp, forOpTableName text = null)
    returns uuid
    returns null on null input
    stable -- leakproof
    language sql as $$
select uuid
    from rbac.permission p
    where p.objectUuid = forObjectUuid
      and (forOp = 'SELECT' or p.op = forOp) -- all other rbac.RbacOp include 'SELECT'
      and p.opTableName = forOpTableName
$$;

create or replace function rbac.findPermissionId(forObjectUuid uuid, forOp rbac.RbacOp, forOpTableName text = null)
    returns uuid
    returns null on null input
    stable -- leakproof
    language sql as $$
select uuid
    from rbac.permission p
    where p.objectUuid = forObjectUuid
      and p.op = forOp
      and p.opTableName = forOpTableName
$$;

create or replace function rbac.getPermissionId(forObjectUuid uuid, forOp rbac.RbacOp, forOpTableName text = null)
    returns uuid
    stable -- leakproof
    language plpgsql as $$
declare
    permissionUuid uuid;
begin
    select uuid into permissionUuid
        from rbac.permission p
        where p.objectUuid = forObjectUuid
          and p.op = forOp
          and forOpTableName is null or p.opTableName = forOpTableName;
    assert permissionUuid is not null,
        format('permission %s %s for object UUID %s cannot be found', forOp, forOpTableName, forObjectUuid);
    return permissionUuid;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-duplicate-role-grant-exception endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure rbac.raiseDuplicateRoleGrantException(subRoleId uuid, superRoleId uuid)
    language plpgsql as $$
declare
    subRoleIdName text;
    superRoleIdName text;
begin
    select roleIdName from rbac.role_ev where uuid=subRoleId into subRoleIdName;
    select roleIdName from rbac.role_ev where uuid=superRoleId into superRoleIdName;
    raise exception '[400] Duplicate role grant detected: role % (%) already granted to % (%)', subRoleId, subRoleIdName, superRoleId, superRoleIdName;
end;
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-GRANTS endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Table to store grants / role- or permission assignments to subjects or roles.
 */
create table rbac.grant
(
    uuid                uuid primary key default uuid_generate_v4(),
    grantedByTriggerOf  uuid references rbac.object (uuid) on delete cascade initially deferred ,
    grantedByRoleUuid   uuid references rbac.role (uuid),
    ascendantUuid       uuid references  rbac.reference (uuid),
    descendantUuid      uuid references  rbac.reference (uuid),
    assumed             boolean not null default true,  -- auto assumed (true) vs. needs assumeRoles (false)
    unique (ascendantUuid, descendantUuid),
    constraint rbacGrant_createdBy check ( grantedByRoleUuid is null or grantedByTriggerOf is null) );
create index on rbac.grant (ascendantUuid);
create index on rbac.grant (descendantUuid);

call base.create_journal('rbac.grant');
create or replace function rbac.findGrantees(grantedId uuid)
    returns setof  rbac.reference
    returns null on null input
    language sql as $$
with recursive grants as (
    select descendantUuid, ascendantUuid
        from rbac.grant
        where descendantUuid = grantedId
    union all
    select g.descendantUuid, g.ascendantUuid
        from rbac.grant g
                 inner join grants on grants.ascendantUuid = g.descendantUuid
)
select ref.*
    from grants
             join rbac.reference ref on ref.uuid = grants.ascendantUuid;
$$;

create or replace function rbac.isGranted(granteeIds uuid[], grantedId uuid)
    returns bool
    returns null on null input
    language sql as $$
with recursive grants as (
    select descendantUuid, ascendantUuid
        from rbac.grant
        where descendantUuid = grantedId
    union all
    select "grant".descendantUuid, "grant".ascendantUuid
        from rbac.grant "grant"
                 inner join grants recur on recur.ascendantUuid = "grant".descendantUuid
)
select exists (
    select true
        from grants
        where ascendantUuid = any(granteeIds)
) or grantedId = any(granteeIds);
$$;

create or replace function rbac.isGranted(granteeId uuid, grantedId uuid)
    returns bool
    returns null on null input
    language sql as $$
select * from rbac.isGranted(array[granteeId], grantedId);
$$;
create or replace function rbac.isPermissionGrantedToSubject(permissionId uuid, subjectId uuid)
    returns BOOL
    stable -- leakproof
    language sql as $$
with recursive grants as (
    select descendantUuid, ascendantUuid
        from rbac.grant
        where descendantUuid = permissionId
    union all
    select g.descendantUuid, g.ascendantUuid
        from rbac.grant g
                 inner join grants on grants.ascendantUuid = g.descendantUuid
)
select exists(
    select true
        from grants
        where ascendantUuid = subjectId
);
$$;

create or replace function rbac.hasInsertPermission(objectUuid uuid, tableName text )
    returns BOOL
    stable -- leakproof
    language plpgsql as $$
declare
    permissionUuid uuid;
begin
    permissionUuid = rbac.findPermissionId(objectUuid, 'INSERT'::rbac.RbacOp, tableName);
    return permissionUuid is not null;
end;
$$;

create or replace function rbac.hasGlobalRoleGranted(forAscendantUuid uuid)
    returns bool
    stable -- leakproof
    language sql as $$
select exists(
           select r.uuid
               from rbac.grant as g
                        join rbac.role as r on r.uuid = g.descendantuuid
                        join rbac.object as o on o.uuid = r.objectuuid
               where g.ascendantuuid = forAscendantUuid
                 and o.objecttable = 'rbac.global'
           );
$$;

create or replace procedure rbac.grantPermissionToRole(permissionUuid uuid, roleUuid uuid)
    language plpgsql as $$
begin
    perform rbac.assertReferenceType('roleId (ascendant)', roleUuid, 'rbac.role');
    perform rbac.assertReferenceType('permissionId (descendant)', permissionUuid, 'rbac.permission');

    insert
        into rbac.grant (grantedByTriggerOf, ascendantUuid, descendantUuid, assumed)
        values (rbac.currentTriggerObjectUuid(), roleUuid, permissionUuid, true)
    on conflict do nothing; -- allow granting multiple times
end;
$$;

create or replace procedure rbac.grantPermissionToRole(permissionUuid uuid, roleDesc rbac.RoleDescriptor)
    language plpgsql as $$
begin
    call rbac.grantPermissionToRole(permissionUuid, rbac.findRoleId(roleDesc));
end;
$$;

create or replace procedure rbac.grantRoleToRole(subRoleId uuid, superRoleId uuid, doAssume bool = true)
    language plpgsql as $$
begin
    perform rbac.assertReferenceType('superRoleId (ascendant)', superRoleId, 'rbac.role');
    perform rbac.assertReferenceType('subRoleId (descendant)', subRoleId, 'rbac.role');

    if rbac.isGranted(subRoleId, superRoleId) then
        call rbac.raiseDuplicateRoleGrantException(subRoleId, superRoleId);
    end if;

    insert
        into rbac.grant (grantedByTriggerOf, ascendantuuid, descendantUuid, assumed)
        values (rbac.currentTriggerObjectUuid(), superRoleId, subRoleId, doAssume)
    on conflict do nothing; -- allow granting multiple times
end; $$;


create or replace procedure rbac.grantRoleToRole(subRole rbac.RoleDescriptor, superRole rbac.RoleDescriptor, doAssume bool = true)
    language plpgsql as $$
declare
    superRoleId uuid;
    subRoleId uuid;
begin
    -- TODO.refa: maybe separate method rbac.grantRoleToRoleIfNotNull(...) for NULLABLE references
    if superRole.objectUuid is null or subRole.objectuuid is null then
        return;
    end if;

    superRoleId := rbac.findRoleId(superRole);
    subRoleId := rbac.findRoleId(subRole);

    perform rbac.assertReferenceType('superRoleId (ascendant)', superRoleId, 'rbac.role');
    perform rbac.assertReferenceType('subRoleId (descendant)', subRoleId, 'rbac.role');

    if rbac.isGranted(subRoleId, superRoleId) then
        call rbac.raiseDuplicateRoleGrantException(subRoleId, superRoleId);
    end if;

    insert
        into rbac.grant (grantedByTriggerOf, ascendantuuid, descendantUuid, assumed)
        values (rbac.currentTriggerObjectUuid(), superRoleId, subRoleId, doAssume)
    on conflict do nothing; -- allow granting multiple times
end; $$;

create or replace procedure rbac.revokeRoleFromRole(subRole rbac.RoleDescriptor, superRole rbac.RoleDescriptor)
    language plpgsql as $$
declare
    superRoleId uuid;
    subRoleId uuid;
begin
    superRoleId := rbac.findRoleId(superRole);
    subRoleId := rbac.findRoleId(subRole);

    perform rbac.assertReferenceType('superRoleId (ascendant)', superRoleId, 'rbac.role');
    perform rbac.assertReferenceType('subRoleId (descendant)', subRoleId, 'rbac.role');

    if (rbac.isGranted(superRoleId, subRoleId)) then
        delete from rbac.grant where ascendantUuid = superRoleId and descendantUuid = subRoleId;
    else
        raise exception 'cannot revoke role % (%) from % (%) because it is not granted',
            subRole, subRoleId, superRole, superRoleId;
    end if;
end; $$;

create or replace procedure rbac.revokePermissionFromRole(permissionId UUID, superRole rbac.RoleDescriptor)
    language plpgsql as $$
declare
    superRoleId uuid;
    permissionOp text;
    objectTable text;
    objectUuid uuid;
begin
    superRoleId := rbac.findRoleId(superRole);

    perform rbac.assertReferenceType('superRoleId (ascendant)', superRoleId, 'rbac.role');
    perform rbac.assertReferenceType('permission (descendant)', permissionId, 'rbac.permission');

    if (rbac.isGranted(superRoleId, permissionId)) then
        delete from rbac.grant where ascendantUuid = superRoleId and descendantUuid = permissionId;
    else
        select p.op, o.objectTable, o.uuid
            from rbac.grant g
                     join rbac.permission p on p.uuid=g.descendantUuid
                     join rbac.object o on o.uuid=p.objectUuid
            where g.uuid=permissionId
            into permissionOp, objectTable, objectUuid;

        raise exception 'cannot revoke permission % (% on %#% (%) from % (%)) because it is not granted',
            permissionId, permissionOp, objectTable, objectUuid, permissionId, superRole, superRoleId;
    end if;
end; $$;

-- ============================================================================
--changeset michael.hoennig:rbac-base-QUERY-ACCESSIBLE-OBJECT-UUIDS runOnChange=true endDelimiter:--//
-- ----------------------------------------------------------------------------
/*

 */
create or replace function rbac.queryAccessibleObjectUuidsOfSubjectIds(
    requiredOp rbac.RbacOp,
    forObjectTable varchar,
    subjectIds uuid[],
    maxObjects integer = 8000)
    returns setof uuid
    returns null on null input
    language plpgsql as $$
declare
    foundRows bigint;
begin
    return query
        WITH RECURSIVE grants AS (
            SELECT descendantUuid, ascendantUuid, 1 AS level
                FROM rbac.grant
                WHERE assumed
                  AND ascendantUuid = any(subjectIds)
            UNION ALL
            SELECT g.descendantUuid, g.ascendantUuid, grants.level + 1 AS level
                FROM rbac.grant g
                         INNER JOIN grants ON grants.descendantUuid = g.ascendantUuid
                WHERE g.assumed
        ),
       granted AS (
           SELECT DISTINCT descendantUuid
               FROM grants
       )
        SELECT DISTINCT perm.objectUuid
            FROM granted
                     JOIN rbac.permission perm ON granted.descendantUuid = perm.uuid
                     JOIN rbac.object obj ON obj.uuid = perm.objectUuid
            WHERE (requiredOp = 'SELECT' OR perm.op = requiredOp)
              AND obj.objectTable = forObjectTable
            LIMIT maxObjects+1;

    foundRows = base.lastRowCount();
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
--changeset michael.hoennig:rbac-base-QUERY-GRANTED-PERMISSIONS endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns all permissions accessible to the given subject UUID (subject or role).
 */
create or replace function rbac.queryPermissionsGrantedToSubjectId(subjectId uuid)
    returns setof rbac.permission
    strict
    language sql as $$
with recursive grants as (
    select descendantUuid, ascendantUuid
        from rbac.grant
        where ascendantUuid = subjectId
    union all
    select g.descendantUuid, g.ascendantUuid
        from rbac.grant g
                 inner join grants on grants.descendantUuid = g.ascendantUuid
)
select perm.*
    from rbac.permission perm
    where perm.uuid in (
        select descendantUuid
            from grants
    );
$$;

--//

-- ============================================================================
--changeset michael.hoennig:rbac-base-QUERY-SUBJECTS-WITH-PERMISSION-FOR-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns all subject UUIDs which have any permission for the given object UUID.
 */

create or replace function rbac.queryAllRbacSubjectsWithPermissionsFor(objectId uuid)
    returns setof rbac.subject
    returns null on null input
    language sql as $$
select *
    from rbac.subject
    where uuid in (
        -- @formatter:off
        with recursive grants as (
            select descendantUuid, ascendantUuid
                from rbac.grant
                where descendantUuid = objectId
            union all
            select "grant".descendantUuid, "grant".ascendantUuid
                from rbac.grant "grant"
                inner join grants recur on recur.ascendantUuid = "grant".descendantUuid
        )
        -- @formatter:on
        select ascendantUuid
            from grants);
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-base-PGSQL-ROLES context:!external-db endDelimiter:--//
-- ----------------------------------------------------------------------------

do $$
    begin
        if '${HSADMINNG_POSTGRES_ADMIN_USERNAME}'='admin' then
            create role admin;
            grant all privileges on all tables in schema public to admin;
        end if;

        if '${HSADMINNG_POSTGRES_RESTRICTED_USERNAME}'='restricted' then
            create role restricted;
            grant all privileges on all tables in schema public to restricted;
        end if;
    end $$;
--//
