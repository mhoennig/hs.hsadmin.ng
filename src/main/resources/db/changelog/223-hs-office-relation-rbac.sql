--liquibase formatted sql

-- ============================================================================
--changeset hs-office-relation-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_relation');
--//


-- ============================================================================
--changeset hs-office-relation-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeRelation', 'hs_office_relation');
--//


-- ============================================================================
--changeset hs-office-relation-rbac-ROLES-CREATION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates and updates the roles and their assignments for relation entities.
 */

create or replace function hsOfficeRelationRbacRolesTrigger()
    returns trigger
    language plpgsql
    strict as $$
declare
    hsOfficeRelationTenant  RbacRoleDescriptor;
    newAnchor                hs_office_person;
    newHolder                hs_office_person;
    oldContact                  hs_office_contact;
    newContact                  hs_office_contact;
begin
    call enterTriggerForObjectUuid(NEW.uuid);

    hsOfficeRelationTenant := hsOfficeRelationTenant(NEW);

    select * from hs_office_person as p where p.uuid = NEW.anchorUuid into newAnchor;
    select * from hs_office_person as p where p.uuid = NEW.holderUuid into newHolder;
    select * from hs_office_contact as c where c.uuid = NEW.contactUuid into newContact;

    if TG_OP = 'INSERT' then

        perform createRoleWithGrants(
                hsOfficeRelationOwner(NEW),
                permissions => array['DELETE'],
                incomingSuperRoles => array[
                    globalAdmin(),
                    hsOfficePersonAdmin(newAnchor)]
            );

        perform createRoleWithGrants(
                hsOfficeRelationAdmin(NEW),
                permissions => array['UPDATE'],
                incomingSuperRoles => array[hsOfficeRelationOwner(NEW)]
            );

        -- the tenant role for those related users who can view the data
        perform createRoleWithGrants(
                hsOfficeRelationTenant,
                permissions => array['SELECT'],
                incomingSuperRoles => array[
                    hsOfficeRelationAdmin(NEW),
                    hsOfficePersonAdmin(newAnchor),
                    hsOfficePersonAdmin(newHolder),
                    hsOfficeContactAdmin(newContact)],
                outgoingSubRoles => array[
                    hsOfficePersonTenant(newAnchor),
                    hsOfficePersonTenant(newHolder),
                    hsOfficeContactTenant(newContact)]
            );

        -- anchor and holder admin roles need each others tenant role
        -- to be able to see the joined relation
        -- TODO: this can probably be avoided through agent+guest roles
        call grantRoleToRole(hsOfficePersonTenant(newAnchor), hsOfficePersonAdmin(newHolder));
        call grantRoleToRole(hsOfficePersonTenant(newHolder), hsOfficePersonAdmin(newAnchor));
        call grantRoleToRoleIfNotNull(hsOfficePersonTenant(newHolder), hsOfficeContactAdmin(newContact));

    elsif TG_OP = 'UPDATE' then

        if OLD.contactUuid <> NEW.contactUuid then
            -- nothing but the contact can be updated,
            -- in other cases, a new relation needs to be created and the old updated

            select * from hs_office_contact as c where c.uuid = OLD.contactUuid into oldContact;

            call revokeRoleFromRole( hsOfficeRelationTenant, hsOfficeContactAdmin(oldContact) );
            call grantRoleToRole( hsOfficeRelationTenant, hsOfficeContactAdmin(newContact) );

            call revokeRoleFromRole( hsOfficeContactTenant(oldContact), hsOfficeRelationTenant );
            call grantRoleToRole( hsOfficeContactTenant(newContact), hsOfficeRelationTenant );
        end if;
    else
        raise exception 'invalid usage of TRIGGER';
    end if;

    call leaveTriggerForObjectUuid(NEW.uuid);
    return NEW;
end; $$;

/*
    An AFTER INSERT TRIGGER which creates the role structure for a new customer.
 */
create trigger createRbacRolesForHsOfficeRelation_Trigger
    after insert
    on hs_office_relation
    for each row
execute procedure hsOfficeRelationRbacRolesTrigger();

/*
    An AFTER UPDATE TRIGGER which updates the role structure of a customer.
 */
create trigger updateRbacRolesForHsOfficeRelation_Trigger
    after update
    on hs_office_relation
    for each row
execute procedure hsOfficeRelationRbacRolesTrigger();
--//


-- ============================================================================
--changeset hs-office-relation-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacIdentityViewFromProjection('hs_office_relation', $idName$
    (select idName from hs_office_person_iv p where p.uuid = target.anchorUuid)
    || '-with-' || target.type || '-' ||
    (select idName from hs_office_person_iv p where p.uuid = target.holderUuid)
    $idName$);
--//


-- ============================================================================
--changeset hs-office-relation-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_relation',
    '(select idName from hs_office_person_iv p where p.uuid = target.holderUuid)',
    $updates$
        contactUuid = new.contactUuid
    $updates$);
--//

-- TODO: exception if one tries to amend any other column


-- ============================================================================
--changeset hs-office-relation-rbac-NEW-RELATHIONSHIP:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
/*
    Creates a global permission for new-relation and assigns it to the hostsharing admins role.
 */
do language plpgsql $$
    declare
        addCustomerPermissions uuid[];
        globalObjectUuid       uuid;
        globalAdminRoleUuid    uuid ;
    begin
        call defineContext('granting global new-relation permission to global admin role', null, null, null);

        globalAdminRoleUuid := findRoleId(globalAdmin());
        globalObjectUuid := (select uuid from global);
        addCustomerPermissions := createPermissions(globalObjectUuid, array ['new-relation']);
        call grantPermissionsToRole(globalAdminRoleUuid, addCustomerPermissions);
    end;
$$;

/**
    Used by the trigger to prevent the add-customer to current user respectively assumed roles.
 */
create or replace function addHsOfficeRelationNotAllowedForCurrentSubjects()
    returns trigger
    language PLPGSQL
as $$
begin
    raise exception '[403] new-relation not permitted for %',
        array_to_string(currentSubjects(), ';', 'null');
end; $$;

/**
    Checks if the user or assumed roles are allowed to create a new customer.
 */
create trigger hs_office_relation_insert_trigger
    before insert
    on hs_office_relation
    for each row
    -- TODO.spec: who is allowed to create new relations
    when ( not hasAssumedRole() )
execute procedure addHsOfficeRelationNotAllowedForCurrentSubjects();
--//

