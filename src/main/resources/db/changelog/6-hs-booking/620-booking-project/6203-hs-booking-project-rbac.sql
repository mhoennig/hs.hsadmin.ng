--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-booking-project-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_booking_project');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-booking-project-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsBookingProject', 'hs_booking_project');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-booking-project-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_booking_project_build_rbac_system(
    NEW hs_booking_project
)
    language plpgsql as $$

declare
    newDebitor hs_office.debitor;
    newDebitorRel hs_office.relation;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office.debitor WHERE uuid = NEW.debitorUuid    INTO newDebitor;
    assert newDebitor.uuid is not null, format('newDebitor must not be null for NEW.debitorUuid = %s', NEW.debitorUuid);

    SELECT debitorRel.*
        FROM hs_office.relation debitorRel
        JOIN hs_office.debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
        WHERE debitor.uuid = NEW.debitorUuid
        INTO newDebitorRel;
    assert newDebitorRel.uuid is not null, format('newDebitorRel must not be null for NEW.debitorUuid = %s', NEW.debitorUuid);


    perform rbac.defineRoleWithGrants(
        hsBookingProjectOWNER(NEW),
            incomingSuperRoles => array[hsOfficeRelationAGENT(newDebitorRel, rbac.unassumed())]
    );

    perform rbac.defineRoleWithGrants(
        hsBookingProjectADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsBookingProjectOWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hsBookingProjectAGENT(NEW),
            incomingSuperRoles => array[hsBookingProjectADMIN(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hsBookingProjectTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsBookingProjectAGENT(NEW)],
            outgoingSubRoles => array[hsOfficeRelationTENANT(newDebitorRel)]
    );

    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'DELETE'), rbac.globalAdmin());

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_booking_project row.
 */

create or replace function hs_booking_project_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_booking_project_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on hs_booking_project
    for each row
execute procedure hs_booking_project_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-booking-project-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to hs_office.relation ----------------------------

/*
    Grants INSERT INTO hs_booking_project permissions to specified role of pre-existing hs_office.relation rows.
 */
do language plpgsql $$
    declare
        row hs_office.relation;
    begin
        call base.defineContext('create INSERT INTO hs_booking_project permissions for pre-exising hs_office.relation rows');

        FOR row IN SELECT * FROM hs_office.relation
            WHERE type = 'DEBITOR'
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_booking_project'),
                        hsOfficeRelationADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_booking_project INSERT permission to specified role of new relation rows.
*/
create or replace function new_hsbk_project_grants_insert_to_relation_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if NEW.type = 'DEBITOR' then
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_booking_project'),
            hsOfficeRelationADMIN(NEW));
    end if;
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_booking_project_grants_after_insert_tg
    after insert on hs_office.relation
    for each row
execute procedure new_hsbk_project_grants_insert_to_relation_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-booking-project-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_booking_project.
*/
create or replace function hs_booking_project_insert_permission_check_tf()
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
    assert superObjectUuid is not null, 'object uuid fetched depending on hs_booking_project.debitorUuid must not be null, also check fetchSql in RBAC DSL';
    if rbac.hasInsertPermission(superObjectUuid, 'hs_booking_project') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_booking_project values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger hs_booking_project_insert_permission_check_tg
    before insert on hs_booking_project
    for each row
        execute procedure hs_booking_project_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-booking-project-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromQuery('hs_booking_project',
    $idName$
        SELECT bookingProject.uuid as uuid, debitorIV.idName || '-' || base.cleanIdentifier(bookingProject.caption) as idName
            FROM hs_booking_project bookingProject
            JOIN hs_office.debitor_iv debitorIV ON debitorIV.uuid = bookingProject.debitorUuid
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-booking-project-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_booking_project',
    $orderBy$
        caption
    $orderBy$,
    $updates$
        version = new.version,
        caption = new.caption
    $updates$);
--//

