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
select *
       -- @formatter:off
        from (
            select r.*, o.objectTable,
                   findIdNameByObjectUuid(o.objectTable, o.uuid) as objectIdName
                from rbacrole as r
                join rbacobject as o on o.uuid = r.objectuuid
                where isGranted(currentSubjectIds(), r.uuid)
        ) as unordered
        -- @formatter:on
        order by objectIdName;
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
    select distinct *
        -- @formatter:off
        from (
            select usersInRolesOfCurrentUser.*
                from RbacUser as usersInRolesOfCurrentUser
                 join RbacGrants as g on g.ascendantuuid = usersInRolesOfCurrentUser.uuid
                 join rbacrole_rv as r on r.uuid = g.descendantuuid
            union
            select users.*
                from RbacUser as users
                where cardinality(assumedRoles()) = 0 and  currentUserId() = users.uuid
        ) as unordered
        -- @formatter:on
        order by unordered.name;
grant all privileges on RbacUser_rv to restricted;
--//

-- ============================================================================
--changeset rbac-views-USER-RV-INSERT-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of insert trigger function for RbacUser_rv.
 */
create or replace function insertRbacUser()
    returns trigger
    language plpgsql as $$
declare
    refUuid uuid;
    newUser RbacUser;
begin
    insert
        into RbacReference as r (uuid, type)
        values( new.uuid, 'RbacUser')
        returning r.uuid into refUuid;
    insert
        into RbacUser (uuid, name)
        values (refUuid, new.name)
        returning * into newUser;
    return newUser;
end;
$$;

/*
    Creates an instead of insert trigger for the RbacUser_rv view.
 */
create trigger insertRbacUser_Trigger
    instead of insert
    on RbacUser_rv
    for each row
execute function insertRbacUser();


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
declare
    targetUserId uuid;
    currentUserId uuid;
begin
    -- @formatter:off
    if cardinality(assumedRoles()) > 0 then
        raise exception '[400] grantedPermissions(...) does not support assumed roles';
    end if;

    targetUserId := findRbacUserId(userName);
    currentUserId := currentUserId();

    if hasGlobalRoleGranted(targetUserId) and not hasGlobalRoleGranted(currentUserId) then
        raise exception '[403] permissions of user "%" are not accessible to user "%"', userName, currentUser();
    end if;

    return query select
        xp.roleUuid,
        (xp.roleObjectTable || '#' || xp.roleObjectIdName || '.' || xp.roleType) as roleName,
        xp.permissionUuid, xp.op, xp.permissionObjectTable, xp.permissionObjectIdName, xp.permissionObjectUuid
        from (select
                  r.uuid as roleUuid, r.roletype, ro.objectTable as roleObjectTable,
                  findIdNameByObjectUuid(ro.objectTable, ro.uuid) as roleObjectIdName,
                  p.uuid as permissionUuid, p.op, po.objecttable as permissionObjectTable,
                  findIdNameByObjectUuid(po.objectTable, po.uuid) as permissionObjectIdName,
                  po.uuid as permissionObjectUuid
              from queryPermissionsGrantedToSubjectId( targetUserId) as p
              join rbacgrants as g on g.descendantUuid = p.uuid
              join rbacobject as po on po.uuid = p.objectUuid
              join rbacrole_rv as r on r.uuid = g.ascendantUuid
              join rbacobject as ro on ro.uuid = r.objectUuid
              where isGranted(targetUserId, r.uuid)
             ) xp;
    -- @formatter:on
end; $$;
