--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset michael.hoennig:hs-office-relation-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office_relation');
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-relation-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsOfficeRelation', 'hs_office_relation');
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-relation-rbac-insert-trigger endDelimiter:--//
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
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office_person WHERE uuid = NEW.holderUuid    INTO newHolderPerson;
    assert newHolderPerson.uuid is not null, format('newHolderPerson must not be null for NEW.holderUuid = %s', NEW.holderUuid);

    SELECT * FROM hs_office_person WHERE uuid = NEW.anchorUuid    INTO newAnchorPerson;
    assert newAnchorPerson.uuid is not null, format('newAnchorPerson must not be null for NEW.anchorUuid = %s', NEW.anchorUuid);

    SELECT * FROM hs_office_contact WHERE uuid = NEW.contactUuid    INTO newContact;
    assert newContact.uuid is not null, format('newContact must not be null for NEW.contactUuid = %s', NEW.contactUuid);


    perform rbac.defineRoleWithGrants(
        hsOfficeRelationOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.globalAdmin()],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hsOfficeRelationADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficeRelationOWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hsOfficeRelationAGENT(NEW),
            incomingSuperRoles => array[hsOfficeRelationADMIN(NEW)]
    );

    perform rbac.defineRoleWithGrants(
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
        call rbac.grantRoleToRole(hsOfficePersonOWNER(newAnchorPerson), hsOfficeRelationADMIN(NEW));
        call rbac.grantRoleToRole(hsOfficeRelationAGENT(NEW), hsOfficePersonADMIN(newAnchorPerson));
        call rbac.grantRoleToRole(hsOfficeRelationOWNER(NEW), hsOfficePersonADMIN(newHolderPerson));
    ELSE
        call rbac.grantRoleToRole(hsOfficeRelationAGENT(NEW), hsOfficePersonADMIN(newHolderPerson));
        call rbac.grantRoleToRole(hsOfficeRelationOWNER(NEW), hsOfficePersonADMIN(newAnchorPerson));
    END IF;

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
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
--changeset michael.hoennig:hs-office-relation-rbac-update-trigger endDelimiter:--//
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
        delete from rbac.grants g where g.grantedbytriggerof = OLD.uuid;
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
--changeset michael.hoennig:hs-office-relation-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to hs_office_person ----------------------------

/*
    Grants INSERT INTO hs_office_relation permissions to specified role of pre-existing hs_office_person rows.
 */
do language plpgsql $$
    declare
        row hs_office_person;
    begin
        call base.defineContext('create INSERT INTO hs_office_relation permissions for pre-exising hs_office_person rows');

        FOR row IN SELECT * FROM hs_office_person
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office_relation'),
                        hsOfficePersonADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_office_relation INSERT permission to specified role of new hs_office_person rows.
*/
create or replace function new_hs_office_relation_grants_insert_to_hs_office_person_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office_relation'),
            hsOfficePersonADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_office_relation_grants_insert_to_hs_office_person_tg
    after insert on hs_office_person
    for each row
execute procedure new_hs_office_relation_grants_insert_to_hs_office_person_tf();


-- ============================================================================
--changeset michael.hoennig:hs_office_relation-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office_relation.
*/
create or replace function hs_office_relation_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via direct foreign key: NEW.anchorUuid
    if rbac.hasInsertPermission(NEW.anchorUuid, 'hs_office_relation') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office_relation not allowed for current subjects % (%)',
            base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger hs_office_relation_insert_permission_check_tg
    before insert on hs_office_relation
    for each row
        execute procedure hs_office_relation_insert_permission_check_tf();
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-relation-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office_relation',
    $idName$
             (select idName from hs_office_person_iv p where p.uuid = anchorUuid)
             || '-with-' || target.type || '-'
             || (select idName from hs_office_person_iv p where p.uuid = holderUuid)
    $idName$);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-relation-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office_relation',
    $orderBy$
        (select idName from hs_office_person_iv p where p.uuid = target.holderUuid)
    $orderBy$,
    $updates$
        contactUuid = new.contactUuid
    $updates$);
--//

