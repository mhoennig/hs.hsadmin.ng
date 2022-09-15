--liquibase formatted sql

-- ============================================================================
--changeset hs-office-partner-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_partner');
--//


-- ============================================================================
--changeset hs-office-partner-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function hsOfficePartnerOwner(partner hs_office_partner)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_office_partner', partner.uuid, 'owner');
end; $$;

create or replace function hsOfficePartnerAdmin(partner hs_office_partner)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_office_partner', partner.uuid, 'admin');
end; $$;

create or replace function hsOfficePartnerTenant(partner hs_office_partner)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_office_partner', partner.uuid, 'tenant');
end; $$;
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
    hsOfficePartnerTenant RbacRoleDescriptor;
    ownerRole             uuid;
    adminRole             uuid;
    oldPerson             hs_office_person;
    newPerson             hs_office_person;
    oldContact            hs_office_contact;
    newContact            hs_office_contact;
begin

    hsOfficePartnerTenant := hsOfficePartnerTenant(NEW);

    select * from hs_office_person as p where p.uuid = NEW.personUuid into newPerson;
    select * from hs_office_contact as c where c.uuid = NEW.contactUuid into newContact;

    if TG_OP = 'INSERT' then

        -- the owner role with full access for the global admins
        ownerRole = createRole(
                hsOfficePartnerOwner(NEW),
                grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
                beneathRole(globalAdmin())
            );

        -- the admin role with full access for the global admins
        adminRole = createRole(
                hsOfficePartnerAdmin(NEW),
                grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit']),
                beneathRole(ownerRole)
            );

        -- the tenant role for those related users who can view the data
        perform createRole(
                hsOfficePartnerTenant,
                grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view']),
                beneathRoles(array[hsOfficePartnerAdmin(NEW), hsOfficePersonAdmin(newPerson), hsOfficeContactAdmin(newContact)]),
                withSubRoles(array[hsOfficePersonTenant(newPerson), hsOfficeContactTenant(newContact)])
            );

    elsif TG_OP = 'UPDATE' then

        if OLD.personUuid <> NEW.personUuid then
            select * from hs_office_person as p where p.uuid = OLD.personUuid into oldPerson;

            call revokeRoleFromRole( hsOfficePartnerTenant, hsOfficePersonAdmin(oldPerson) );
            call grantRoleToRole( hsOfficePartnerTenant, hsOfficePersonAdmin(newPerson) );

            call revokeRoleFromRole( hsOfficePersonTenant(oldPerson), hsOfficePartnerTenant );
            call grantRoleToRole( hsOfficePersonTenant(newPerson), hsOfficePartnerTenant );
        end if;

        if OLD.contactUuid <> NEW.contactUuid then
            select * from hs_office_contact as c where c.uuid = OLD.contactUuid into oldContact;

            call revokeRoleFromRole( hsOfficePartnerTenant, hsOfficeContactAdmin(oldContact) );
            call grantRoleToRole( hsOfficePartnerTenant, hsOfficeContactAdmin(newContact) );

            call revokeRoleFromRole( hsOfficeContactTenant(oldContact), hsOfficePartnerTenant );
            call grantRoleToRole( hsOfficeContactTenant(newContact), hsOfficePartnerTenant );
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

/*
    Creates a view to the partner main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
create or replace view hs_office_partner_iv as
select target.uuid,
       cleanIdentifier(
                       (select idName from hs_office_person_iv p where p.uuid = target.personuuid)
                       || '-' ||
                       (select idName from hs_office_contact_iv c where c.uuid = target.contactuuid)
           )
           as idName
    from hs_office_partner as target;
-- TODO.spec: Is it ok that everybody has access to this information?
grant all privileges on hs_office_partner_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case the prefix).
 */
create or replace function hs_office_partnerUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from hs_office_partner_iv iv where iv.idName = hs_office_partnerUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the label).
 */
create or replace function hs_office_partnerIdNameByUuid(uuid uuid)
    returns varchar
    language sql
    strict as $$
select idName from hs_office_partner_iv iv where iv.uuid = hs_office_partnerIdNameByUuid.uuid;
$$;
--//


-- ============================================================================
--changeset hs-office-partner-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the partner main table with row-level limitation
    based on the 'view' permission of the current user or assumed roles.
 */
set session session authorization default;
drop view if exists hs_office_partner_rv;
create or replace view hs_office_partner_rv as
select target.*
    from hs_office_partner as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'hs_office_partner', currentSubjectsUuids()));
grant all privileges on hs_office_partner_rv to restricted;
--//


-- ============================================================================
--changeset hs-office-partner-rbac-INSTEAD-OF-INSERT-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of insert trigger function for hs_office_partner_rv.
 */
create or replace function insertHsOfficePartner()
    returns trigger
    language plpgsql as $$
declare
    newUser hs_office_partner;
begin
    insert
        into hs_office_partner
        values (new.*)
        returning * into newUser;
    return newUser;
end;
$$;

/*
    Creates an instead of insert trigger for the hs_office_partner_rv view.
 */
create trigger insertHsOfficePartner_Trigger
    instead of insert
    on hs_office_partner_rv
    for each row
execute function insertHsOfficePartner();
--//


-- ============================================================================
--changeset hs-office-partner-rbac-INSTEAD-OF-DELETE-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of delete trigger function for hs_office_partner_rv.

    Checks if the current subject (user / assumed role) has the permission to delete the row.
 */
create or replace function deleteHsOfficePartner()
    returns trigger
    language plpgsql as $$
begin
    if old.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('delete', 'hs_office_partner', currentSubjectsUuids())) then
        delete from hs_office_partner p where p.uuid = old.uuid;
        return old;
    end if;
    raise exception '[403] Subject % is not allowed to delete partner uuid %', currentSubjectsUuids(), old.uuid;
end; $$;

/*
    Creates an instead of delete trigger for the hs_office_partner_rv view.
 */
create trigger deleteHsOfficePartner_Trigger
    instead of delete
    on hs_office_partner_rv
    for each row
execute function deleteHsOfficePartner();
--/


-- ============================================================================
--changeset hs-office-partner-rbac-INSTEAD-OF-UPDATE-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of update trigger function for hs_office_partner_rv.

    Checks if the current subject (user / assumed role) has the permission to update the row.
 */
create or replace function updateHsOfficePartner()
    returns trigger
    language plpgsql as $$
begin
    if old.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('edit', 'hs_office_partner', currentSubjectsUuids())) then
        update hs_office_partner
            set personUuid = new.personUuid,
                contactUuid = new.contactUuid,
                registrationOffice = new.registrationOffice,
                registrationNumber = new.registrationNumber,
                birthday = new.birthday,
                birthName = new.birthName,
                dateOfDeath = new.dateOfDeath
            where uuid = old.uuid;
        return old;
    end if;
    raise exception '[403] Subject % is not allowed to update partner uuid %', currentSubjectsUuids(), old.uuid;
end; $$;

/*
    Creates an instead of delete trigger for the hs_office_partner_rv view.
 */
create trigger updateHsOfficePartner_Trigger
    instead of update
    on hs_office_partner_rv
    for each row
execute function updateHsOfficePartner();
--/


-- ============================================================================
--changeset hs-office-partner-rbac-NEW-CONTACT:1 endDelimiter:--//
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

