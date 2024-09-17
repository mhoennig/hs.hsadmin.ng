--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-membership-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office_membership');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-membership-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsOfficeMembership', 'hs_office_membership');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-membership-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficeMembership(
    NEW hs_office_membership
)
    language plpgsql as $$

declare
    newPartnerRel hs_office_relation;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT partnerRel.*
        FROM hs_office_partner AS partner
        JOIN hs_office_relation AS partnerRel ON partnerRel.uuid = partner.partnerRelUuid
        WHERE partner.uuid = NEW.partnerUuid
        INTO newPartnerRel;
    assert newPartnerRel.uuid is not null, format('newPartnerRel must not be null for NEW.partnerUuid = %s', NEW.partnerUuid);


    perform rbac.defineRoleWithGrants(
        hsOfficeMembershipOWNER(NEW),
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hsOfficeMembershipADMIN(NEW),
            permissions => array['DELETE', 'UPDATE'],
            incomingSuperRoles => array[
            	hsOfficeMembershipOWNER(NEW),
            	hsOfficeRelationADMIN(newPartnerRel)]
    );

    perform rbac.defineRoleWithGrants(
        hsOfficeMembershipAGENT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hsOfficeMembershipADMIN(NEW),
            	hsOfficeRelationAGENT(newPartnerRel)],
            outgoingSubRoles => array[hsOfficeRelationTENANT(newPartnerRel)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_membership row.
 */

create or replace function insertTriggerForHsOfficeMembership_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficeMembership(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficeMembership_tg
    after insert on hs_office_membership
    for each row
execute procedure insertTriggerForHsOfficeMembership_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-membership-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO hs_office_membership permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO hs_office_membership permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office_membership'),
                        rbac.globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office_membership INSERT permission to specified role of new global rows.
*/
create or replace function new_hsof_membership_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office_membership'),
            rbac.globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_office_membership_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure new_hsof_membership_grants_insert_to_global_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-membership-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office_membership.
*/
create or replace function hs_office_membership_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT INSERT if rbac.global ADMIN
    if rbac.isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office_membership values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger hs_office_membership_insert_permission_check_tg
    before insert on hs_office_membership
    for each row
        execute procedure hs_office_membership_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-membership-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromQuery('hs_office_membership',
    $idName$
        SELECT m.uuid AS uuid,
                'M-' || p.partnerNumber || m.memberNumberSuffix as idName
        FROM hs_office_membership AS m
        JOIN hs_office_partner AS p ON p.uuid = m.partnerUuid
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-membership-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office_membership',
    $orderBy$
        validity
    $orderBy$,
    $updates$
        validity = new.validity,
        membershipFeeBillable = new.membershipFeeBillable,
        status = new.status
    $updates$);
--//

