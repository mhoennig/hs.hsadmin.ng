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
        order by objectTable || '#' || objectIdName || '.' || roleType;
grant all privileges on rbacrole_rv to restricted;
--//


-- ============================================================================
--changeset rbac-views-GRANT-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the grants table with row-level limitation
    based on the direct grants of the current user.
 */
drop view if exists rbacgrants_rv;
create or replace view rbacgrants_rv as
    -- @formatter:off
    select o.objectTable || '#' || findIdNameByObjectUuid(o.objectTable, o.uuid) || '.' || r.roletype as grantedByRoleIdName,
           g.objectTable || '#' || g.objectIdName || '.' || g.roletype as grantedRoleIdName, g.userName, g.assumed,
           g.grantedByRoleUuid, g.descendantUuid as grantedRoleUuid, g.ascendantUuid as userUuid,
           g.objectTable, g.objectUuid, g.objectIdName, g.roleType as grantedRoleType
        from (
             select g.grantedbyroleuuid, g.ascendantuuid, g.descendantuuid, g.assumed,
                    u.name as userName, o.objecttable, r.objectuuid, r.roletype,
                    findIdNameByObjectUuid(o.objectTable, o.uuid) as objectIdName
                 from rbacgrants as g
                 join rbacrole as r on r.uuid = g.descendantUuid
                 join rbacobject o on o.uuid = r.objectuuid
                 join rbacuser u on u.uuid = g.ascendantuuid
                 where isGranted(currentSubjectIds(), r.uuid)
         ) as g
        join RbacRole as r on r.uuid = grantedByRoleUuid
        join RbacObject as o on o.uuid = r.objectUuid
    order by grantedRoleIdName;
    -- @formatter:on
grant all privileges on rbacrole_rv to restricted;
--//


-- ============================================================================
--changeset rbac-views-GRANTS-RV-INSERT-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of insert trigger function for RbacGrants_RV.
 */
create or replace function insertRbacGrant()
    returns trigger
    language plpgsql as $$
declare
    newGrant RbacGrants_RV;
begin
    call grantRoleToUser(assumedRoleUuid(), new.grantedRoleUuid, new.userUuid, new.assumed);
    select grv.*
        from RbacGrants_RV grv
        where grv.userUuid=new.userUuid and grv.grantedRoleUuid=new.grantedRoleUuid
        into newGrant;
    return newGrant;
end; $$;

/*
    Creates an instead of insert trigger for the RbacGrants_rv view.
 */
create trigger insertRbacGrant_Trigger
    instead of insert
    on RbacGrants_rv
    for each row
execute function insertRbacGrant();
--/


-- ============================================================================
--changeset rbac-views-GRANTS-RV-DELETE-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of delete trigger function for RbacGrants_RV.
 */
create or replace function deleteRbacGrant()
    returns trigger
    language plpgsql as $$
begin
    call revokeRoleFromUser(old.grantedByRoleUuid, old.grantedRoleUuid, old.userUuid);
    return old;
end; $$;

/*
    Creates an instead of delete trigger for the RbacGrants_rv view.
 */
create trigger deleteRbacGrant_Trigger
    instead of delete
    on RbacGrants_rv
    for each row
execute function deleteRbacGrant();
--/


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
create or replace function grantedPermissions(targetUserUuid uuid)
    returns table(roleUuid uuid, roleName text, permissionUuid uuid, op RbacOp, objectTable varchar, objectIdName varchar, objectUuid uuid)
    returns null on null input
    language plpgsql as $$
declare
    currentUserId uuid;
begin
    -- @formatter:off
    currentUserId := currentUserId();

    if hasGlobalRoleGranted(targetUserUuid) and not hasGlobalRoleGranted(currentUserId) then
        raise exception '[403] permissions of user "%" are not accessible to user "%"', targetUserUuid, currentUser();
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
              from queryPermissionsGrantedToSubjectId( targetUserUuid) as p
              join rbacgrants as g on g.descendantUuid = p.uuid
              join rbacobject as po on po.uuid = p.objectUuid
              join rbacrole_rv as r on r.uuid = g.ascendantUuid
              join rbacobject as ro on ro.uuid = r.objectUuid
              where isGranted(targetUserUuid, r.uuid)
             ) xp;
    -- @formatter:on
end; $$;
--//
