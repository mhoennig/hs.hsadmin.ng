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
        hsOfficeBankAccountOwner(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalAdmin()],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficeBankAccountAdmin(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficeBankAccountOwner(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficeBankAccountReferrer(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsOfficeBankAccountAdmin(NEW)]
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
--changeset hs-office-bankaccount-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_bankaccount,
    where only global-admin has that permission.
*/
create or replace function hs_office_bankaccount_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into hs_office_bankaccount not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_bankaccount_insert_permission_check_tg
    before insert on hs_office_bankaccount
    for each row
    when ( not isGlobalAdmin() )
        execute procedure hs_office_bankaccount_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset hs-office-bankaccount-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_bankaccount',
    $idName$
        iban || ':' || holder
    $idName$);
--//

-- ============================================================================
--changeset hs-office-bankaccount-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_bankaccount',
    $orderBy$
        iban || ':' || holder
    $orderBy$,
    $updates$
        holder = new.holder,
        iban = new.iban,
        bic = new.bic
    $updates$);
--//

