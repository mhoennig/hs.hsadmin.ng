--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-relation-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office.relation');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-relation-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsOfficeRelation', 'hs_office.relation');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-relation-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_office.relation_build_rbac_system(
    NEW hs_office.relation
)
    language plpgsql as $$

declare
    newHolderPerson hs_office.person;
    newAnchorPerson hs_office.person;
    newContact hs_office.contact;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office.person WHERE uuid = NEW.holderUuid    INTO newHolderPerson;
    assert newHolderPerson.uuid is not null, format('newHolderPerson must not be null for NEW.holderUuid = %s', NEW.holderUuid);

    SELECT * FROM hs_office.person WHERE uuid = NEW.anchorUuid    INTO newAnchorPerson;
    assert newAnchorPerson.uuid is not null, format('newAnchorPerson must not be null for NEW.anchorUuid = %s', NEW.anchorUuid);

    SELECT * FROM hs_office.contact WHERE uuid = NEW.contactUuid    INTO newContact;
    assert newContact.uuid is not null, format('newContact must not be null for NEW.contactUuid = %s', NEW.contactUuid);


    perform rbac.defineRoleWithGrants(
        hsOfficeRelationOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.globalADMIN()],
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
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office.relation row.
 */

create or replace function hs_office.relation_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.relation_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on hs_office.relation
    for each row
execute procedure hs_office.relation_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-relation-rbac-update-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure hs_office.relation_update_rbac_system(
    OLD hs_office.relation,
    NEW hs_office.relation
)
    language plpgsql as $$
begin

    if NEW.contactUuid is distinct from OLD.contactUuid then
        delete from rbac.grants g where g.grantedbytriggerof = OLD.uuid;
        call hs_office.relation_build_rbac_system(NEW);
    end if;
end; $$;

/*
    AFTER UPDATE TRIGGER to re-wire the grant structure for a new hs_office.relation row.
 */

create or replace function hs_office.relation_update_rbac_system_after_update_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.relation_update_rbac_system(OLD, NEW);
    return NEW;
end; $$;

create trigger update_rbac_system_after_update_tg
    after update on hs_office.relation
    for each row
execute procedure hs_office.relation_update_rbac_system_after_update_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-relation-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to hs_office.person ----------------------------

/*
    Grants INSERT INTO hs_office.relation permissions to specified role of pre-existing hs_office.person rows.
 */
do language plpgsql $$
    declare
        row hs_office.person;
    begin
        call base.defineContext('create INSERT INTO hs_office.relation permissions for pre-exising hs_office.person rows');

        FOR row IN SELECT * FROM hs_office.person
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office.relation'),
                        hsOfficePersonADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_office.relation INSERT permission to specified role of new person rows.
*/
create or replace function hs_office.new_relation_grants_insert_to_person_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office.relation'),
            hsOfficePersonADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_relation_grants_after_insert_tg
    after insert on hs_office.person
    for each row
execute procedure hs_office.new_relation_grants_insert_to_person_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-relation-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office.relation.
*/
create or replace function hs_office.relation_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via direct foreign key: NEW.anchorUuid
    if rbac.hasInsertPermission(NEW.anchorUuid, 'hs_office.relation') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office.relation values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger relation_insert_permission_check_tg
    before insert on hs_office.relation
    for each row
        execute procedure hs_office.relation_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-relation-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office.relation',
    $idName$
             (select idName from hs_office.person_iv p where p.uuid = anchorUuid)
             || '-with-' || target.type || '-'
             || (select idName from hs_office.person_iv p where p.uuid = holderUuid)
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-relation-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office.relation',
    $orderBy$
        (select idName from hs_office.person_iv p where p.uuid = target.holderUuid)
    $orderBy$,
    $updates$
        contactUuid = new.contactUuid
    $updates$);
--//

