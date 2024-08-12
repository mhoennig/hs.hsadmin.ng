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
            before insert on %s
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

create procedure generateRbacRoleDescriptors(prefix text, targetTable text)
    language plpgsql as $$
declare
    sql text;
begin
    sql = format($sql$
        create or replace function %1$sOwner(entity %2$s, assumed boolean = true)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'OWNER', assumed);
        end; $f$;

        create or replace function %1$sAdmin(entity %2$s, assumed boolean = true)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'ADMIN', assumed);
        end; $f$;

        create or replace function %1$sAgent(entity %2$s, assumed boolean = true)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'AGENT', assumed);
        end; $f$;

        create or replace function %1$sTenant(entity %2$s, assumed boolean = true)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'TENANT', assumed);
        end; $f$;

        -- TODO: remove guest role
        create or replace function %1$sGuest(entity %2$s, assumed boolean = true)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'GUEST', assumed);
        end; $f$;

        create or replace function %1$sReferrer(entity %2$s)
            returns RbacRoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return roleDescriptor('%2$s', entity.uuid, 'REFERRER');
        end; $f$;

        $sql$, prefix, targetTable);
    execute sql;
end; $$;
--//


-- ============================================================================
--changeset rbac-generators-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure generateRbacIdentityViewFromQuery(targetTable text, sqlQuery text)
    language plpgsql as $$
declare
    sql text;
begin
    targettable := lower(targettable);

    -- create a view to the target main table which maps an idName to the objectUuid
    sql = format($sql$
            create or replace view %1$s_iv as %2$s;
            grant all privileges on %1$s_iv to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
        $sql$, targetTable, sqlQuery);
    execute sql;

    -- creates a function which maps an idName to the objectUuid
    sql = format($sql$
        create or replace function %1$sUuidByIdName(givenIdName varchar)
            returns uuid
            language plpgsql as $f$
        declare
            singleMatch uuid;
        begin
            select uuid into strict singleMatch from %1$s_iv iv where iv.idName = givenIdName;
            return singleMatch;
        end; $f$;
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

create or replace procedure generateRbacIdentityViewFromProjection(targetTable text, sqlProjection text)
    language plpgsql as $$
declare
    sqlQuery text;
begin
    targettable := lower(targettable);

    sqlQuery = format($sql$
            select target.uuid, cleanIdentifier(%2$s) as idName
                from %1$s as target;
        $sql$, targetTable, sqlProjection);
    call generateRbacIdentityViewFromQuery(targetTable, sqlQuery);
end; $$;
--//


-- ============================================================================
--changeset rbac-generators-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure generateRbacRestrictedView(targetTable text, orderBy text, columnUpdates text = null, columnNames text = '*')
    language plpgsql as $$
declare
    sql text;
    newColumns text;
begin
    targetTable := lower(targetTable);
    if columnNames = '*' then
        columnNames := columnsNames(targetTable);
    end if;

    /*
        Creates a restricted view based on the 'SELECT' permission of the current subject.
    */
    sql := format($sql$
        create or replace view %1$s_rv as
            with accessible_%1$s_uuids as (

                -- TODO.perf: this CTE query makes RBAC-SELECT-permission-queries so slow (~500ms), any idea how to optimize?
                --  My guess is, that the depth of role-grants causes the problem.
                with recursive grants as (
                    select descendantUuid, ascendantUuid, 1 as level
                        from RbacGrants
                        where assumed
                          and ascendantUuid = any (currentSubjectsuUids())
                    union all
                    select g.descendantUuid, g.ascendantUuid, level + 1 as level
                        from RbacGrants g
                                 inner join grants on grants.descendantUuid = g.ascendantUuid
                        where g.assumed and level<10
                )
                select distinct perm.objectUuid as objectUuid
                    from grants
                             join RbacPermission perm on grants.descendantUuid = perm.uuid
                             join RbacObject obj on obj.uuid = perm.objectUuid
                    where obj.objectTable = '%1$s' -- 'SELECT' permission is included in all other permissions
                    limit 8001
            )
            select target.*
                from %1$s as target
                where target.uuid in (select * from accessible_%1$s_uuids)
                order by %2$s;

        grant all privileges on %1$s_rv to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
        $sql$, targetTable, orderBy);
    execute sql;

    /**
        Instead of insert trigger function for the restricted view.
     */
    newColumns := 'new.' || replace(columnNames, ',', ', new.');
    sql := format($sql$
    create or replace function %1$sInsert()
        returns trigger
        language plpgsql as $f$
    declare
        newTargetRow %1$s;
    begin
        insert
            into %1$s (%2$s)
            values (%3$s)
            returning * into newTargetRow;
        return newTargetRow;
    end; $f$;
    $sql$, targetTable, columnNames, newColumns);
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
            if old.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('DELETE', '%1$s', currentSubjectsUuids())) then
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
        based on the 'UPDATE' permission of the current subject.
     */
    if columnUpdates is not null then
        sql := format($sql$
            create or replace function %1$sUpdate()
                returns trigger
                language plpgsql as $f$
            begin
                if old.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('UPDATE', '%1$s', currentSubjectsUuids())) then
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
    end if;
end; $$;
--//
