--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:rbac-generators-RELATED-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure rbac.generateRelatedRbacObject(targetTable varchar)
    language plpgsql as $$
declare
    targetTableName text;
    targetSchemaPrefix text;
    createInsertTriggerSQL text;
    createDeleteTriggerSQL text;
begin
    if POSITION('.' IN targetTable) > 0 then
        targetSchemaPrefix := SPLIT_PART(targetTable, '.', 1) || '.';
        targetTableName := SPLIT_PART(targetTable, '.', 2);
    else
        targetSchemaPrefix := '';
        targetTableName := targetTable;
    end if;

    if targetSchemaPrefix = '' and targetTableName = 'customer' then
        raise exception 'missing targetShemaPrefix: %', targetTable;
    end if;

    createInsertTriggerSQL = format($sql$
        create trigger createRbacObjectFor_%s_insert_tg_1058_25
            before insert on %s%s
            for each row
                execute procedure rbac.insert_related_object();
        $sql$, targetTableName, targetSchemaPrefix, targetTableName);
    execute createInsertTriggerSQL;

    createDeleteTriggerSQL = format($sql$
        create trigger createRbacObjectFor_%s_delete_tg_1058_35
            after delete on %s%s
            for each row
                execute procedure rbac.delete_related_rbac_rules_tf();
        $sql$, targetTableName, targetSchemaPrefix, targetTableName);
    execute createDeleteTriggerSQL;
end;
$$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-generators-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------

create procedure rbac.generateRbacRoleDescriptors(targetTable text)
    language plpgsql as $$
declare
    sql text;
begin
    sql = format($sql$
        create or replace function %1$s_OWNER(entity %1$s, assumed boolean = true)
            returns rbac.RoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return rbac.roleDescriptorOf('%1$s', entity.uuid, 'OWNER', assumed);
        end; $f$;

        create or replace function %1$s_ADMIN(entity %1$s, assumed boolean = true)
            returns rbac.RoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return rbac.roleDescriptorOf('%1$s', entity.uuid, 'ADMIN', assumed);
        end; $f$;

        create or replace function %1$s_AGENT(entity %1$s, assumed boolean = true)
            returns rbac.RoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return rbac.roleDescriptorOf('%1$s', entity.uuid, 'AGENT', assumed);
        end; $f$;

        create or replace function %1$s_TENANT(entity %1$s, assumed boolean = true)
            returns rbac.RoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return rbac.roleDescriptorOf('%1$s', entity.uuid, 'TENANT', assumed);
        end; $f$;

        -- TODO: remove guest role
        create or replace function %1$s_GUEST(entity %1$s, assumed boolean = true)
            returns rbac.RoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return rbac.roleDescriptorOf('%1$s', entity.uuid, 'GUEST', assumed);
        end; $f$;

        create or replace function %1$s_REFERRER(entity %1$s)
            returns rbac.RoleDescriptor
            language plpgsql
            strict as $f$
        begin
            return rbac.roleDescriptorOf('%1$s', entity.uuid, 'REFERRER');
        end; $f$;

        $sql$, targetTable);
    execute sql;
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-generators-IDENTITY-VIEW runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure rbac.generateRbacIdentityViewFromQuery(targetTable text, sqlQuery text)
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
        create or replace function %1$s_uuid_by_id_name(givenIdName varchar)
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
        create or replace function %1$s_id_name_by_uuid(givenUuid uuid)
            returns varchar
            language sql
            strict as $f$
        select idName from %1$s_iv iv where iv.uuid = givenUuid;
        $f$;
    $sql$, targetTable);
    execute sql;
end; $$;

create or replace procedure rbac.generateRbacIdentityViewFromProjection(targetTable text, sqlProjection text)
    language plpgsql as $$
declare
    sqlQuery text;
begin
    targettable := lower(targettable);

    sqlQuery = format($sql$
            select target.uuid, base.cleanIdentifier(%2$s) as idName
                from %1$s as target;
        $sql$, targetTable, sqlProjection);
    call rbac.generateRbacIdentityViewFromQuery(targetTable, sqlQuery);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:rbac-generators-RESTRICTED-VIEW runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace procedure rbac.generateRbacRestrictedView(targetTable text, orderBy text, columnUpdates text = null, columnNames text = '*')
    language plpgsql as $$
declare
    sql text;
    newColumns text;
begin
    targetTable := lower(targetTable);
    if columnNames = '*' then
        columnNames := base.tableColumnNames(targetTable);
    end if;

    /*
        Creates a restricted view based on the 'SELECT' permission of the current subject.
    */
    sql := format($sql$
        create or replace view %1$s_rv as
            with accessible_uuids as (
                     with recursive
                          recursive_grants as
                              (select distinct rbac.grant.descendantuuid,
                                               rbac.grant.ascendantuuid,
                                               1 as level,
                                               true
                                   from rbac.grant
                                   where rbac.grant.assumed
                                     and (rbac.grant.ascendantuuid = any (rbac.currentSubjectOrAssumedRolesUuids()))
                               union all
                               select distinct g.descendantuuid,
                                               g.ascendantuuid,
                                               grants.level + 1 as level,
                                               base.assertTrue(grants.level < 22, 'too many grant-levels: ' || grants.level)
                                   from rbac.grant g
                                            join recursive_grants grants on grants.descendantuuid = g.ascendantuuid
                                   where g.assumed),
                          grant_count AS (
                            SELECT COUNT(*) AS grant_count FROM recursive_grants
                          ),
                          count_check as (select base.assertTrue((select count(*) as grant_count from recursive_grants) < 400000,
                                'too many grants for current subjects: ' || (select count(*) as grant_count from recursive_grants))
                                                     as valid)
                      select distinct perm.objectuuid
                          from recursive_grants
                                   join rbac.permission perm on recursive_grants.descendantuuid = perm.uuid
                                   join rbac.object obj on obj.uuid = perm.objectuuid
                                   join count_check cc on cc.valid
                          where obj.objectTable = '%1$s' -- 'SELECT' permission is included in all other permissions
            )
            select target.*
                from %1$s as target
                where rbac.hasGlobalAdminRole() or target.uuid in (select * from accessible_uuids)
                order by %2$s;

        grant all privileges on %1$s_rv to ${HSADMINNG_POSTGRES_RESTRICTED_USERNAME};
        $sql$, targetTable, orderBy);
    execute sql;

    /**
        Instead of insert trigger function for the restricted view.
     */
    newColumns := 'new.' || replace(columnNames, ', ', ', new.');
    sql := format($sql$
    create or replace function %1$s_instead_of_insert_tf()
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
        create or replace trigger instead_of_insert_tg
            instead of insert
            on %1$s_rv
            for each row
        execute function %1$s_instead_of_insert_tf();
    $sql$, targetTable);
    execute sql;

    /**
        Instead of delete trigger function for the restricted view.
     */
    sql := format($sql$
        create or replace function %1$s_instead_of_delete_tf()
            returns trigger
            language plpgsql as $f$
        begin
            if old.uuid in (select rbac.queryAccessibleObjectUuidsOfSubjectIds('DELETE', '%1$s', rbac.currentSubjectOrAssumedRolesUuids())) then
                delete from %1$s p where p.uuid = old.uuid;
                return old;
            end if;
            raise exception '[403] Subject %% is not allowed to delete %1$s uuid %%', rbac.currentSubjectOrAssumedRolesUuids(), old.uuid;
        end; $f$;
    $sql$, targetTable);
    execute sql;

    /*
        Creates an instead of delete trigger for the restricted view.
     */
    sql := format($sql$
        create or replace trigger instead_of_delete_tg
            instead of delete
            on %1$s_rv
            for each row
        execute function %1$s_instead_of_delete_tf();
    $sql$, targetTable);
    execute sql;

    /**
        Instead of update trigger function for the restricted view
        based on the 'UPDATE' permission of the current subject.
     */
    if columnUpdates is not null then
        sql := format($sql$
            create or replace function %1$s_instead_of_update_tf()
                returns trigger
                language plpgsql as $f$
            begin
                if old.uuid in (select rbac.queryAccessibleObjectUuidsOfSubjectIds('UPDATE', '%1$s', rbac.currentSubjectOrAssumedRolesUuids())) then
                    update %1$s
                        set %2$s
                        where uuid = old.uuid;
                    return old;
                end if;
                raise exception '[403] Subject %% is not allowed to update %1$s uuid %%', rbac.currentSubjectOrAssumedRolesUuids(), old.uuid;
            end; $f$;
        $sql$, targetTable, columnUpdates);
        execute sql;

        /*
            Creates an instead of delete trigger for the restricted view.
         */
        sql = format($sql$
            create or replace trigger instead_of_update_tg
                instead of update
                on %1$s_rv
                for each row
            execute function %1$s_instead_of_update_tf();
        $sql$, targetTable);
        execute sql;
    end if;
end; $$;
--//
