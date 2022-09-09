--liquibase formatted sql

-- ============================================================================
--changeset hs-admin-partner-rbac-CREATE-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the related RbacObject through a BEFORE INSERT TRIGGER.
 */
create trigger createRbacObjectForHsAdminPartner_Trigger
    before insert
    on hs_admin_partner
    for each row
execute procedure createRbacObject();
--//

-- ============================================================================
--changeset hs-admin-partner-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function hsAdminPartnerOwner(partner hs_admin_partner)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_admin_partner', partner.uuid, 'owner');
end; $$;

create or replace function hsAdminPartnerAdmin(partner hs_admin_partner)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_admin_partner', partner.uuid, 'admin');
end; $$;

create or replace function hsAdminPartnerTenant(partner hs_admin_partner)
    returns RbacRoleDescriptor
    language plpgsql
    strict as $$
begin
    return roleDescriptor('hs_admin_partner', partner.uuid, 'tenant');
end; $$;
--//


-- ============================================================================
--changeset hs-admin-partner-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles and their assignments for a new partner for the AFTER INSERT TRIGGER.
 */

create or replace function createRbacRolesForHsAdminContact()
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

    -- the owner role with full access for the global admins
    ownerRole = createRole(
            hsAdminPartnerOwner(NEW),
            grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['*']),
            beneathRole(globalAdmin())
        );

    -- the admin role with full access for the global admins
    adminRole = createRole(
            hsAdminPartnerAdmin(NEW),
            grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['edit']),
            beneathRole(globalAdmin())
        );

    -- the tenant role for those related users who can view the data
    perform createRole(
            hsAdminPartnerTenant(NEW),
            grantingPermissions(forObjectUuid => NEW.uuid, permitOps => array ['view']),
            beneathRole(ownerRole)
        );

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */

create trigger createRbacRolesForHsAdminContact_Trigger
    after insert
    on hs_admin_partner
    for each row
execute procedure createRbacRolesForHsAdminContact();
--//


-- ============================================================================
--changeset hs-admin-partner-rbac-ROLES-REMOVAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Deletes the roles and their assignments of a deleted partner for the BEFORE DELETE TRIGGER.
 */
create or replace function deleteRbacRulesForHsAdminContact()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP = 'DELETE' then
        call deleteRole(findRoleId(hsAdminPartnerOwner(OLD)));
        call deleteRole(findRoleId(hsAdminPartnerTenant(OLD)));
    else
        raise exception 'invalid usage of TRIGGER BEFORE DELETE';
    end if;
    return old;
end; $$;

/*
    An BEFORE DELETE TRIGGER which deletes the role structure of a partner.
 */
create trigger deleteRbacRulesForTestContact_Trigger
    before delete
    on hs_admin_partner
    for each row
execute procedure deleteRbacRulesForHsAdminContact();
--//

-- ============================================================================
--changeset hs-admin-partner-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a view to the partner main table which maps the identifying name
    (in this case, the prefix) to the objectUuid.
 */
create or replace view hs_admin_partner_iv as
select target.uuid,
       cleanIdentifier(
                       (select idName from hs_admin_person_iv person where person.uuid = target.personuuid)
                       || '-' ||
                       (select idName from hs_admin_contact_iv contact where contact.uuid = target.contactuuid)
           )
           as idName
    from hs_admin_partner as target;
-- TODO.spec: Is it ok that everybody has access to this information?
grant all privileges on hs_admin_partner_iv to restricted;

/*
    Returns the objectUuid for a given identifying name (in this case the prefix).
 */
create or replace function hs_admin_partnerUuidByIdName(idName varchar)
    returns uuid
    language sql
    strict as $$
select uuid from hs_admin_partner_iv iv where iv.idName = hs_admin_partnerUuidByIdName.idName;
$$;

/*
    Returns the identifying name for a given objectUuid (in this case the label).
 */
create or replace function hs_admin_partnerIdNameByUuid(uuid uuid)
    returns varchar
    language sql
    strict as $$
select idName from hs_admin_partner_iv iv where iv.uuid = hs_admin_partnerIdNameByUuid.uuid;
$$;
--//


-- ============================================================================
--changeset hs-admin-partner-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a view to the partner main table with row-level limitation
    based on the 'view' permission of the current user or assumed roles.
 */
set session session authorization default;
drop view if exists hs_admin_partner_rv;
create or replace view hs_admin_partner_rv as
select target.*
    from hs_admin_partner as target
    where target.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('view', 'hs_admin_partner', currentSubjectsUuids()));
grant all privileges on hs_admin_partner_rv to restricted;
--//


-- ============================================================================
--changeset hs-admin-partner-rbac-INSTEAD-OF-INSERT-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of insert trigger function for hs_admin_partner_rv.
 */
create or replace function insertHsAdminContact()
    returns trigger
    language plpgsql as $$
declare
    newUser hs_admin_partner;
begin
    insert
        into hs_admin_partner
        values (new.*)
        returning * into newUser;
    return newUser;
end;
$$;

/*
    Creates an instead of insert trigger for the hs_admin_partner_rv view.
 */
create trigger insertHsAdminContact_Trigger
    instead of insert
    on hs_admin_partner_rv
    for each row
execute function insertHsAdminContact();
--//

-- ============================================================================
--changeset hs-admin-partner-rbac-INSTEAD-OF-DELETE-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Instead of delete trigger function for hs_admin_partner_rv.
 */
create or replace function deleteHsAdminContact()
    returns trigger
    language plpgsql as $$
begin
    if true or hasGlobalRoleGranted(currentUserUuid()) or
       old.uuid in (select queryAccessibleObjectUuidsOfSubjectIds('delete', 'hs_admin_partner', currentSubjectsUuids())) then
        delete from hs_admin_partner c where c.uuid = old.uuid;
        return old;
    end if;
    raise exception '[403] User % not allowed to delete partner uuid %', currentUser(), old.uuid;
end; $$;

/*
    Creates an instead of delete trigger for the hs_admin_partner_rv view.
 */
create trigger deleteHsAdminContact_Trigger
    instead of delete
    on hs_admin_partner_rv
    for each row
execute function deleteHsAdminContact();
--/

-- ============================================================================
--changeset hs-admin-partner-rbac-NEW-CONTACT:1 endDelimiter:--//
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
create or replace function addHsAdminContactNotAllowedForCurrentSubjects()
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
create trigger hs_admin_partner_insert_trigger
    before insert
    on hs_admin_partner
    for each row
    -- TODO.spec: who is allowed to create new partners
    when ( not hasAssumedRole() )
execute procedure addHsAdminContactNotAllowedForCurrentSubjects();
--//

