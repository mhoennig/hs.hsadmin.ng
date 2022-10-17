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
            after delete
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

        create or replace function %1$sAgent(entity %2$s)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'agent');
        end; $f$;

        create or replace function %1$sTenant(entity %2$s)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'tenant');
        end; $f$;

        create or replace function %1$sGuest(entity %2$s)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'guest');
        end; $f$;

        $sql$, prefix, targetTable);
    execute sql;
end; $$;
--//


-- ============================================================================
--changeset rbac-generators-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure generateRbacIdentityView(targetTable text, idNameExpression text)
    language plpgsql as $$
declare
    sql text;
begin
    -- create a view to the target main table which maps an idName to the objectUuid
    sql = format($sql$
            create or replace view %1$s_iv as
            select target.uuid, cleanIdentifier(%2$s) as idName
                from %1$s as target;
            grant all privileges on %1$s_iv to restricted;
        $sql$, targetTable, idNameExpression);
    execute sql;

    -- creates a function which maps an idName to the objectUuid
    sql = format($sql$
        create or replace function %1$sUuidByIdName(givenIdName varchar)
            returns uuid
            language sql
            strict as $f$
        select uuid from %1$s_iv iv where iv.idName = givenIdName;
        $f$;
        $sql$, targetTable);
    execute sql;

    -- creates a function which maps an objectUuid to the related idName
    sql = format($sql$
        create or replace function %1$sIdNameByUuid(givenUuid uuid)
            returns varchar
            language sql
            strict as $f$
        select idName from %1$s_iv iv where iv.uuid = givenUuid;
        $f$;
    $sql$, targetTable);
    execute sql;
end; $$;
--//


-- ============================================================================
--changeset rbac-generators-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure generateRbacRestrictedView(targetTable text, orderBy text, columnUpdates text)
    language plpgsql as $$
declare
    sql text;
begin
    /*
        Creates a restricted view based on the 'view' permission of the current subject.
    */
    sql := format($sql$
        set session session authorization default;
        create view %1$s_rv as
            with accessibleObjects as (
                select queryAccessibleObjectUuidsOfSubjectIds('view', '%1$s', currentSubjectsUuids())
            )
            select target.*
                from %1$s as target
                where target.uuid in (select * from accessibleObjects)
                order by %2$s;
            grant all privileges on %1$s_rv to restricted;
        $sql$, targetTable, orderBy);
    execute sql;

    /**
        Instead of insert trigger function for the restricted view.
     */
    sql := format($sql$
        create or replace function %1$sInsert()
            returns trigger
            language plpgsql as $f$
        declare
            newTargetRow %1$s;
        begin
            insert
                into %1$s
                values (new.*)
                returning * into newTargetRow;
            return newTargetRow;
        end; $f$;
        $sql$, targetTable);
    execute sql;

    /*
        Creates an instead of insert trigger for the restricted view.
     */
    sql := format($sql$
        create trigger %1$sInsert_tg
            instead of insert
            on %1$s_rv
            for each row
        execute function %1$sInsert();
    $sql$, targetTable);
    execute sql;

    /**
        Instead of delete trigger function for the restricted view.
     */
    sql := format($sql$
        create or replace function %1$sDelete()
            returns trigger
            language plpgsql as $f$
        begin
            if old.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('delete', '%1$s', currentSubjectsUuids())) then
                delete from %1$s p where p.uuid = old.uuid;
                return old;
            end if;
            raise exception '[403] Subject %% is not allowed to delete %1$s uuid %%', currentSubjectsUuids(), old.uuid;
        end; $f$;
    $sql$, targetTable);
    execute sql;

    /*
        Creates an instead of delete trigger for the restricted view.
     */
    sql := format($sql$
        create trigger %1$sDelete_tg
            instead of delete
            on %1$s_rv
            for each row
        execute function %1$sDelete();
    $sql$, targetTable);
    execute sql;

    /**
        Instead of update trigger function for the restricted view
        based on the 'edit' permission of the current subject.
     */
    sql := format($sql$
        create or replace function %1$sUpdate()
            returns trigger
            language plpgsql as $f$
        begin
            if old.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('edit', '%1$s', currentSubjectsUuids())) then
                update %1$s
                    set %2$s
                    where uuid = old.uuid;
                return old;
            end if;
            raise exception '[403] Subject %% is not allowed to update %1$s uuid %%', currentSubjectsUuids(), old.uuid;
        end; $f$;
    $sql$, targetTable, columnUpdates);
    execute sql;

    /*
        Creates an instead of delete trigger for the restricted view.
     */
    sql = format($sql$
        create trigger %1$sUpdate_tg
            instead of update
            on %1$s_rv
            for each row
        execute function %1$sUpdate();
    $sql$, targetTable);
    execute sql;
end; $$;
--//
