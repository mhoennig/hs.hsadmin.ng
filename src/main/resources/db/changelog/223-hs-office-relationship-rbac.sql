--liquibase formatted sql

-- ============================================================================
--changeset hs-office-relationship-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_relationship');
--//


-- ============================================================================
--changeset hs-office-relationship-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeRelationship', 'hs_office_relationship');
--//


-- ============================================================================
--changeset hs-office-relationship-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates and updates the roles and their assignments for relationship entities.
 */

create or replace function hsOfficeRelationshipRbacRolesTrigger()
    returns trigger
    language plpgsql
    strict as $$
declare
    hsOfficeRelationshipTenant  RbacRoleDescriptor;
    newRelAnchor                hs_office_person;
    newRelHolder                hs_office_person;
    oldContact                  hs_office_contact;
    newContact                  hs_office_contact;
begin

    hsOfficeRelationshipTenant := hsOfficeRelationshipTenant(NEW);

    select * from hs_office_person as p where p.uuid = NEW.relAnchorUuid into newRelAnchor;
    select * from hs_office_person as p where p.uuid = NEW.relHolderUuid into newRelHolder;
    select * from hs_office_contact as c where c.uuid = NEW.contactUuid into newContact;

    if TG_OP = 'INSERT' then

        perform createRoleWithGrants(
                hsOfficeRelationshipOwner(NEW),
                permissions => array['*'],
                incomingSuperRoles => array[
                    globalAdmin(),
                    hsOfficePersonAdmin(newRelAnchor)]
            );

        perform createRoleWithGrants(
                hsOfficeRelationshipAdmin(NEW),
                permissions => array['edit'],
                incomingSuperRoles => array[hsOfficeRelationshipOwner(NEW)]
            );

        -- the tenant role for those related users who can view the data
        perform createRoleWithGrants(
                hsOfficeRelationshipTenant,
                permissions => array['view'],
                incomingSuperRoles => array[
                    hsOfficeRelationshipAdmin(NEW),
                    hsOfficePersonAdmin(newRelAnchor),
                    hsOfficePersonAdmin(newRelHolder),
                    hsOfficeContactAdmin(newContact)],
                outgoingSubRoles => array[
                    hsOfficePersonTenant(newRelAnchor),
                    hsOfficePersonTenant(newRelHolder),
                    hsOfficeContactTenant(newContact)]
            );

        -- anchor and holder admin roles need each others tenant role
        -- to be able to see the joined relationship
        -- TODO: this can probably be avoided through agent+guest roles
        call grantRoleToRole(hsOfficePersonTenant(newRelAnchor), hsOfficePersonAdmin(newRelHolder));
        call grantRoleToRole(hsOfficePersonTenant(newRelHolder), hsOfficePersonAdmin(newRelAnchor));
        call grantRoleToRoleIfNotNull(hsOfficePersonTenant(newRelHolder), hsOfficeContactAdmin(newContact));

    elsif TG_OP = 'UPDATE' then

        if OLD.contactUuid <> NEW.contactUuid then
            -- nothing but the contact can be updated,
            -- in other cases, a new relationship needs to be created and the old updated

            select * from hs_office_contact as c where c.uuid = OLD.contactUuid into oldContact;

            call revokeRoleFromRole( hsOfficeRelationshipTenant, hsOfficeContactAdmin(oldContact) );
            call grantRoleToRole( hsOfficeRelationshipTenant, hsOfficeContactAdmin(newContact) );

            call revokeRoleFromRole( hsOfficeContactTenant(oldContact), hsOfficeRelationshipTenant );
            call grantRoleToRole( hsOfficeContactTenant(newContact), hsOfficeRelationshipTenant );
        end if;
    else
        raise exception 'invalid usage of TRIGGER';
    end if;

    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */
create trigger createRbacRolesForHsOfficeRelationship_Trigger
    after insert
    on hs_office_relationship
    for each row
execute procedure hsOfficeRelationshipRbacRolesTrigger();

/*
    An AFTER UPDATE TRIGGER which updates the role structure of a customer.
 */
create trigger updateRbacRolesForHsOfficeRelationship_Trigger
    after update
    on hs_office_relationship
    for each row
execute procedure hsOfficeRelationshipRbacRolesTrigger();
--//


-- ============================================================================
--changeset hs-office-relationship-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityView('hs_office_relationship', $idName$
    (select idName from hs_office_person_iv p where p.uuid = target.relAnchorUuid)
    || '-with-' || target.relType || '-' ||
    (select idName from hs_office_person_iv p where p.uuid = target.relHolderUuid)
    $idName$);
--//


-- ============================================================================
--changeset hs-office-relationship-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_relationship',
    '(select idName from hs_office_person_iv p where p.uuid = target.relHolderUuid)',
    $updates$
        contactUuid = new.contactUuid
    $updates$);
--//

-- TODO: exception if one tries to amend any other column


-- ============================================================================
--changeset hs-office-relationship-rbac-NEW-RELATHIONSHIP:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-relationship and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions uuid[];
        globalObjectUuid       uuid;
        globalAdminRoleUuid    uuid ;
    begin
        call defineContext('granting global new-relationship permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-relationship']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addHsOfficeRelationshipNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-relationship not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new customer.
 */
create trigger hs_office_relationship_insert_trigger
    before insert
    on hs_office_relationship
    for each row
    -- TODO.spec: who is allowed to create new relationships
    when ( not hasAssumedRole() )
execute procedure addHsOfficeRelationshipNotAllowedForCurrentSubjects();
--//

