--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


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
--changeset hs-office-partner-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficePartner(
    NEW hs_office_partner
)
    language plpgsql as $$

declare
    newPartnerRel hs_office_relation;
    newPartnerDetails hs_office_partner_details;

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office_relation WHERE uuid = NEW.partnerRelUuid    INTO newPartnerRel;
    assert newPartnerRel.uuid is not null, format('newPartnerRel must not be null for NEW.partnerRelUuid = %s', NEW.partnerRelUuid);

    SELECT * FROM hs_office_partner_details WHERE uuid = NEW.detailsUuid    INTO newPartnerDetails;
    assert newPartnerDetails.uuid is not null, format('newPartnerDetails must not be null for NEW.detailsUuid = %s', NEW.detailsUuid);

    call grantPermissionToRole(createPermission(NEW.uuid, 'DELETE'), hsOfficeRelationAdmin(newPartnerRel));
    call grantPermissionToRole(createPermission(NEW.uuid, 'SELECT'), hsOfficeRelationTenant(newPartnerRel));
    call grantPermissionToRole(createPermission(NEW.uuid, 'UPDATE'), hsOfficeRelationAgent(newPartnerRel));
    call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'DELETE'), hsOfficeRelationAdmin(newPartnerRel));
    call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'SELECT'), hsOfficeRelationAgent(newPartnerRel));
    call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'UPDATE'), hsOfficeRelationAgent(newPartnerRel));

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_partner row.
 */

create or replace function insertTriggerForHsOfficePartner_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficePartner(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficePartner_tg
    after insert on hs_office_partner
    for each row
execute procedure insertTriggerForHsOfficePartner_tf();
--//


-- ============================================================================
--changeset hs-office-partner-rbac-update-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure updateRbacRulesForHsOfficePartner(
    OLD hs_office_partner,
    NEW hs_office_partner
)
    language plpgsql as $$

declare
    oldPartnerRel hs_office_relation;
    newPartnerRel hs_office_relation;
    oldPartnerDetails hs_office_partner_details;
    newPartnerDetails hs_office_partner_details;

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office_relation WHERE uuid = OLD.partnerRelUuid    INTO oldPartnerRel;
    assert oldPartnerRel.uuid is not null, format('oldPartnerRel must not be null for OLD.partnerRelUuid = %s', OLD.partnerRelUuid);

    SELECT * FROM hs_office_relation WHERE uuid = NEW.partnerRelUuid    INTO newPartnerRel;
    assert newPartnerRel.uuid is not null, format('newPartnerRel must not be null for NEW.partnerRelUuid = %s', NEW.partnerRelUuid);

    SELECT * FROM hs_office_partner_details WHERE uuid = OLD.detailsUuid    INTO oldPartnerDetails;
    assert oldPartnerDetails.uuid is not null, format('oldPartnerDetails must not be null for OLD.detailsUuid = %s', OLD.detailsUuid);

    SELECT * FROM hs_office_partner_details WHERE uuid = NEW.detailsUuid    INTO newPartnerDetails;
    assert newPartnerDetails.uuid is not null, format('newPartnerDetails must not be null for NEW.detailsUuid = %s', NEW.detailsUuid);


    if NEW.partnerRelUuid <> OLD.partnerRelUuid then

        call revokePermissionFromRole(getPermissionId(OLD.uuid, 'DELETE'), hsOfficeRelationAdmin(oldPartnerRel));
        call grantPermissionToRole(createPermission(NEW.uuid, 'DELETE'), hsOfficeRelationAdmin(newPartnerRel));

        call revokePermissionFromRole(getPermissionId(OLD.uuid, 'UPDATE'), hsOfficeRelationAgent(oldPartnerRel));
        call grantPermissionToRole(createPermission(NEW.uuid, 'UPDATE'), hsOfficeRelationAgent(newPartnerRel));

        call revokePermissionFromRole(getPermissionId(OLD.uuid, 'SELECT'), hsOfficeRelationTenant(oldPartnerRel));
        call grantPermissionToRole(createPermission(NEW.uuid, 'SELECT'), hsOfficeRelationTenant(newPartnerRel));

        call revokePermissionFromRole(getPermissionId(oldPartnerDetails.uuid, 'DELETE'), hsOfficeRelationAdmin(oldPartnerRel));
        call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'DELETE'), hsOfficeRelationAdmin(newPartnerRel));

        call revokePermissionFromRole(getPermissionId(oldPartnerDetails.uuid, 'UPDATE'), hsOfficeRelationAgent(oldPartnerRel));
        call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'UPDATE'), hsOfficeRelationAgent(newPartnerRel));

        call revokePermissionFromRole(getPermissionId(oldPartnerDetails.uuid, 'SELECT'), hsOfficeRelationAgent(oldPartnerRel));
        call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'SELECT'), hsOfficeRelationAgent(newPartnerRel));

    end if;

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to re-wire the grant structure for a new hs_office_partner row.
 */

create or replace function updateTriggerForHsOfficePartner_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call updateRbacRulesForHsOfficePartner(OLD, NEW);
    return NEW;
end; $$;

create trigger updateTriggerForHsOfficePartner_tg
    after update on hs_office_partner
    for each row
execute procedure updateTriggerForHsOfficePartner_tf();
--//


-- ============================================================================
--changeset hs-office-partner-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO hs_office_partner permissions for the related global rows.
 */
do language plpgsql $$
    declare
        row global;
        permissionUuid uuid;
        roleUuid uuid;
    begin
        call defineContext('create INSERT INTO hs_office_partner permissions for the related global rows');

        FOR row IN SELECT * FROM global
            LOOP
                roleUuid := findRoleId(globalAdmin());
                permissionUuid := createPermission(row.uuid, 'INSERT', 'hs_office_partner');
                call grantPermissionToRole(permissionUuid, roleUuid);
            END LOOP;
    END;
$$;

/**
    Adds hs_office_partner INSERT permission to specified role of new global rows.
*/
create or replace function hs_office_partner_global_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_partner'),
            globalAdmin());
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_hs_office_partner_global_insert_tg
    after insert on global
    for each row
execute procedure hs_office_partner_global_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_partner,
    where only global-admin has that permission.
*/
create or replace function hs_office_partner_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into hs_office_partner not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_partner_insert_permission_check_tg
    before insert on hs_office_partner
    for each row
    when ( not isGlobalAdmin() )
        execute procedure hs_office_partner_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset hs-office-partner-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

    call generateRbacIdentityViewFromQuery('hs_office_partner',
        $idName$
            SELECT partner.partnerNumber
            || ':' || (SELECT idName FROM hs_office_person_iv p WHERE p.uuid = partner.personUuid)
            || '-' || (SELECT idName FROM hs_office_contact_iv c WHERE c.uuid = partner.contactUuid)
            FROM hs_office_partner AS partner
        $idName$);
--//

-- ============================================================================
--changeset hs-office-partner-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_partner',
    $orderBy$
        SELECT partner.partnerNumber
            || ':' || (SELECT idName FROM hs_office_person_iv p WHERE p.uuid = partner.personUuid)
            || '-' || (SELECT idName FROM hs_office_contact_iv c WHERE c.uuid = partner.contactUuid)
            FROM hs_office_partner AS partner
    $orderBy$,
    $updates$
        partnerRelUuid = new.partnerRelUuid,
        personUuid = new.personUuid,
        contactUuid = new.contactUuid
    $updates$);
--//

