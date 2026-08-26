--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:rbac-current-context-grant-GRANT-ROLE-TO-CURRENT-CONTEXT runOnChange:true endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Grants creator access to the new object to the current subject or, if roles are assumed,
    to these assumed roles; an object created under an assumed role belongs to that role's
    context, not to the real subject.

    A subject context is granted the given (OWNER) role. An assumed-role context instead only
    gets the object's own SELECT/UPDATE/DELETE permissions: permissions are sinks in the grant
    graph, so creator access can never close a privilege-escalating cycle, which a role grant
    could, e.g. once a REPRESENTATIVE relation of a context-created person reaches back to
    the creating context via the anchor-person's OWNER.
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
                -- permissions the context already reaches via the spec-defined grants are skipped,
                -- e.g. for rbac.global#global:ADMIN, which reaches every object anyway
                insert
                    into rbac.grant (grantedByRoleUuid, ascendantUuid, descendantUuid, assumed)
                    select grantedRoleUuid, contextUuid, perm.uuid, true
                        from rbac.permission perm
                        where perm.objectUuid = (select r.objectUuid from rbac.role r where r.uuid = grantedRoleUuid)
                          and perm.op in ('SELECT', 'UPDATE', 'DELETE')
                          and not rbac.isGranted(array[contextUuid], perm.uuid)
                on conflict do nothing;
            else
                raise exception 'cannot grant % to current context reference % of type %',
                    grantedRole, contextUuid, contextRefType;
            end if;
        end loop;
end; $$;
--//
