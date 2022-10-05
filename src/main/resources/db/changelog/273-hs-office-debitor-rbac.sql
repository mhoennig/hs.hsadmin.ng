--liquibase formatted sql

-- ============================================================================
--changeset hs-office-debitor-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_debitor');
--//


-- ============================================================================
--changeset hs-office-debitor-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeDebitor', 'hs_office_debitor');
--//


-- ============================================================================
--changeset hs-office-debitor-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates and updates the roles and their assignments for debitor entities.
 */

create or replace function hsOfficeDebitorRbacRolesTrigger()
    returns trigger
    language plpgsql
    strict as $$
declare
    hsOfficeDebitorTenant RbacRoleDescriptor;
    ownerRole             uuid;
    adminRole             uuid;
    oldPartner            hs_office_partner;
    newPartner            hs_office_partner;
    newPerson             hs_office_person;
    oldContact            hs_office_contact;
    newContact            hs_office_contact;
    newBankAccount        hs_office_bankaccount;
begin

    hsOfficeDebitorTenant := hsOfficeDebitorTenant(NEW);

    select * from hs_office_partner as p where p.uuid = NEW.partnerUuid into newPartner;
    select * from hs_office_person as p where p.uuid = newPartner.personUuid into newPerson;
    select * from hs_office_contact as c where c.uuid = NEW.billingContactUuid into newContact;
    select * from hs_office_bankaccount as b where b.uuid = NEW.refundBankAccountUuid into newBankAccount;
    if TG_OP = 'INSERT' then

        -- the owner role with full access for the global admins
        ownerRole = createRole(
                hsOfficeDebitorOwner(NEW),
                grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
                beneathRole(globalAdmin())
            );

        -- the admin role with full access for owner
        adminRole = createRole(
                hsOfficeDebitorAdmin(NEW),
                withoutPermissions(),
                beneathRoles(array [
                    hsOfficeDebitorOwner(NEW),
                    hsOfficePartnerAdmin(newPartner),
                    hsOfficePersonAdmin(newPerson),
                    hsOfficeContactAdmin(newContact),
                    hsOfficeBankAccountAdmin(newBankAccount)]),
                withSubRoles(array [
                    hsOfficePartnerTenant(newPartner),
                    hsOfficePersonTenant(newPerson),
                    hsOfficeContactTenant(newContact),
                    hsOfficeBankAccountTenant(newBankAccount)])
            );

        -- the tenant role for those related users who can view the data
        perform createRole(
                hsOfficeDebitorTenant,
                grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view']),
                beneathRole(hsOfficeDebitorAdmin(NEW))
            );

    elsif TG_OP = 'UPDATE' then

        if OLD.partnerUuid <> NEW.partnerUuid then
            select * from hs_office_partner as p where p.uuid = OLD.partnerUuid into oldPartner;

            call revokeRoleFromRole(hsOfficeDebitorAdmin(OLD), hsOfficePartnerAdmin(oldPartner));
            call grantRoleToRole(hsOfficeDebitorAdmin(NEW), hsOfficePartnerAdmin(newPartner));

            call revokeRoleFromRole(hsOfficePartnerTenant(oldPartner), hsOfficeDebitorAdmin(OLD));
            call grantRoleToRole(hsOfficePartnerTenant(newPartner), hsOfficeDebitorAdmin(NEW));

            -- TODO: What about the person of the partner? And what if the person of the partner changes?
        end if;

        if OLD.billingContactUuid <> NEW.billingContactUuid then
            select * from hs_office_contact as c where c.uuid = OLD.billingContactUuid into oldContact;

            call revokeRoleFromRole(hsOfficeDebitorAdmin(OLD), hsOfficeContactAdmin(oldContact));
            call grantRoleToRole(hsOfficeDebitorAdmin(NEW), hsOfficeContactAdmin(newContact));

            call revokeRoleFromRole(hsOfficeContactTenant(oldContact), hsOfficeDebitorAdmin(OLD));
            call grantRoleToRole(hsOfficeContactTenant(newContact), hsOfficeDebitorAdmin(NEW));
        end if;
    else
        raise exception 'invalid usage of TRIGGER';
    end if;

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new debitor.
 */
create trigger createRbacRolesForHsOfficeDebitor_Trigger
    after insert
    on hs_office_debitor
    for each row
execute procedure hsOfficeDebitorRbacRolesTrigger();

/*
    An AFTER UPDATE TRIGGER which updates the role structure of a debitor.
 */
create trigger updateRbacRolesForHsOfficeDebitor_Trigger
    after update
    on hs_office_debitor
    for each row
execute procedure hsOfficeDebitorRbacRolesTrigger();
--//


-- ============================================================================
--changeset hs-office-debitor-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('hs_office_debitor', $idName$
    '#' || debitorNumber || ':' ||
    (select idName from hs_office_partner_iv p where p.uuid = target.partnerUuid)
    $idName$);
--//


-- ============================================================================
--changeset hs-office-debitor-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_debitor',
                                'target.debitorNumber',
                                $updates$
        billingContactUuid = new.billingContactUuid,
        vatId = new.vatId,
        vatCountryCode = new.vatCountryCode,
        vatBusiness = new.vatBusiness
    $updates$);
--//

-- ============================================================================
--changeset hs-office-debitor-rbac-NEW-DEBITOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-debitor and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addDebitorPermissions uuid[];
        globalObjectUuid      uuid;
        globalAdminRoleUuid   uuid ;
    begin
        call defineContext('granting global new-debitor permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addDebitorPermissions := createPermissions(globalObjectUuid, array ['new-debitor']);
        call grantPermissionsToRole(globalAdminRoleUuid, addDebitorPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-debitor to current user respectively assumed roles.
 */
create or replace function addHsOfficeDebitorNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-debitor not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new debitor.
 */
create trigger hs_office_debitor_insert_trigger
    before insert
    on hs_office_debitor
    for each row
    -- TODO.spec: who is allowed to create new debitors
    when ( not hasAssumedRole() )
execute procedure addHsOfficeDebitorNotAllowedForCurrentSubjects();
--//

