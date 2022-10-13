--liquibase formatted sql

-- ============================================================================
--changeset hs-office-sepaMandate-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_sepaMandate');
--//


-- ============================================================================
--changeset hs-office-sepaMandate-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeSepaMandate', 'hs_office_sepaMandate');
--//


-- ============================================================================
--changeset hs-office-sepaMandate-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates and updates the roles and their assignments for sepaMandate entities.
 */

create or replace function hsOfficeSepaMandateRbacRolesTrigger()
    returns trigger
    language plpgsql
    strict as $$
declare
    newHsOfficeDebitor      hs_office_debitor;
    newHsOfficeBankAccount  hs_office_bankAccount;
begin

    select * from hs_office_debitor as p where p.uuid = NEW.debitorUuid into newHsOfficeDebitor;
    select * from hs_office_bankAccount as c where c.uuid = NEW.bankAccountUuid into newHsOfficeBankAccount;

    if TG_OP = 'INSERT' then


        -- === ATTENTION: code generated from related Mermaid flowchart: ===


        perform createRoleWithGrants(
                hsOfficeSepaMandateOwner(NEW),
                permissions => array['*'],
                incomingSuperRoles => array[globalAdmin()]
            );

        perform createRoleWithGrants(
                hsOfficeSepaMandateAdmin(NEW),
                permissions => array['edit'],
                incomingSuperRoles => array[hsOfficeSepaMandateOwner(NEW)],
                outgoingSubRoles => array[hsOfficeBankAccountTenant(newHsOfficeBankAccount)]
            );

        perform createRoleWithGrants(
                hsOfficeSepaMandateAgent(NEW),
                incomingSuperRoles => array[hsOfficeSepaMandateAdmin(NEW), hsOfficeDebitorAdmin(newHsOfficeDebitor), hsOfficeBankAccountAdmin(newHsOfficeBankAccount)],
                outgoingSubRoles => array[hsOfficeDebitorTenant(newHsOfficeDebitor)]
            );

        perform createRoleWithGrants(
                hsOfficeSepaMandateTenant(NEW),
                incomingSuperRoles => array[hsOfficeSepaMandateAgent(NEW)],
                outgoingSubRoles => array[hsOfficeDebitorGuest(newHsOfficeDebitor), hsOfficeBankAccountGuest(newHsOfficeBankAccount)]
            );

        perform createRoleWithGrants(
                hsOfficeSepaMandateGuest(NEW),
                permissions => array['view'],
                incomingSuperRoles => array[hsOfficeSepaMandateTenant(NEW)]
            );

        -- === END of code generated from Mermaid flowchart. ===


    else
        raise exception 'invalid usage of TRIGGER';
    end if;

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */
create trigger createRbacRolesForHsOfficeSepaMandate_Trigger
    after insert
    on hs_office_sepaMandate
    for each row
execute procedure hsOfficeSepaMandateRbacRolesTrigger();
--//


-- ============================================================================
--changeset hs-office-sepaMandate-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('hs_office_sepaMandate', idNameExpression => 'target.reference');
--//


-- ============================================================================
--changeset hs-office-sepaMandate-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_sepaMandate',
    orderby => 'target.reference',
    columnUpdates => $updates$
        validity = new.validity
    $updates$);
--//


-- ============================================================================
--changeset hs-office-sepaMandate-rbac-NEW-SepaMandate:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-sepaMandate and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions uuid[];
        globalObjectUuid       uuid;
        globalAdminRoleUuid    uuid ;
    begin
        call defineContext('granting global new-sepaMandate permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-sepamandate']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addHsOfficeSepaMandateNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-sepaMandate not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new customer.
 */
create trigger hs_office_sepaMandate_insert_trigger
    before insert
    on hs_office_sepaMandate
    for each row
    -- TODO.spec: who is allowed to create new sepaMandates
    when ( not hasAssumedRole() )
execute procedure addHsOfficeSepaMandateNotAllowedForCurrentSubjects();
--//

