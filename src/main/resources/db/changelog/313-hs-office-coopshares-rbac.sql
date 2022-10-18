--liquibase formatted sql

-- ============================================================================
--changeset hs-office-coopSharesTransaction-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_coopSharesTransaction');
--//


-- ============================================================================
--changeset hs-office-coopSharesTransaction-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeCoopSharesTransaction', 'hs_office_coopSharesTransaction');
--//


-- ============================================================================
--changeset hs-office-coopSharesTransaction-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates and updates the permissions for coopSharesTransaction entities.
 */

create or replace function hsOfficeCoopSharesTransactionRbacRolesTrigger()
    returns trigger
    language plpgsql
    strict as $$
declare
    newHsOfficeMembership      hs_office_membership;
begin

    select * from hs_office_membership as p where p.uuid = NEW.membershipUuid into newHsOfficeMembership;

    if TG_OP = 'INSERT' then

        -- Each coopSharesTransaction entity belong exactly to one membership entity
        -- and it makes little sense just to delegate coopSharesTransaction roles.
        -- Therefore, we do not create coopSharesTransaction roles at all,
        -- but instead just assign extra permissions to existing membership-roles.

        -- coopsharestransactions cannot be edited nor deleted, just created+viewed
        call grantPermissionsToRole(
                getRoleId(hsOfficeMembershipTenant(newHsOfficeMembership), 'fail'),
                createPermissions(NEW.uuid, array ['view'])
            );

    else
        raise exception 'invalid usage of TRIGGER';
    end if;

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */
create trigger createRbacRolesForHsOfficeCoopSharesTransaction_Trigger
    after insert
    on hs_office_coopSharesTransaction
    for each row
execute procedure hsOfficeCoopSharesTransactionRbacRolesTrigger();
--//


-- ============================================================================
--changeset hs-office-coopSharesTransaction-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('hs_office_coopSharesTransaction', 
    idNameExpression => 'target.reference');
--//


-- ============================================================================
--changeset hs-office-coopSharesTransaction-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_coopSharesTransaction', orderby => 'target.reference');
--//


-- ============================================================================
--changeset hs-office-coopSharesTransaction-rbac-NEW-CoopSharesTransaction:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-coopSharesTransaction and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions uuid[];
        globalObjectUuid       uuid;
        globalAdminRoleUuid    uuid ;
    begin
        call defineContext('granting global new-coopSharesTransaction permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-coopsharestransaction']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addHsOfficeCoopSharesTransactionNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-coopsharestransaction not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new customer.
 */
create trigger hs_office_coopSharesTransaction_insert_trigger
    before insert
    on hs_office_coopSharesTransaction
    for each row
    when ( not hasAssumedRole() )
execute procedure addHsOfficeCoopSharesTransactionNotAllowedForCurrentSubjects();
--//

