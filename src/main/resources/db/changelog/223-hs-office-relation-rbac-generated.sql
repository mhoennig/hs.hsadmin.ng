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

    SELECT * FROM hs_office_person WHERE uuid = NEW.anchorUuid    INTO newAnchorPerson;

    SELECT * FROM hs_office_contact WHERE uuid = NEW.contactUuid    INTO newContact;

    perform createRoleWithGrants(
        hsOfficeRelationOwner(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalAdmin()],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficeRelationAdmin(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[
            	hsOfficePersonAdmin(newAnchorPerson),
            	hsOfficeRelationOwner(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficeRelationAgent(NEW),
            incomingSuperRoles => array[
            	hsOfficePersonAdmin(newHolderPerson),
            	hsOfficeRelationAdmin(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficeRelationTenant(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hsOfficeContactAdmin(newContact),
            	hsOfficePersonAdmin(newHolderPerson),
            	hsOfficeRelationAgent(NEW)],
            outgoingSubRoles => array[
            	hsOfficeContactReferrer(newContact),
            	hsOfficePersonReferrer(newAnchorPerson),
            	hsOfficePersonReferrer(newHolderPerson)]
    );

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

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_relation,
    where only global-admin has that permission.
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
    when ( not isGlobalAdmin() )
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

