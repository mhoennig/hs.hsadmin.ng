--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-office-relation-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_relation');
--//


-- ============================================================================
--changeset hs-office-relation-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeRelation', 'hs_office_relation');
--//


-- ============================================================================
--changeset hs-office-relation-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficeRelation(
    NEW hs_office_relation
)
    language plpgsql as $$

declare
    newHolderPerson hs_office_person;
    newAnchorPerson hs_office_person;
    newContact hs_office_contact;

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office_person WHERE uuid = NEW.holderUuid    INTO newHolderPerson;
    assert newHolderPerson.uuid is not null, format('newHolderPerson must not be null for NEW.holderUuid = %s', NEW.holderUuid);

    SELECT * FROM hs_office_person WHERE uuid = NEW.anchorUuid    INTO newAnchorPerson;
    assert newAnchorPerson.uuid is not null, format('newAnchorPerson must not be null for NEW.anchorUuid = %s', NEW.anchorUuid);

    SELECT * FROM hs_office_contact WHERE uuid = NEW.contactUuid    INTO newContact;
    assert newContact.uuid is not null, format('newContact must not be null for NEW.contactUuid = %s', NEW.contactUuid);


    perform createRoleWithGrants(
        hsOfficeRelationOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalADMIN()],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficeRelationADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficeRelationOWNER(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficeRelationAGENT(NEW),
            incomingSuperRoles => array[hsOfficeRelationADMIN(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficeRelationTENANT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hsOfficeContactADMIN(newContact),
            	hsOfficeRelationAGENT(NEW)],
            outgoingSubRoles => array[
            	hsOfficeContactREFERRER(newContact),
            	hsOfficePersonREFERRER(newAnchorPerson),
            	hsOfficePersonREFERRER(newHolderPerson)]
    );

    IF NEW.type = 'REPRESENTATIVE' THEN
        call grantRoleToRole(hsOfficePersonOWNER(newAnchorPerson), hsOfficeRelationADMIN(NEW));
        call grantRoleToRole(hsOfficeRelationAGENT(NEW), hsOfficePersonADMIN(newAnchorPerson));
        call grantRoleToRole(hsOfficeRelationOWNER(NEW), hsOfficePersonADMIN(newHolderPerson));
    ELSE
        call grantRoleToRole(hsOfficeRelationAGENT(NEW), hsOfficePersonADMIN(newHolderPerson));
        call grantRoleToRole(hsOfficeRelationOWNER(NEW), hsOfficePersonADMIN(newAnchorPerson));
    END IF;

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_relation row.
 */

create or replace function insertTriggerForHsOfficeRelation_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficeRelation(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficeRelation_tg
    after insert on hs_office_relation
    for each row
execute procedure insertTriggerForHsOfficeRelation_tf();
--//


-- ============================================================================
--changeset hs-office-relation-rbac-update-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure updateRbacRulesForHsOfficeRelation(
    OLD hs_office_relation,
    NEW hs_office_relation
)
    language plpgsql as $$
begin

    if NEW.contactUuid is distinct from OLD.contactUuid then
        delete from rbacgrants g where g.grantedbytriggerof = OLD.uuid;
        call buildRbacSystemForHsOfficeRelation(NEW);
    end if;
end; $$;

/*
    AFTER INSERT TRIGGER to re-wire the grant structure for a new hs_office_relation row.
 */

create or replace function updateTriggerForHsOfficeRelation_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call updateRbacRulesForHsOfficeRelation(OLD, NEW);
    return NEW;
end; $$;

create trigger updateTriggerForHsOfficeRelation_tg
    after update on hs_office_relation
    for each row
execute procedure updateTriggerForHsOfficeRelation_tf();
--//


-- ============================================================================
--changeset hs-office-relation-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO hs_office_relation permissions for the related hs_office_person rows.
 */
do language plpgsql $$
    declare
        row hs_office_person;
    begin
        call defineContext('create INSERT INTO hs_office_relation permissions for the related hs_office_person rows');

        FOR row IN SELECT * FROM hs_office_person
            LOOP
                call grantPermissionToRole(
                    createPermission(row.uuid, 'INSERT', 'hs_office_relation'),
                    hsOfficePersonADMIN(row));
            END LOOP;
    END;
$$;

/**
    Adds hs_office_relation INSERT permission to specified role of new hs_office_person rows.
*/
create or replace function hs_office_relation_hs_office_person_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_relation'),
            hsOfficePersonADMIN(NEW));
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_hs_office_relation_hs_office_person_insert_tg
    after insert on hs_office_person
    for each row
execute procedure hs_office_relation_hs_office_person_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_relation,
    where the check is performed by a direct role.

    A direct role is a role depending on a foreign key directly available in the NEW row.
*/
create or replace function hs_office_relation_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into hs_office_relation not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_relation_insert_permission_check_tg
    before insert on hs_office_relation
    for each row
    when ( not hasInsertPermission(NEW.anchorUuid, 'INSERT', 'hs_office_relation') )
        execute procedure hs_office_relation_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset hs-office-relation-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_relation',
    $idName$
             (select idName from hs_office_person_iv p where p.uuid = anchorUuid)
             || '-with-' || target.type || '-'
             || (select idName from hs_office_person_iv p where p.uuid = holderUuid)
    $idName$);
--//

-- ============================================================================
--changeset hs-office-relation-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_relation',
    $orderBy$
        (select idName from hs_office_person_iv p where p.uuid = target.holderUuid)
    $orderBy$,
    $updates$
        contactUuid = new.contactUuid
    $updates$);
--//

