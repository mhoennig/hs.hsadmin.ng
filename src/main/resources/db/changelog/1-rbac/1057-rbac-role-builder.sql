--liquibase formatted sql


-- =================================================================
-- CREATE ROLE
--changeset michael.hoennig:rbac-role-builder-define-role endDelimiter:--//
-- -----------------------------------------------------------------

create or replace function rbac.defineRoleWithGrants(
    roleDescriptor rbac.RoleDescriptor,
    permissions rbac.RbacOp[] = array[]::rbac.RbacOp[],
    incomingSuperRoles rbac.RoleDescriptor[] = array[]::rbac.RoleDescriptor[],
    outgoingSubRoles rbac.RoleDescriptor[] = array[]::rbac.RoleDescriptor[],
    subjectUuids uuid[] = array[]::uuid[],
    grantedByRole rbac.RoleDescriptor = null
)
    returns uuid
    called on null input
    language plpgsql as $$
declare
    roleUuid                uuid;
    permission              rbac.RbacOp;
    permissionUuid          uuid;
    subRoleDesc             rbac.RoleDescriptor;
    superRoleDesc           rbac.RoleDescriptor;
    subRoleUuid             uuid;
    superRoleUuid           uuid;
    subjectUuid             uuid;
    userGrantsByRoleUuid    uuid;
begin
    roleUuid := coalesce(rbac.findRoleId(roleDescriptor), rbac.createRole(roleDescriptor));

    foreach permission in array permissions
        loop
            permissionUuid := rbac.createPermission(roleDescriptor.objectuuid, permission);
            call rbac.grantPermissionToRole(permissionUuid, roleUuid);
        end loop;

    foreach superRoleDesc in array array_remove(incomingSuperRoles, null)
        loop
            superRoleUuid := rbac.getRoleId(superRoleDesc);
            call rbac.grantRoleToRole(roleUuid, superRoleUuid, superRoleDesc.assumed);
        end loop;

    foreach subRoleDesc in array array_remove(outgoingSubRoles, null)
        loop
            subRoleUuid := rbac.getRoleId(subRoleDesc);
            call rbac.grantRoleToRole(subRoleUuid, roleUuid, subRoleDesc.assumed);
        end loop;

    if cardinality(subjectUuids) > 0 then
        -- direct grants to users need a grantedByRole which can revoke the grant
        if grantedByRole is null then
            userGrantsByRoleUuid := roleUuid; -- TODO.impl: or do we want to require an explicit userGrantsByRoleUuid?
        else
            userGrantsByRoleUuid := rbac.getRoleId(grantedByRole);
        end if;
        foreach subjectUuid in array subjectUuids
            loop
                call rbac.grantRoleToSubjectUnchecked(userGrantsByRoleUuid, roleUuid, subjectUuid);
            end loop;
    end if;

    return roleUuid;
end; $$;
--//

