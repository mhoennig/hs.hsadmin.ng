--liquibase formatted sql


-- =================================================================
-- CREATE ROLE
--changeset rbac-role-builder-create-role:1 endDelimiter:--//
-- -----------------------------------------------------------------

-- TODO: rename to defineRoleWithGrants because it does not complain if the role already exists
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
    roleUuid                uuid;
    permission              RbacOp;
    permissionUuid          uuid;
    subRoleDesc             RbacRoleDescriptor;
    superRoleDesc           RbacRoleDescriptor;
    subRoleUuid             uuid;
    superRoleUuid           uuid;
    userUuid                uuid;
    userGrantsByRoleUuid    uuid;
begin
    roleUuid := coalesce(findRoleId(roleDescriptor), createRole(roleDescriptor));

    foreach permission in array permissions
        loop
            permissionUuid := createPermission(roleDescriptor.objectuuid, permission);
            call grantPermissionToRole(permissionUuid, roleUuid);
        end loop;

    foreach superRoleDesc in array array_remove(incomingSuperRoles, null)
        loop
            superRoleUuid := getRoleId(superRoleDesc);
            call grantRoleToRole(roleUuid, superRoleUuid, superRoleDesc.assumed);
        end loop;

    foreach subRoleDesc in array array_remove(outgoingSubRoles, null)
        loop
            subRoleUuid := getRoleId(subRoleDesc);
            call grantRoleToRole(subRoleUuid, roleUuid, subRoleDesc.assumed);
        end loop;

    if cardinality(userUuids) > 0 then
        -- direct grants to users need a grantedByRole which can revoke the grant
        if grantedByRole is null then
            userGrantsByRoleUuid := roleUuid; -- TODO.spec: or do we want to require an explicit userGrantsByRoleUuid?
        else
            userGrantsByRoleUuid := getRoleId(grantedByRole);
        end if;
        foreach userUuid in array userUuids
            loop
                call grantRoleToUserUnchecked(userGrantsByRoleUuid, roleUuid, userUuid);
            end loop;
    end if;

    return roleUuid;
end; $$;
--//

