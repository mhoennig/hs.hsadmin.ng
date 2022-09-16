--liquibase formatted sql


-- ============================================================================
--changeset rbac-generators-RELATED-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure generateRelatedRbacObject(targetTable varchar)
    language plpgsql as $$
declare
    createInsertTriggerSQL text;
    createDeleteTriggerSQL text;
begin
    createInsertTriggerSQL = format($sql$
        create trigger createRbacObjectFor_%s_Trigger
            before insert
            on %s
            for each row
                execute procedure insertRelatedRbacObject();
        $sql$, targetTable, targetTable);
    execute createInsertTriggerSQL;

    createDeleteTriggerSQL = format($sql$
        create trigger deleteRbacRulesFor_%s_Trigger
            before delete
            on %s
            for each row
                execute procedure deleteRelatedRbacObject();
        $sql$, targetTable, targetTable);
    execute createDeleteTriggerSQL;
end; $$;
--//


-- ============================================================================
--changeset rbac-generators-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure generateRbacRoleDescriptors(prefix text, targetTable text)
    language plpgsql as $$
declare
    sql text;
begin
    sql = format($sql$
        create or replace function %1$sOwner(entity %2$s)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'owner');
        end; $f$;

        create or replace function %1$sAdmin(entity %2$s)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'admin');
        end; $f$;

        create or replace function %1$sTenant(entity %2$s)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'tenant');
        end; $f$;

        $sql$, prefix, targetTable);
    execute sql;
end; $$;
--//
