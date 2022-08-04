--liquibase formatted sql

-- ============================================================================
--changeset rbac-views-ROLE-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the role table with row-level limitation
    based on the grants of the current user or assumed roles.
 */
drop view if exists rbacrole_rv;
create or replace view rbacrole_rv as
select DISTINCT r.*, o.objectTable,
       findIdNameByObjectUuid(o.objectTable, o.uuid) as objectIdName
    from rbacrole as r
    join rbacobject as o on o.uuid=r.objectuuid
    where isGranted(currentSubjectIds(), r.uuid);
grant all privileges on rbacrole_rv to restricted;
--//


-- ============================================================================
--changeset rbac-views-USER-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the users table with row-level limitation
    based on the grants of the current user or assumed roles.
 */
drop view if exists RbacUser_rv;
create or replace view RbacUser_rv as
select u.*
    from RbacUser as u
             join RbacGrants as g on g.ascendantuuid = u.uuid
             join rbacrole_rv as r on r.uuid = g.descendantuuid;
grant all privileges on RbacUser_rv to restricted;
--//


-- ============================================================================
--changeset rbac-views-OWN-GRANTED-PERMISSIONS-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to all permissions granted to the current user or
    based on the grants of the current user or assumed roles.
 */
-- @formatter:off
drop view if exists RbacOwnGrantedPermissions_rv;
create or replace view RbacOwnGrantedPermissions_rv as
select r.uuid as roleuuid, p.uuid as permissionUuid,
       (r.objecttable || '#' || r.objectidname || '.' ||  r.roletype) as roleName, p.op,
       o.objecttable, r.objectidname, o.uuid as objectuuid
    from rbacrole_rv r
    join rbacgrants g on g.ascendantuuid = r.uuid
    join rbacpermission p on p.uuid = g.descendantuuid
    join rbacobject o on o.uuid = p.objectuuid;
grant all privileges on RbacOwnGrantedPermissions_rv to restricted;
-- @formatter:om

-- ============================================================================
--changeset rbac-views-GRANTED-PERMISSIONS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Returns all permissions granted to the given user,
    which are also visible to the current user or assumed roles.
 */
create or replace function grantedPermissions(userName varchar)
    returns table(roleUuid uuid, roleName text, permissionUuid uuid, op RbacOp, objectTable varchar, objectIdName varchar, objectUuid uuid)
    returns null on null input
    language plpgsql as $$
begin
    -- @formatter:off
    if cardinality(assumedRoles()) > 0 then
        raise exception 'grantedPermissions(...) does not support assumed roles';
    end if;

    return query select
        xp.roleUuid,
        (xp.objecttable || '#' || xp.objectidname || '.' || xp.roletype) as roleName,
        xp.permissionUuid, xp.op, xp.objecttable, xp.objectIdName, xp.objectuuid
        from (select
                  r.uuid as roleUuid, r.roletype,
                  p.uuid as permissionUuid, p.op, o.objecttable,
                  findIdNameByObjectUuid(o.objectTable, o.uuid) as objectIdName,
                  o.uuid as objectuuid
                  from queryPermissionsGrantedToSubjectId( findRbacUserId(userName)) p
                           join rbacgrants g on g.descendantuuid = p.uuid
                           join rbacobject o on o.uuid = p.objectuuid
                           join rbacrole r on r.uuid = g.ascendantuuid
                  where isGranted(currentUserId(), r.uuid)
             ) xp;
    -- @formatter:on
end; $$;
