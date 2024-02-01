--liquibase formatted sql

-- ============================================================================
--changeset hs-office-partner-details-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_partner_details');
--//





-- ============================================================================
--changeset hs-office-partner-details-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('hs_office_partner_details', $idName$
    (select idName || '-details' from hs_office_partner_iv partner_iv
        join hs_office_partner partner on (partner_iv.uuid = partner.uuid)
        where partner.detailsUuid = target.uuid)
    $idName$);
--//


-- ============================================================================
--changeset hs-office-partner-details-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_partner_details',
    'target.uuid', -- no specific order required
    $updates$
        registrationOffice = new.registrationOffice,
        registrationNumber = new.registrationNumber,
        birthPlace         = new.birthPlace,
        birthName          = new.birthName,
        birthday           = new.birthday,
        dateOfDeath        = new.dateOfDeath
    $updates$);
--//


-- ============================================================================
--changeset hs-office-partner-details-rbac-NEW-CONTACT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-partner-details and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions uuid[];
        globalObjectUuid       uuid;
        globalAdminRoleUuid    uuid ;
    begin
        call defineContext('granting global new-partner-details permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-partner-details']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

-- TODO.refa: the code below could be moved to a generator, maybe even the code above.
--  Additionally, the code below is not neccesary for all entities, specifiy when it is!

/**
    Used by the trigger to prevent the add-partner-details to current user respectively assumed roles.
 */
create or replace function addHsOfficePartnerDetailsNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-partner-details not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create new partner-details.
 */
create trigger hs_office_partner_details_insert_trigger
    before insert
    on hs_office_partner_details
    for each row
    when ( not hasAssumedRole() )
execute procedure addHsOfficePartnerDetailsNotAllowedForCurrentSubjects();
--//

