--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-bankaccount-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office_bankaccount');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-bankaccount-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsOfficeBankAccount', 'hs_office_bankaccount');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-bankaccount-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficeBankAccount(
    NEW hs_office_bankaccount
)
    language plpgsql as $$

declare

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    perform rbac.defineRoleWithGrants(
        hsOfficeBankAccountOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.globalADMIN()],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hsOfficeBankAccountADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficeBankAccountOWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hsOfficeBankAccountREFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsOfficeBankAccountADMIN(NEW)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_bankaccount row.
 */

create or replace function insertTriggerForHsOfficeBankAccount_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficeBankAccount(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficeBankAccount_tg
    after insert on hs_office_bankaccount
    for each row
execute procedure insertTriggerForHsOfficeBankAccount_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-bankaccount-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office_bankaccount',
    $idName$
        iban
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-bankaccount-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office_bankaccount',
    $orderBy$
        iban
    $orderBy$,
    $updates$
        holder = new.holder,
        iban = new.iban,
        bic = new.bic
    $updates$);
--//

