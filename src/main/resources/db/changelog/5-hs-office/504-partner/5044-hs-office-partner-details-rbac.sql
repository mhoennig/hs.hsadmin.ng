--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-office-partner-details-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_partner_details');
--//


-- ============================================================================
--changeset hs-office-partner-details-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficePartnerDetails', 'hs_office_partner_details');
--//


-- ============================================================================
--changeset hs-office-partner-details-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficePartnerDetails(
    NEW hs_office_partner_details
)
    language plpgsql as $$

declare

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_partner_details row.
 */

create or replace function insertTriggerForHsOfficePartnerDetails_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficePartnerDetails(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficePartnerDetails_tg
    after insert on hs_office_partner_details
    for each row
execute procedure insertTriggerForHsOfficePartnerDetails_tf();
--//


-- ============================================================================
--changeset hs-office-partner-details-rbac-GRANTING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to global ----------------------------

/*
    Grants INSERT INTO hs_office_partner_details permissions to specified role of pre-existing global rows.
 */
do language plpgsql $$
    declare
        row global;
    begin
        call defineContext('create INSERT INTO hs_office_partner_details permissions for pre-exising global rows');

        FOR row IN SELECT * FROM global
            -- unconditional for all rows in that table
            LOOP
                call grantPermissionToRole(
                        createPermission(row.uuid, 'INSERT', 'hs_office_partner_details'),
                        globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office_partner_details INSERT permission to specified role of new global rows.
*/
create or replace function new_hs_office_partner_details_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_partner_details'),
            globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_office_partner_details_grants_insert_to_global_tg
    after insert on global
    for each row
execute procedure new_hs_office_partner_details_grants_insert_to_global_tf();


-- ============================================================================
--changeset hs_office_partner_details-rbac-CHECKING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office_partner_details.
*/
create or replace function hs_office_partner_details_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT INSERT if global ADMIN
    if isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office_partner_details not allowed for current subjects % (%)',
            currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_partner_details_insert_permission_check_tg
    before insert on hs_office_partner_details
    for each row
        execute procedure hs_office_partner_details_insert_permission_check_tf();
--//


-- ============================================================================
--changeset hs-office-partner-details-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromQuery('hs_office_partner_details',
    $idName$
        SELECT partnerDetails.uuid as uuid, partner_iv.idName as idName
            FROM hs_office_partner_details AS partnerDetails
            JOIN hs_office_partner partner ON partner.detailsUuid = partnerDetails.uuid
            JOIN hs_office_partner_iv partner_iv ON partner_iv.uuid = partner.uuid
    $idName$);
--//


-- ============================================================================
--changeset hs-office-partner-details-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_partner_details',
    $orderBy$
        uuid
    $orderBy$,
    $updates$
        registrationOffice = new.registrationOffice,
        registrationNumber = new.registrationNumber,
        birthPlace = new.birthPlace,
        birthName = new.birthName,
        birthday = new.birthday,
        dateOfDeath = new.dateOfDeath
    $updates$);
--//

