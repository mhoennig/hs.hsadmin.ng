--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-office-bankaccount-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_bankaccount');
--//


-- ============================================================================
--changeset hs-office-bankaccount-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeBankAccount', 'hs_office_bankaccount');
--//


-- ============================================================================
--changeset hs-office-bankaccount-rbac-insert-trigger:1 endDelimiter:--//
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
    call enterTriggerForObjectUuid(NEW.uuid);

    perform createRoleWithGrants(
        hsOfficeBankAccountOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalADMIN()],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficeBankAccountADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficeBankAccountOWNER(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficeBankAccountREFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsOfficeBankAccountADMIN(NEW)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
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
--changeset hs-office-bankaccount-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_bankaccount',
    $idName$
        iban
    $idName$);
--//


-- ============================================================================
--changeset hs-office-bankaccount-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_bankaccount',
    $orderBy$
        iban
    $orderBy$,
    $updates$
        holder = new.holder,
        iban = new.iban,
        bic = new.bic
    $updates$);
--//

