--liquibase formatted sql

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
--changeset hs-office-bankaccount-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles and their assignments for a new bankaccount for the AFTER INSERT TRIGGER.
 */

create or replace function createRbacRolesForHsOfficeBankAccount()
    returns trigger
    language plpgsql
    strict as $$
declare
    ownerRole uuid;
    adminRole uuid;
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of TRIGGER AFTER INSERT';
    end if;

    -- the owner role with full access for the creator assigned to the current user
    ownerRole := createRole(
        hsOfficeBankAccountOwner(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['delete']),
        beneathRole(globalAdmin()),
        withoutSubRoles(),
        withUser(currentUser()), -- TODO.spec: Who is owner of a new bankaccount?
        grantedByRole(globalAdmin())
        );

    -- the admin role for those related users who can view the data and related records
    adminRole := createRole(
            hsOfficeBankAccountAdmin(NEW),
            -- Where bankaccounts can be created, assigned, re-assigned and deleted, they cannot be updated.
            -- Thus SQL UPDATE and 'edit' permission are being implemented.
            withoutPermissions(),
            beneathRole(ownerRole)
        );

    -- TODO.spec: assumption can not be updated

    -- the tenant role for those related users who can view the data
    perform createRole(
        hsOfficeBankAccountTenant(NEW),
        grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view']),
        beneathRole(adminRole)
        );

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */

create trigger createRbacRolesForHsOfficeBankAccount_Trigger
    after insert
    on hs_office_bankaccount
    for each row
execute procedure createRbacRolesForHsOfficeBankAccount();
--//


-- ============================================================================
--changeset hs-office-bankaccount-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityView('hs_office_bankaccount', $idName$
    target.holder
    $idName$);
--//


-- ============================================================================
--changeset hs-office-bankaccount-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_bankaccount', 'target.holder',
    $updates$
        holder = new.holder,
        iban = new.iban,
        bic = new.bic
    $updates$);
--/


-- ============================================================================
--changeset hs-office-bankaccount-rbac-NEW-BANKACCOUNT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-bankaccount and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions  uuid[];
        globalObjectUuid        uuid;
        globalAdminRoleUuid         uuid ;
    begin
        call defineContext('granting global new-bankaccount permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-bankaccount']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addHsOfficeBankAccountNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-bankaccount not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new customer.
 */
create trigger hs_office_bankaccount_insert_trigger
    before insert
    on hs_office_bankaccount
    for each row
    -- TODO.spec: who is allowed to create new bankaccounts
    when ( not hasAssumedRole() )
execute procedure addHsOfficeBankAccountNotAllowedForCurrentSubjects();
--//

