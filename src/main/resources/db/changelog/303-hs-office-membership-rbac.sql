--liquibase formatted sql

-- ============================================================================
--changeset hs-office-membership-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_membership');
--//


-- ============================================================================
--changeset hs-office-membership-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeMembership', 'hs_office_membership');
--//


-- ============================================================================
--changeset hs-office-membership-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates and updates the roles and their assignments for membership entities.
 */

create or replace function hsOfficeMembershipRbacRolesTrigger()
    returns trigger
    language plpgsql
    strict as $$
declare
    newHsOfficePartner  hs_office_partner;
    newHsOfficeDebitor  hs_office_debitor;
begin

    select * from hs_office_partner as p where p.uuid = NEW.partnerUuid into newHsOfficePartner;
    select * from hs_office_debitor as c where c.uuid = NEW.mainDebitorUuid into newHsOfficeDebitor;

    if TG_OP = 'INSERT' then

        -- === ATTENTION: code generated from related Mermaid flowchart: ===

        perform createRoleWithGrants(
                hsOfficeMembershipOwner(NEW),
                permissions => array['*'],
                incomingSuperRoles => array[globalAdmin()]
            );

        perform createRoleWithGrants(
                hsOfficeMembershipAdmin(NEW),
                permissions => array['edit'],
                incomingSuperRoles => array[hsOfficeMembershipOwner(NEW)]
            );

        perform createRoleWithGrants(
                hsOfficeMembershipAgent(NEW),
                incomingSuperRoles => array[hsOfficeMembershipAdmin(NEW), hsOfficePartnerAdmin(newHsOfficePartner), hsOfficeDebitorAdmin(newHsOfficeDebitor)],
                outgoingSubRoles => array[hsOfficePartnerTenant(newHsOfficePartner), hsOfficeDebitorTenant(newHsOfficeDebitor)]
            );

        perform createRoleWithGrants(
                hsOfficeMembershipTenant(NEW),
                incomingSuperRoles => array[hsOfficeMembershipAgent(NEW), hsOfficePartnerAgent(newHsOfficePartner), hsOfficeDebitorAgent(newHsOfficeDebitor)],
                outgoingSubRoles => array[hsOfficePartnerGuest(newHsOfficePartner), hsOfficeDebitorGuest(newHsOfficeDebitor)]
            );

        perform createRoleWithGrants(
                hsOfficeMembershipGuest(NEW),
                permissions => array['view'],
                incomingSuperRoles => array[hsOfficeMembershipTenant(NEW), hsOfficePartnerTenant(newHsOfficePartner), hsOfficeDebitorTenant(newHsOfficeDebitor)]
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
create trigger createRbacRolesForHsOfficeMembership_Trigger
    after insert
    on hs_office_membership
    for each row
execute procedure hsOfficeMembershipRbacRolesTrigger();
--//


-- ============================================================================
--changeset hs-office-membership-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('hs_office_membership', idNameExpression => $idName$
    target.memberNumber ||
    ':' || (select split_part(idName, ':', 2) from hs_office_partner_iv p where p.uuid = target.partnerUuid)
    $idName$);
--//


-- ============================================================================
--changeset hs-office-membership-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_membership',
    orderby => 'target.memberNumber',
    columnUpdates => $updates$
        validity = new.validity,
        reasonForTermination = new.reasonForTermination,
        membershipFeeBillable = new.membershipFeeBillable
    $updates$);
--//


-- ============================================================================
--changeset hs-office-membership-rbac-NEW-Membership:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-membership and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions uuid[];
        globalObjectUuid       uuid;
        globalAdminRoleUuid    uuid ;
    begin
        call defineContext('granting global new-membership permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-membership']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addHsOfficeMembershipNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-membership not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new customer.
 */
create trigger hs_office_membership_insert_trigger
    before insert
    on hs_office_membership
    for each row
    -- TODO.spec: who is allowed to create new memberships
    when ( not hasAssumedRole() )
execute procedure addHsOfficeMembershipNotAllowedForCurrentSubjects();
--//

