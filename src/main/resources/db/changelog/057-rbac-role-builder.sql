--liquibase formatted sql

-- ============================================================================
-- PERMISSIONS
--changeset rbac-role-builder-to-uuids:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function toPermissionUuids(forObjectUuid uuid, permitOps RbacOp[])
    returns uuid[]
    language plpgsql
    strict as $$
begin
    return createPermissions(forObjectUuid, permitOps);
end; $$;

create or replace function toRoleUuids(roleDescriptors RbacRoleDescriptor[])
    returns uuid[]
    language plpgsql
    strict as $$
declare
    superRoleDescriptor RbacRoleDescriptor;
    superRoleUuids      uuid[] := array []::uuid[];
begin
    foreach superRoleDescriptor in array roleDescriptors
        loop
            if superRoleDescriptor is not null then
                superRoleUuids := superRoleUuids || getRoleId(superRoleDescriptor, 'fail');
            end if;
        end loop;

    return superRoleUuids;
end; $$;


-- =================================================================
-- CREATE ROLE
--changeset rbac-role-builder-create-role:1 endDelimiter:--//
-- -----------------------------------------------------------------

create or replace function createRoleWithGrants(
    roleDescriptor RbacRoleDescriptor,
    permissions RbacOp[] = array[]::RbacOp[],
    incomingSuperRoles RbacRoleDescriptor[] = array[]::RbacRoleDescriptor[],
    outgoingSubRoles RbacRoleDescriptor[] = array[]::RbacRoleDescriptor[],
    userUuids uuid[] = array[]::uuid[],
    grantedByRole RbacRoleDescriptor = null
)
    returns uuid
    called on null input
    language plpgsql as $$
declare
    roleUuid      uuid;
    superRoleUuid uuid;
    subRoleUuid   uuid;
    userUuid      uuid;
    grantedByRoleUuid uuid;
begin
    roleUuid := createRole(roleDescriptor);

    if cardinality(permissions)  >0 then
        call grantPermissionsToRole(roleUuid, toPermissionUuids(roleDescriptor.objectuuid, permissions));
    end if;

    foreach superRoleUuid in array toRoleUuids(incomingSuperRoles)
        loop
            call grantRoleToRole(roleUuid, superRoleUuid);
        end loop;

    foreach subRoleUuid in array toRoleUuids(outgoingSubRoles)
        loop
            call grantRoleToRole(subRoleUuid, roleUuid);
        end loop;

    if cardinality(userUuids) > 0 then
        if grantedByRole is null then
            raise exception 'to directly assign users to roles, grantingRole has to be given';
        end if;
        grantedByRoleUuid := getRoleId(grantedByRole, 'fail');
        foreach userUuid in array userUuids
            loop
                call grantRoleToUserUnchecked(grantedByRoleUuid, roleUuid, userUuid);
            end loop;
    end if;

    return roleUuid;
end; $$;
--//

