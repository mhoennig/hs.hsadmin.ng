--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:rbac-current-context-grant-GRANT-ROLE-TO-CURRENT-CONTEXT runOnChange:true endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Grants the given role to the current subject or, if roles are assumed, to these assumed roles.

    An object created under an assumed role belongs to that role's context, not to the real
    subject; granting ownership to the subject would make the new object invisible in the very
    context it was created in.
 */
create or replace procedure rbac.grantRoleToCurrentContext(grantedRole rbac.RoleDescriptor)
    language plpgsql as $$
declare
    grantedRoleUuid uuid;
    contextUuid     uuid;
    contextRefType  rbac.ReferenceType;
begin
    grantedRoleUuid := rbac.getRoleId(grantedRole);
    foreach contextUuid in array rbac.currentSubjectOrAssumedRolesUuids()
        loop
            select type from rbac.reference where uuid = contextUuid into contextRefType;
            if contextRefType = 'rbac.subject' then
                call rbac.grantRoleToSubjectUnchecked(grantedRoleUuid, grantedRoleUuid, contextUuid);
            elsif contextRefType = 'rbac.role' then
                -- skip if the assumed role already reaches the granted role via the spec-defined grants
                if not rbac.isGranted(contextUuid, grantedRoleUuid) then
                    insert
                        into rbac.grant (grantedByRoleUuid, ascendantUuid, descendantUuid, assumed)
                        values (grantedRoleUuid, contextUuid, grantedRoleUuid, true)
                    on conflict do nothing;
                end if;
            else
                raise exception 'cannot grant % to current context reference % of type %',
                    grantedRole, contextUuid, contextRefType;
            end if;
        end loop;
end; $$;
--//
