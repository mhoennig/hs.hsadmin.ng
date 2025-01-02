--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-booking-project-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_booking.project');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-booking-project-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_booking.project');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-booking-project-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_booking.project_build_rbac_system(
    NEW hs_booking.project
)
    language plpgsql as $$

declare
    newDebitor hs_office.debitor;
    newDebitorRel hs_office.relation;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office.debitor WHERE uuid = NEW.debitorUuid    INTO newDebitor;
    assert newDebitor.uuid is not null, format('newDebitor must not be null for NEW.debitorUuid = %s of hs_booking.project', NEW.debitorUuid);

    SELECT debitorRel.*
        FROM hs_office.relation debitorRel
        JOIN hs_office.debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
        WHERE debitor.uuid = NEW.debitorUuid
        INTO newDebitorRel;
    assert newDebitorRel.uuid is not null, format('newDebitorRel must not be null for NEW.debitorUuid = %s of hs_booking.project', NEW.debitorUuid);


    perform rbac.defineRoleWithGrants(
        hs_booking.project_OWNER(NEW),
            incomingSuperRoles => array[hs_office.relation_AGENT(newDebitorRel, rbac.unassumed())]
    );

    perform rbac.defineRoleWithGrants(
        hs_booking.project_ADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hs_booking.project_OWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hs_booking.project_AGENT(NEW),
            incomingSuperRoles => array[hs_booking.project_ADMIN(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hs_booking.project_TENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hs_booking.project_AGENT(NEW)],
            outgoingSubRoles => array[hs_office.relation_TENANT(newDebitorRel)]
    );

    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'DELETE'), rbac.global_ADMIN());

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_booking.project row.
 */

create or replace function hs_booking.project_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_booking.project_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on hs_booking.project
    for each row
execute procedure hs_booking.project_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-booking-project-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to hs_office.relation ----------------------------

/*
    Grants INSERT INTO hs_booking.project permissions to specified role of pre-existing hs_office.relation rows.
 */
do language plpgsql $$
    declare
        row hs_office.relation;
    begin
        call base.defineContext('create INSERT INTO hs_booking.project permissions for pre-exising hs_office.relation rows');

        FOR row IN SELECT * FROM hs_office.relation
            WHERE type = 'DEBITOR'
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_booking.project'),
                        hs_office.relation_ADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_booking.project INSERT permission to specified role of new relation rows.
*/
create or replace function hs_booking.project_grants_insert_to_relation_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if NEW.type = 'DEBITOR' then
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_booking.project'),
            hs_office.relation_ADMIN(NEW));
    end if;
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger project_z_grants_after_insert_tg
    after insert on hs_office.relation
    for each row
execute procedure hs_booking.project_grants_insert_to_relation_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-booking-project-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_booking.project.
*/
create or replace function hs_booking.project_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via indirect foreign key: NEW.debitorUuid
    superObjectUuid := (SELECT debitorRel.uuid
        FROM hs_office.relation debitorRel
        JOIN hs_office.debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
        WHERE debitor.uuid = NEW.debitorUuid
    );
    assert superObjectUuid is not null, 'object uuid fetched depending on hs_booking.project.debitorUuid must not be null, also check fetchSql in RBAC DSL';
    if rbac.hasInsertPermission(superObjectUuid, 'hs_booking.project') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_booking.project values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger project_insert_permission_check_tg
    before insert on hs_booking.project
    for each row
        execute procedure hs_booking.project_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-booking-project-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromQuery('hs_booking.project',
    $idName$
        SELECT bookingProject.uuid as uuid, debitorIV.idName || '-' || base.cleanIdentifier(bookingProject.caption) as idName
            FROM hs_booking.project bookingProject
            JOIN hs_office.debitor_iv debitorIV ON debitorIV.uuid = bookingProject.debitorUuid
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-booking-project-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_booking.project',
    $orderBy$
        caption
    $orderBy$,
    $updates$
        version = new.version,
        caption = new.caption
    $updates$);
--//


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-booking-project-rbac-rebuild endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_booking.project after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_booking.project', null, <<insert executing global admin user here>>);
--  call hs_booking.project_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_booking.project.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_booking.project.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_booking.project_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_booking.project;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grants;

    FOR row IN SELECT * FROM hs_booking.project LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grants g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_booking.project_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grants;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

