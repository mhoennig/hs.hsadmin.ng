--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-partner-details-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office_partner_details');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-partner-details-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsOfficePartnerDetails', 'hs_office_partner_details');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-partner-details-rbac-insert-trigger endDelimiter:--//
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
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
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
--changeset InsertTriggerGenerator:hs-office-partner-details-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO hs_office_partner_details permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO hs_office_partner_details permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office_partner_details'),
                        rbac.globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office_partner_details INSERT permission to specified role of new global rows.
*/
create or replace function rbac.new_hsof_partner_details_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office_partner_details'),
            rbac.globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_office_partner_details_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure rbac.new_hsof_partner_details_grants_insert_to_global_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs_office_partner_details-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
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
    -- check INSERT INSERT if rbac.global ADMIN
    if rbac.isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office_partner_details values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger hs_office_partner_details_insert_permission_check_tg
    before insert on hs_office_partner_details
    for each row
        execute procedure hs_office_partner_details_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-partner-details-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromQuery('hs_office_partner_details',
    $idName$
        SELECT partnerDetails.uuid as uuid, partner_iv.idName as idName
            FROM hs_office_partner_details AS partnerDetails
            JOIN hs_office_partner partner ON partner.detailsUuid = partnerDetails.uuid
            JOIN hs_office_partner_iv partner_iv ON partner_iv.uuid = partner.uuid
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-partner-details-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office_partner_details',
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

