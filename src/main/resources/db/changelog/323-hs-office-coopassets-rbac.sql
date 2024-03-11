--liquibase formatted sql

-- ============================================================================
--changeset hs-office-coopAssetsTransaction-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_coopAssetsTransaction');
--//


-- ============================================================================
--changeset hs-office-coopAssetsTransaction-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeCoopAssetsTransaction', 'hs_office_coopAssetsTransaction');
--//


-- ============================================================================
--changeset hs-office-coopAssetsTransaction-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates and updates the permissions for coopAssetsTransaction entities.
 */

create or replace function hsOfficeCoopAssetsTransactionRbacRolesTrigger()
    returns trigger
    language plpgsql
    strict as $$
declare
    newHsOfficeMembership      hs_office_membership;
begin
    call enterTriggerForObjectUuid(NEW.uuid);

    select * from hs_office_membership as p where p.uuid = NEW.membershipUuid into newHsOfficeMembership;

    if TG_OP = 'INSERT' then

        -- Each coopAssetsTransaction entity belong exactly to one membership entity
        -- and it makes little sense just to delegate coopAssetsTransaction roles.
        -- Therefore, we do not create coopAssetsTransaction roles at all,
        -- but instead just assign extra permissions to existing membership-roles.

        -- coopassetstransactions cannot be edited nor deleted, just created+viewed
        call grantPermissionsToRole(
                getRoleId(hsOfficeMembershipTenant(newHsOfficeMembership)),
                createPermissions(NEW.uuid, array ['SELECT'])
            );

    else
        raise exception 'invalid usage of TRIGGER';
    end if;

    call leaveTriggerForObjectUuid(NEW.uuid);
    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */
create trigger createRbacRolesForHsOfficeCoopAssetsTransaction_Trigger
    after insert
    on hs_office_coopAssetsTransaction
    for each row
execute procedure hsOfficeCoopAssetsTransactionRbacRolesTrigger();
--//


-- ============================================================================
--changeset hs-office-coopAssetsTransaction-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityViewFromProjection('hs_office_coopAssetsTransaction', 'target.reference');
--//


-- ============================================================================
--changeset hs-office-coopAssetsTransaction-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_coopAssetsTransaction', orderby => 'target.reference');
--//


-- ============================================================================
--changeset hs-office-coopAssetsTransaction-rbac-NEW-CoopAssetsTransaction:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-coopAssetsTransaction and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions uuid[];
        globalObjectUuid       uuid;
        globalAdminRoleUuid    uuid ;
    begin
        call defineContext('granting global new-coopAssetsTransaction permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-coopassetstransaction']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addHsOfficeCoopAssetsTransactionNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-coopassetstransaction not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new customer.
 */
create trigger hs_office_coopAssetsTransaction_insert_trigger
    before insert
    on hs_office_coopAssetsTransaction
    for each row
    when ( not hasAssumedRole() )
execute procedure addHsOfficeCoopAssetsTransactionNotAllowedForCurrentSubjects();
--//

