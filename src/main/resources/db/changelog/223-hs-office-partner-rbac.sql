--liquibase formatted sql

-- ============================================================================
--changeset hs-office-partner-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_partner');
--//


-- ============================================================================
--changeset hs-office-partner-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficePartner', 'hs_office_partner');
--//


-- ============================================================================
--changeset hs-office-partner-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates and updates the roles and their assignments for partner entities.
 */

create or replace function hsOfficePartnerRbacRolesTrigger()
    returns trigger
    language plpgsql
    strict as $$
declare
    oldPerson             hs_office_person;
    newPerson             hs_office_person;
    oldContact            hs_office_contact;
    newContact            hs_office_contact;
begin

    select * from hs_office_person as p where p.uuid = NEW.personUuid into newPerson;
    select * from hs_office_contact as c where c.uuid = NEW.contactUuid into newContact;

    if TG_OP = 'INSERT' then

        -- === ATTENTION: code generated from related Mermaid flowchart: ===

        perform createRoleWithGrants(
                hsOfficePartnerOwner(NEW),
                permissions => array['*'],
                incomingSuperRoles => array[globalAdmin()]
            );

        perform createRoleWithGrants(
                hsOfficePartnerAdmin(NEW),
                permissions => array['edit'],
                incomingSuperRoles => array[
                    hsOfficePartnerOwner(NEW)],
                outgoingSubRoles => array[
                    hsOfficePersonTenant(newPerson),
                    hsOfficeContactTenant(newContact)]
            );

        perform createRoleWithGrants(
                hsOfficePartnerAgent(NEW),
                incomingSuperRoles => array[
                    hsOfficePartnerAdmin(NEW),
                    hsOfficePersonAdmin(newPerson),
                    hsOfficeContactAdmin(newContact)]
            );

        perform createRoleWithGrants(
                hsOfficePartnerTenant(NEW),
                incomingSuperRoles => array[
                    hsOfficePartnerAgent(NEW)],
                outgoingSubRoles => array[
                    hsOfficePersonGuest(newPerson),
                    hsOfficeContactGuest(newContact)]
            );

        perform createRoleWithGrants(
                hsOfficePartnerGuest(NEW),
                permissions => array['view'],
                incomingSuperRoles => array[hsOfficePartnerTenant(NEW)]
            );

        -- === END of code generated from Mermaid flowchart. ===

        -- Each partner-details entity belong exactly to one partner entity
        -- and it makes little sense just to delegate partner-details roles.
        -- Therefore, we did not model partner-details roles,
        -- but instead just assign extra permissions to existing partner-roles.

        --Attention: Cannot be in partner-details because of insert order (partner is not in database yet)

        call grantPermissionsToRole(
                getRoleId(hsOfficePartnerOwner(NEW), 'fail'),
                createPermissions(NEW.detailsUuid, array ['*'])
            );

        call grantPermissionsToRole(
                getRoleId(hsOfficePartnerAdmin(NEW), 'fail'),
                createPermissions(NEW.detailsUuid, array ['edit'])
            );

        call grantPermissionsToRole(
            -- Yes, here hsOfficePartnerAGENT is used, not hsOfficePartnerTENANT.
            -- Do NOT grant view permission on partner-details to hsOfficePartnerTENANT!
            -- Otherwise package-admins etc. would be able to read the data.
                getRoleId(hsOfficePartnerAgent(NEW), 'fail'),
                createPermissions(NEW.detailsUuid, array ['view'])
            );


    elsif TG_OP = 'UPDATE' then

        if OLD.personUuid <> NEW.personUuid then
            select * from hs_office_person as p where p.uuid = OLD.personUuid into oldPerson;

            call revokeRoleFromRole(hsOfficePersonTenant(oldPerson), hsOfficePartnerAdmin(OLD));
            call grantRoleToRole(hsOfficePersonTenant(newPerson), hsOfficePartnerAdmin(NEW));

            call revokeRoleFromRole(hsOfficePartnerAgent(OLD), hsOfficePersonAdmin(oldPerson));
            call grantRoleToRole(hsOfficePartnerAgent(NEW), hsOfficePersonAdmin(newPerson));

            call revokeRoleFromRole(hsOfficePersonGuest(oldPerson), hsOfficePartnerTenant(OLD));
            call grantRoleToRole(hsOfficePersonGuest(newPerson), hsOfficePartnerTenant(NEW));
        end if;

        if OLD.contactUuid <> NEW.contactUuid then
            select * from hs_office_contact as c where c.uuid = OLD.contactUuid into oldContact;

            call revokeRoleFromRole(hsOfficeContactTenant(oldContact), hsOfficePartnerAdmin(OLD));
            call grantRoleToRole(hsOfficeContactTenant(newContact), hsOfficePartnerAdmin(NEW));

            call revokeRoleFromRole(hsOfficePartnerAgent(OLD), hsOfficeContactAdmin(oldContact));
            call grantRoleToRole(hsOfficePartnerAgent(NEW), hsOfficeContactAdmin(newContact));

            call revokeRoleFromRole(hsOfficeContactGuest(oldContact), hsOfficePartnerTenant(OLD));
            call grantRoleToRole(hsOfficeContactGuest(newContact), hsOfficePartnerTenant(NEW));
        end if;
    else
        raise exception 'invalid usage of TRIGGER';
    end if;

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */
create trigger createRbacRolesForHsOfficePartner_Trigger
    after insert
    on hs_office_partner
    for each row
execute procedure hsOfficePartnerRbacRolesTrigger();

/*
    An AFTER UPDATE TRIGGER which updates the role structure of a customer.
 */
create trigger updateRbacRolesForHsOfficePartner_Trigger
    after update
    on hs_office_partner
    for each row
execute procedure hsOfficePartnerRbacRolesTrigger();
--//


-- ============================================================================
--changeset hs-office-partner-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('hs_office_partner', $idName$
    -- TODO: simplify by using just debitorNumberPrefix for the essential part
    debitorNumberPrefix || ':' ||
    (select idName from hs_office_person_iv p where p.uuid = target.personuuid)
    || '-' ||
    (select idName from hs_office_contact_iv c where c.uuid = target.contactuuid)
    $idName$);
--//


-- ============================================================================
--changeset hs-office-partner-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_partner',
    '(select idName from hs_office_person_iv p where p.uuid = target.personUuid)',
    $updates$
        debitorNumberPrefix = new.debitorNumberPrefix,
        personUuid = new.personUuid,
        contactUuid = new.contactUuid
    $updates$);
--//


-- ============================================================================
--changeset hs-office-partner-rbac-NEW-PARTNER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-partner and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions uuid[];
        globalObjectUuid       uuid;
        globalAdminRoleUuid    uuid ;
    begin
        call defineContext('granting global new-partner permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-partner']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addHsOfficePartnerNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-partner not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new customer.
 */
create trigger hs_office_partner_insert_trigger
    before insert
    on hs_office_partner
    for each row
    -- TODO.spec: who is allowed to create new partners
    when ( not hasAssumedRole() )
execute procedure addHsOfficePartnerNotAllowedForCurrentSubjects();
--//

