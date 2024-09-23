--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-bankaccount-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office.bankaccount');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-bankaccount-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_office.bankaccount');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-bankaccount-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_office.bankaccount_build_rbac_system(
    NEW hs_office.bankaccount
)
    language plpgsql as $$

declare

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    perform rbac.defineRoleWithGrants(
        hs_office.bankaccount_OWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.global_ADMIN()],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.bankaccount_ADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hs_office.bankaccount_OWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.bankaccount_REFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hs_office.bankaccount_ADMIN(NEW)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office.bankaccount row.
 */

create or replace function hs_office.bankaccount_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.bankaccount_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on hs_office.bankaccount
    for each row
execute procedure hs_office.bankaccount_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-bankaccount-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office.bankaccount',
    $idName$
        iban
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-bankaccount-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office.bankaccount',
    $orderBy$
        iban
    $orderBy$,
    $updates$
        holder = new.holder,
        iban = new.iban,
        bic = new.bic
    $updates$);
--//

