--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:rbac-views-ROLE-ENHANCED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the role table with additional columns
    for easier human readability.
 */
drop view if exists rbac.role_ev;
create or replace view rbac.role_ev as
select (objectTable || '#' || objectIdName || ':' || roleType) as roleIdName, *
       -- @formatter:off
    from (
             select r.*,
                    o.objectTable, rbac.findIdNameByObjectUuid(o.objectTable, o.uuid) as objectIdName
                 from rbac.role as r
                          join rbac.object as o on o.uuid = r.objectuuid
         ) as unordered
         -- @formatter:on
    order by roleIdName;
--//

-- ============================================================================
--changeset michael.hoennig:rbac-views-ROLE-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the role table with row-level limitation
    based on the grants of the current user or assumed roles.
 */
drop view if exists rbac.role_rv;
create or replace view rbac.role_rv as
select *
       -- @formatter:off
        from (
            select r.*, o.objectTable,
                   rbac.findIdNameByObjectUuid(o.objectTable, o.uuid) as objectIdName
                from rbac.role as r
                join rbac.object as o on o.uuid = r.objectuuid
                where rbac.isGranted(rbac.currentSubjectOrAssumedRolesUuids(), r.uuid)
        ) as unordered
        -- @formatter:on
        order by objectTable || '#' || objectIdName || ':' || roleType;
grant all privileges on rbac.role_rv to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
--//


-- ============================================================================
--changeset michael.hoennig:rbac-views-GRANT-ENHANCED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the grants table with additional columns
    for easier human readability.
 */
drop view if exists rbac.grant_ev;
create or replace view rbac.grant_ev as
    -- @formatter:off
    select x.grantUuid as uuid,
           x.grantedByTriggerOf as grantedByTriggerOf,
           go.objectTable || '#' || rbac.findIdNameByObjectUuid(go.objectTable, go.uuid) || ':' || r.roletype as grantedByRoleIdName,
           x.ascendingIdName as ascendantIdName,
           x.descendingIdName as descendantIdName,
           x.grantedByRoleUuid,
           x.ascendantUuid as ascendantUuid,
           x.descendantUuid as descendantUuid,
           x.op as permOp, x.optablename as permOpTableName,
           x.assumed
        from (
             select g.uuid as grantUuid,
                    g.grantedbytriggerof as grantedbytriggerof,
                    g.grantedbyroleuuid, g.ascendantuuid, g.descendantuuid, g.assumed,

                    coalesce(
                        'user:' || au.name,
                        'role:' || aro.objectTable || '#' || rbac.findIdNameByObjectUuid(aro.objectTable, aro.uuid) || ':' || ar.roletype
                        ) as ascendingIdName,
                    aro.objectTable, aro.uuid,
                    ( case
                            when dro is not null
                                then ('role:' || dro.objectTable || '#' || rbac.findIdNameByObjectUuid(dro.objectTable, dro.uuid) || ':' || dr.roletype)
                            when dp.op = 'INSERT'
                                then 'perm:' || dpo.objecttable || '#' || rbac.findIdNameByObjectUuid(dpo.objectTable, dpo.uuid) || ':'  || dp.op || '>' || dp.opTableName
                            else 'perm:' || dpo.objecttable || '#' || rbac.findIdNameByObjectUuid(dpo.objectTable, dpo.uuid) || ':' || dp.op
                        end
                    ) as descendingIdName,
                    dro.objectTable, dro.uuid,
                    dp.op, dp.optablename
                 from rbac.grant as g

                left outer join rbac.role as ar on ar.uuid = g.ascendantUuid
                    left outer join rbac.object as aro on aro.uuid = ar.objectuuid
                left outer join rbac.subject as au on au.uuid = g.ascendantUuid

                left outer join rbac.role as dr on dr.uuid = g.descendantUuid
                    left outer join rbac.object as dro on dro.uuid = dr.objectuuid
                left outer join rbac.permission dp on dp.uuid = g.descendantUuid
                    left outer join rbac.object as dpo on dpo.uuid = dp.objectUuid
         ) as x
         left outer join rbac.role as r on r.uuid = grantedByRoleUuid
         left outer join rbac.subject u on u.uuid = x.ascendantuuid
         left outer join rbac.object go on go.uuid = r.objectuuid

        order by x.ascendingIdName, x.descendingIdName;
    -- @formatter:on
--//


-- ============================================================================
--changeset michael.hoennig:rbac-views-GRANT-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the grants table with row-level limitation
    based on the direct grants of the current user.
 */
create or replace view rbac.grant_rv as
-- @formatter:off
select o.objectTable || '#' || rbac.findIdNameByObjectUuid(o.objectTable, o.uuid) || ':' || r.roletype as grantedByRoleIdName,
       g.objectTable || '#' || g.objectIdName || ':' || g.roletype as grantedRoleIdName, g.userName, g.assumed,
       g.grantedByRoleUuid, g.descendantUuid as grantedRoleUuid, g.ascendantUuid as subjectUuid,
       g.objectTable, g.objectUuid, g.objectIdName, g.roleType as grantedRoleType
    from (
             select g.grantedbyroleuuid, g.ascendantuuid, g.descendantuuid, g.assumed,
                    u.name as userName, o.objecttable, r.objectuuid, r.roletype,
                    rbac.findIdNameByObjectUuid(o.objectTable, o.uuid) as objectIdName
                 from rbac.grant as g
                          join rbac.role as r on r.uuid = g.descendantUuid
                          join rbac.object o on o.uuid = r.objectuuid
                          left outer join rbac.subject u on u.uuid = g.ascendantuuid
                 where rbac.isGranted(rbac.currentSubjectOrAssumedRolesUuids(), r.uuid)
         ) as g
             join rbac.role as r on r.uuid = grantedByRoleUuid
             join rbac.object as o on o.uuid = r.objectUuid
    order by grantedRoleIdName;
-- @formatter:on
grant all privileges on rbac.role_rv to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
--//


-- ============================================================================
--changeset michael.hoennig:rbac-views-GRANTS-RV-INSERT-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of insert trigger function for rbac.grant_rv.
 */
create or replace function rbac.insert_grant_tf()
    returns trigger
    language plpgsql as $$
declare
    newGrant rbac.grant_rv;
begin
    call rbac.grantRoleToSubject(rbac.assumedRoleUuid(), new.grantedRoleUuid, new.subjectUuid, new.assumed);
    select grv.*
        from rbac.grant_rv grv
        where grv.subjectUuid=new.subjectUuid and grv.grantedRoleUuid=new.grantedRoleUuid
        into newGrant;
    return newGrant;
end; $$;

/*
    Creates an instead of insert trigger for the rbac.grant_rv view.
 */
create trigger insert_grant_tg
    instead of insert
    on rbac.grant_rv
    for each row
execute function rbac.insert_grant_tf();
--/


-- ============================================================================
--changeset michael.hoennig:rbac-views-GRANTS-RV-DELETE-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of delete trigger function for rbac.grant_rv.

    Checks if the current subject or assumed role have the permission to revoke the grant.
 */
create or replace function rbac.delete_grant_tf()
    returns trigger
    language plpgsql as $$
begin
    call rbac.revokeRoleFromSubject(old.grantedByRoleUuid, old.grantedRoleUuid, old.subjectUuid);
    return old;
end; $$;

/*
    Creates an instead of delete trigger for the rbac.grant_rv view.
 */
create trigger delete_grant_tg
    instead of delete
    on rbac.grant_rv
    for each row
execute function rbac.delete_grant_tf();
--/


-- ============================================================================
--changeset michael.hoennig:rbac-views-USER-ENHANCED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the users table with additional columns
    for easier human readability.
 */
drop view if exists rbac.subject_ev;
create or replace view rbac.subject_ev as
select distinct *
                -- @formatter:off
    from (
             select usersInRolesOfcurrentSubject.*
                 from rbac.subject as usersInRolesOfcurrentSubject
                          join rbac.grant as g on g.ascendantuuid = usersInRolesOfcurrentSubject.uuid
                          join rbac.role_ev as r on r.uuid = g.descendantuuid
             union
             select users.*
                 from rbac.subject as users
         ) as unordered
         -- @formatter:on
    order by unordered.name;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-views-USER-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the users table with row-level limitation
    based on the grants of the current user or assumed roles.
 */
drop view if exists rbac.subject_rv;
create or replace view rbac.subject_rv as
    select distinct *
        -- @formatter:off
        from (
            select usersInRolesOfcurrentSubject.*
                from rbac.subject as usersInRolesOfcurrentSubject
                 join rbac.grant as g on g.ascendantuuid = usersInRolesOfcurrentSubject.uuid
                 join rbac.role_rv as r on r.uuid = g.descendantuuid
            union
            select users.*
                from rbac.subject as users
                where cardinality(base.assumedRoles()) = 0 and
                        (rbac.currentSubjectUuid() = users.uuid or rbac.hasGlobalRoleGranted(rbac.currentSubjectUuid()))

        ) as unordered
        -- @formatter:on
        order by unordered.name;
grant all privileges on rbac.subject_rv to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
--//

-- ============================================================================
--changeset michael.hoennig:rbac-views-USER-RV-INSERT-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of insert trigger function for rbac.subject_rv.
 */
create or replace function rbac.insert_subject_tf()
    returns trigger
    language plpgsql as $$
declare
    refUuid uuid;
    newUser rbac.subject;
begin
    insert
        into  rbac.reference as r (uuid, type)
        values( new.uuid, 'rbac.subject')
        returning r.uuid into refUuid;
    insert
        into rbac.subject (uuid, name)
        values (refUuid, new.name)
        returning * into newUser;
    return newUser;
end;
$$;

/*
    Creates an instead of insert trigger for the rbac.subject_rv view.
 */
create trigger insert_subject_tg
    instead of insert
    on rbac.subject_rv
    for each row
execute function rbac.insert_subject_tf();
--//

-- ============================================================================
--changeset michael.hoennig:rbac-views-USER-RV-DELETE-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of delete trigger function for rbac.subject_rv.

    Checks if the current subject (user / assumed role) has the permission to delete the user.
 */
create or replace function rbac.delete_subject_tf()
    returns trigger
    language plpgsql as $$
begin
    if rbac.currentSubjectUuid() = old.uuid or rbac.hasGlobalRoleGranted(rbac.currentSubjectUuid()) then
        delete from rbac.subject where uuid = old.uuid;
        return old;
    end if;
    raise exception '[403] User % not allowed to delete user uuid %', base.currentSubject(), old.uuid;
end; $$;

/*
    Creates an instead of delete trigger for the rbac.subject_rv view.
 */
create trigger delete_subject_tg
    instead of delete
    on rbac.subject_rv
    for each row
execute function rbac.delete_subject_tf();
--/

-- ============================================================================
--changeset michael.hoennig:rbac-views-OWN-GRANTED-PERMISSIONS-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to all permissions granted to the current user or
    based on the grants of the current user or assumed roles.
 */
-- @formatter:off
drop view if exists rbac.own_granted_permissions_rv;
create or replace view rbac.own_granted_permissions_rv as
select r.uuid as roleuuid, p.uuid as permissionUuid,
       (r.objecttable || ':' || r.objectidname || ':' ||  r.roletype) as roleName, p.op,
       o.objecttable, r.objectidname, o.uuid as objectuuid
    from rbac.role_rv r
    join rbac.grant g on g.ascendantuuid = r.uuid
    join rbac.permission p on p.uuid = g.descendantuuid
    join rbac.object o on o.uuid = p.objectuuid;
grant all privileges on rbac.own_granted_permissions_rv to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
-- @formatter:om

-- ============================================================================
--changeset michael.hoennig:rbac-views-GRANTED-PERMISSIONS endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns all permissions granted to the given user,
    which are also visible to the current user or assumed roles.
*/
create or replace function rbac.grantedPermissionsRaw(targetSubjectUuid uuid)
    returns table(roleUuid uuid, roleName text, permissionUuid uuid, op rbac.RbacOp, opTableName varchar(60), objectTable varchar(60), objectIdName varchar, objectUuid uuid)
    returns null on null input
    language plpgsql as $$
declare
    currentSubjectUuid uuid;
begin
    -- @formatter:off
    currentSubjectUuid := rbac.currentSubjectUuid();

    if rbac.hasGlobalRoleGranted(targetSubjectUuid) and not rbac.hasGlobalRoleGranted(currentSubjectUuid) then
        raise exception '[403] permissions of user "%" are not accessible to user "%"', targetSubjectUuid, base.currentSubject();
    end if;

    return query select
        xp.roleUuid,
        (xp.roleObjectTable || '#' || xp.roleObjectIdName || ':' || xp.roleType) as roleName,
        xp.permissionUuid, xp.op, xp.opTableName,
        xp.permissionObjectTable, xp.permissionObjectIdName, xp.permissionObjectUuid
        from (select
                  r.uuid as roleUuid, r.roletype, ro.objectTable as roleObjectTable,
                  rbac.findIdNameByObjectUuid(ro.objectTable, ro.uuid) as roleObjectIdName,
                  p.uuid as permissionUuid, p.op, p.opTableName,
                  po.objecttable as permissionObjectTable,
                  rbac.findIdNameByObjectUuid(po.objectTable, po.uuid) as permissionObjectIdName,
                  po.uuid as permissionObjectUuid
              from rbac.queryPermissionsGrantedToSubjectId( targetSubjectUuid) as p
              join rbac.grant as g on g.descendantUuid = p.uuid
              join rbac.object as po on po.uuid = p.objectUuid
              join rbac.role_rv as r on r.uuid = g.ascendantUuid
              join rbac.object as ro on ro.uuid = r.objectUuid
              where rbac.isGranted(targetSubjectUuid, r.uuid)
             ) xp;
    -- @formatter:on
end; $$;

create or replace function rbac.grantedPermissions(targetSubjectUuid uuid)
    returns table(roleUuid uuid, roleName text, permissionUuid uuid, op rbac.RbacOp, opTableName varchar(60), objectTable varchar(60), objectIdName varchar, objectUuid uuid)
    returns null on null input
    language sql as $$
    select * from rbac.grantedPermissionsRaw(targetSubjectUuid)
    union all
    select roleUuid, roleName, permissionUuid, 'SELECT'::rbac.RbacOp, opTableName, objectTable, objectIdName, objectUuid
        from rbac.grantedPermissionsRaw(targetSubjectUuid)
        where op <> 'SELECT'::rbac.RbacOp;
$$;
--//
