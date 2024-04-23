--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-booking-item-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_booking_item');
--//


-- ============================================================================
--changeset hs-booking-item-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsBookingItem', 'hs_booking_item');
--//


-- ============================================================================
--changeset hs-booking-item-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsBookingItem(
    NEW hs_booking_item
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
        hsBookingItemOWNER(NEW),
            incomingSuperRoles => array[hsOfficeRelationAGENT(newDebitorRel)]
    );

    perform createRoleWithGrants(
        hsBookingItemADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[
            	hsBookingItemOWNER(NEW),
            	hsOfficeRelationAGENT(newDebitorRel)]
    );

    perform createRoleWithGrants(
        hsBookingItemAGENT(NEW),
            incomingSuperRoles => array[hsBookingItemADMIN(NEW)]
    );

    perform createRoleWithGrants(
        hsBookingItemTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsBookingItemAGENT(NEW)],
            outgoingSubRoles => array[hsOfficeRelationTENANT(newDebitorRel)]
    );

    call grantPermissionToRole(createPermission(NEW.uuid, 'DELETE'), globalAdmin());

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_booking_item row.
 */

create or replace function insertTriggerForHsBookingItem_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsBookingItem(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsBookingItem_tg
    after insert on hs_booking_item
    for each row
execute procedure insertTriggerForHsBookingItem_tf();
--//


-- ============================================================================
--changeset hs-booking-item-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO hs_booking_item permissions for the related hs_office_relation rows.
 */
do language plpgsql $$
    declare
        row hs_office_relation;
    begin
        call defineContext('create INSERT INTO hs_booking_item permissions for the related hs_office_relation rows');

        FOR row IN SELECT * FROM hs_office_relation
			WHERE type = 'DEBITOR'
            LOOP
                call grantPermissionToRole(
                    createPermission(row.uuid, 'INSERT', 'hs_booking_item'),
                    hsOfficeRelationADMIN(row));
            END LOOP;
    END;
$$;

/**
    Adds hs_booking_item INSERT permission to specified role of new hs_office_relation rows.
*/
create or replace function hs_booking_item_hs_office_relation_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if NEW.type = 'DEBITOR' then
		call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_booking_item'),
            hsOfficeRelationADMIN(NEW));
	end if;
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_hs_booking_item_hs_office_relation_insert_tg
    after insert on hs_office_relation
    for each row
execute procedure hs_booking_item_hs_office_relation_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_booking_item,
    where the check is performed by an indirect role.

    An indirect role is a role which depends on an object uuid which is not a direct foreign key
    of the source entity, but needs to be fetched via joined tables.
*/
create or replace function hs_booking_item_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$

declare
    superRoleObjectUuid uuid;

begin
        superRoleObjectUuid := (SELECT debitorRel.uuid
            FROM hs_office_relation debitorRel
            JOIN hs_office_debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
            WHERE debitor.uuid = NEW.debitorUuid
        );
        assert superRoleObjectUuid is not null, 'superRoleObjectUuid must not be null';

        if ( not hasInsertPermission(superRoleObjectUuid, 'INSERT', 'hs_booking_item') ) then
            raise exception
                '[403] insert into hs_booking_item not allowed for current subjects % (%)',
                currentSubjects(), currentSubjectsUuids();
    end if;
    return NEW;
end; $$;

create trigger hs_booking_item_insert_permission_check_tg
    before insert on hs_booking_item
    for each row
        execute procedure hs_booking_item_insert_permission_check_tf();
--//

-- ============================================================================
--changeset hs-booking-item-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

    call generateRbacIdentityViewFromQuery('hs_booking_item',
        $idName$
            SELECT bookingItem.uuid as uuid, debitorIV.idName || '-' || cleanIdentifier(bookingItem.caption) as idName
            FROM hs_booking_item bookingItem
            JOIN hs_office_debitor_iv debitorIV ON debitorIV.uuid = bookingItem.debitorUuid
        $idName$);
--//

-- ============================================================================
--changeset hs-booking-item-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_booking_item',
    $orderBy$
        validity
    $orderBy$,
    $updates$
        version = new.version,
        caption = new.caption,
        validity = new.validity,
        resources = new.resources
    $updates$);
--//

