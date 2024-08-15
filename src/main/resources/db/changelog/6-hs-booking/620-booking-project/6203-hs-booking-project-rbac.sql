--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-booking-project-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_booking_project');
--//


-- ============================================================================
--changeset hs-booking-project-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsBookingProject', 'hs_booking_project');
--//


-- ============================================================================
--changeset hs-booking-project-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsBookingProject(
    NEW hs_booking_project
)
    language plpgsql as $$

declare
    newDebitor hs_office_debitor;
    newDebitorRel hs_office_relation;

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office_debitor WHERE uuid = NEW.debitorUuid    INTO newDebitor;
    assert newDebitor.uuid is not null, format('newDebitor must not be null for NEW.debitorUuid = %s', NEW.debitorUuid);

    SELECT debitorRel.*
        FROM hs_office_relation debitorRel
        JOIN hs_office_debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
        WHERE debitor.uuid = NEW.debitorUuid
        INTO newDebitorRel;
    assert newDebitorRel.uuid is not null, format('newDebitorRel must not be null for NEW.debitorUuid = %s', NEW.debitorUuid);


    perform createRoleWithGrants(
        hsBookingProjectOWNER(NEW),
            incomingSuperRoles => array[hsOfficeRelationAGENT(newDebitorRel, unassumed())]
    );

    perform createRoleWithGrants(
        hsBookingProjectADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsBookingProjectOWNER(NEW)]
    );

    perform createRoleWithGrants(
        hsBookingProjectAGENT(NEW),
            incomingSuperRoles => array[hsBookingProjectADMIN(NEW)]
    );

    perform createRoleWithGrants(
        hsBookingProjectTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsBookingProjectAGENT(NEW)],
            outgoingSubRoles => array[hsOfficeRelationTENANT(newDebitorRel)]
    );

    call grantPermissionToRole(createPermission(NEW.uuid, 'DELETE'), globalAdmin());

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_booking_project row.
 */

create or replace function insertTriggerForHsBookingProject_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsBookingProject(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsBookingProject_tg
    after insert on hs_booking_project
    for each row
execute procedure insertTriggerForHsBookingProject_tf();
--//


-- ============================================================================
--changeset hs-booking-project-rbac-GRANTING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to hs_office_relation ----------------------------

/*
    Grants INSERT INTO hs_booking_project permissions to specified role of pre-existing hs_office_relation rows.
 */
do language plpgsql $$
    declare
        row hs_office_relation;
    begin
        call defineContext('create INSERT INTO hs_booking_project permissions for pre-exising hs_office_relation rows');

        FOR row IN SELECT * FROM hs_office_relation
            WHERE type = 'DEBITOR'
            LOOP
                call grantPermissionToRole(
                        createPermission(row.uuid, 'INSERT', 'hs_booking_project'),
                        hsOfficeRelationADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_booking_project INSERT permission to specified role of new hs_office_relation rows.
*/
create or replace function new_hs_booking_project_grants_insert_to_hs_office_relation_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if NEW.type = 'DEBITOR' then
        call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_booking_project'),
            hsOfficeRelationADMIN(NEW));
    end if;
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_booking_project_grants_insert_to_hs_office_relation_tg
    after insert on hs_office_relation
    for each row
execute procedure new_hs_booking_project_grants_insert_to_hs_office_relation_tf();


-- ============================================================================
--changeset hs_booking_project-rbac-CHECKING-INSERT-PERMISSION:1 endDelimiter:--//
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
        FROM hs_office_relation debitorRel
        JOIN hs_office_debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
        WHERE debitor.uuid = NEW.debitorUuid
    );
    assert superObjectUuid is not null, 'object uuid fetched depending on hs_booking_project.debitorUuid must not be null, also check fetchSql in RBAC DSL';
    if hasInsertPermission(superObjectUuid, 'hs_booking_project') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_booking_project values(%) not allowed for current subjects % (%)',
            NEW, currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_booking_project_insert_permission_check_tg
    before insert on hs_booking_project
    for each row
        execute procedure hs_booking_project_insert_permission_check_tf();
--//


-- ============================================================================
--changeset hs-booking-project-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromQuery('hs_booking_project',
    $idName$
        SELECT bookingProject.uuid as uuid, debitorIV.idName || '-' || cleanIdentifier(bookingProject.caption) as idName
            FROM hs_booking_project bookingProject
            JOIN hs_office_debitor_iv debitorIV ON debitorIV.uuid = bookingProject.debitorUuid
    $idName$);
--//


-- ============================================================================
--changeset hs-booking-project-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_booking_project',
    $orderBy$
        caption
    $orderBy$,
    $updates$
        version = new.version,
        caption = new.caption
    $updates$);
--//

