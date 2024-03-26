--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-office-sepamandate-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_sepamandate');
--//


-- ============================================================================
--changeset hs-office-sepamandate-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeSepaMandate', 'hs_office_sepamandate');
--//


-- ============================================================================
--changeset hs-office-sepamandate-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficeSepaMandate(
    NEW hs_office_sepamandate
)
    language plpgsql as $$

declare
    newBankAccount hs_office_bankaccount;
    newDebitorRel hs_office_relation;

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office_bankaccount WHERE uuid = NEW.bankAccountUuid    INTO newBankAccount;

    SELECT * FROM hs_office_relation WHERE uuid = NEW.debitorRelUuid    INTO newDebitorRel;

    perform createRoleWithGrants(
        hsOfficeSepaMandateOwner(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalAdmin()],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficeSepaMandateAdmin(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficeSepaMandateOwner(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficeSepaMandateAgent(NEW),
            incomingSuperRoles => array[hsOfficeSepaMandateAdmin(NEW)],
            outgoingSubRoles => array[
            	hsOfficeBankAccountReferrer(newBankAccount),
            	hsOfficeRelationAgent(newDebitorRel)]
    );

    perform createRoleWithGrants(
        hsOfficeSepaMandateReferrer(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hsOfficeBankAccountAdmin(newBankAccount),
            	hsOfficeRelationAgent(newDebitorRel),
            	hsOfficeSepaMandateAgent(NEW)],
            outgoingSubRoles => array[hsOfficeRelationTenant(newDebitorRel)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_sepamandate row.
 */

create or replace function insertTriggerForHsOfficeSepaMandate_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficeSepaMandate(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficeSepaMandate_tg
    after insert on hs_office_sepamandate
    for each row
execute procedure insertTriggerForHsOfficeSepaMandate_tf();
--//


-- ============================================================================
--changeset hs-office-sepamandate-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_sepamandate,
    where only global-admin has that permission.
*/
create or replace function hs_office_sepamandate_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into hs_office_sepamandate not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_sepamandate_insert_permission_check_tg
    before insert on hs_office_sepamandate
    for each row
    when ( not isGlobalAdmin() )
        execute procedure hs_office_sepamandate_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset hs-office-sepamandate-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_sepamandate',
    $idName$
        concat(tradeName, familyName, givenName)
    $idName$);
--//

-- ============================================================================
--changeset hs-office-sepamandate-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_sepamandate',
    $orderBy$
        concat(tradeName, familyName, givenName)
    $orderBy$,
    $updates$
        reference = new.reference,
        agreement = new.agreement,
        validity = new.validity
    $updates$);
--//

