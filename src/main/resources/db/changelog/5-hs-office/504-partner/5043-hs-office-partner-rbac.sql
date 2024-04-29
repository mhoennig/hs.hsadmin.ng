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

    call grantPermissionToRole(createPermission(NEW.uuid, 'DELETE'), hsOfficeRelationOWNER(newPartnerRel));
    call grantPermissionToRole(createPermission(NEW.uuid, 'SELECT'), hsOfficeRelationTENANT(newPartnerRel));
    call grantPermissionToRole(createPermission(NEW.uuid, 'UPDATE'), hsOfficeRelationADMIN(newPartnerRel));
    call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'DELETE'), hsOfficeRelationOWNER(newPartnerRel));
    call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'SELECT'), hsOfficeRelationAGENT(newPartnerRel));
    call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'UPDATE'), hsOfficeRelationAGENT(newPartnerRel));

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

        call revokePermissionFromRole(getPermissionId(OLD.uuid, 'DELETE'), hsOfficeRelationOWNER(oldPartnerRel));
        call grantPermissionToRole(createPermission(NEW.uuid, 'DELETE'), hsOfficeRelationOWNER(newPartnerRel));

        call revokePermissionFromRole(getPermissionId(OLD.uuid, 'UPDATE'), hsOfficeRelationADMIN(oldPartnerRel));
        call grantPermissionToRole(createPermission(NEW.uuid, 'UPDATE'), hsOfficeRelationADMIN(newPartnerRel));

        call revokePermissionFromRole(getPermissionId(OLD.uuid, 'SELECT'), hsOfficeRelationTENANT(oldPartnerRel));
        call grantPermissionToRole(createPermission(NEW.uuid, 'SELECT'), hsOfficeRelationTENANT(newPartnerRel));

        call revokePermissionFromRole(getPermissionId(oldPartnerDetails.uuid, 'DELETE'), hsOfficeRelationOWNER(oldPartnerRel));
        call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'DELETE'), hsOfficeRelationOWNER(newPartnerRel));

        call revokePermissionFromRole(getPermissionId(oldPartnerDetails.uuid, 'UPDATE'), hsOfficeRelationAGENT(oldPartnerRel));
        call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'UPDATE'), hsOfficeRelationAGENT(newPartnerRel));

        call revokePermissionFromRole(getPermissionId(oldPartnerDetails.uuid, 'SELECT'), hsOfficeRelationAGENT(oldPartnerRel));
        call grantPermissionToRole(createPermission(newPartnerDetails.uuid, 'SELECT'), hsOfficeRelationAGENT(newPartnerRel));

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
--changeset hs-office-partner-rbac-GRANTING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to global ----------------------------

/*
    Grants INSERT INTO hs_office_partner permissions to specified role of pre-existing global rows.
 */
do language plpgsql $$
    declare
        row global;
    begin
        call defineContext('create INSERT INTO hs_office_partner permissions for pre-exising global rows');

        FOR row IN SELECT * FROM global
            -- unconditional for all rows in that table
            LOOP
                call grantPermissionToRole(
                        createPermission(row.uuid, 'INSERT', 'hs_office_partner'),
                        globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office_partner INSERT permission to specified role of new global rows.
*/
create or replace function new_hs_office_partner_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_partner'),
            globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_office_partner_grants_insert_to_global_tg
    after insert on global
    for each row
execute procedure new_hs_office_partner_grants_insert_to_global_tf();


-- ============================================================================
--changeset hs_office_partner-rbac-CHECKING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office_partner.
*/
create or replace function hs_office_partner_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT INSERT if global ADMIN
    if isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office_partner not allowed for current subjects % (%)',
            currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_partner_insert_permission_check_tg
    before insert on hs_office_partner
    for each row
        execute procedure hs_office_partner_insert_permission_check_tf();
--//


-- ============================================================================
--changeset hs-office-partner-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_partner',
    $idName$
        'P-' || partnerNumber
    $idName$);
--//


-- ============================================================================
--changeset hs-office-partner-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_partner',
    $orderBy$
        'P-' || partnerNumber
    $orderBy$,
    $updates$
        partnerRelUuid = new.partnerRelUuid
    $updates$);
--//

