--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-membership-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office.membership');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-membership-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsOfficeMembership', 'hs_office.membership');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-membership-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_office.membership_build_rbac_system(
    NEW hs_office.membership
)
    language plpgsql as $$

declare
    newPartnerRel hs_office.relation;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT partnerRel.*
        FROM hs_office.partner AS partner
        JOIN hs_office.relation AS partnerRel ON partnerRel.uuid = partner.partnerRelUuid
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
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office.membership row.
 */

create or replace function hs_office.membership_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.membership_build_rbac_system(NEW);
    return NEW;
end; $$;

create trigger build_rbac_system_after_insert_tg
    after insert on hs_office.membership
    for each row
execute procedure hs_office.membership_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-membership-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO hs_office.membership permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO hs_office.membership permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office.membership'),
                        rbac.globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office.membership INSERT permission to specified role of new global rows.
*/
create or replace function hs_office.new_membership_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office.membership'),
            rbac.globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_membership_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure hs_office.new_membership_grants_insert_to_global_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-membership-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office.membership.
*/
create or replace function hs_office.membership_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission if rbac.global ADMIN
    if rbac.isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office.membership values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger membership_insert_permission_check_tg
    before insert on hs_office.membership
    for each row
        execute procedure hs_office.membership_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-membership-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromQuery('hs_office.membership',
    $idName$
        SELECT m.uuid AS uuid,
                'M-' || p.partnerNumber || m.memberNumberSuffix as idName
        FROM hs_office.membership AS m
        JOIN hs_office.partner AS p ON p.uuid = m.partnerUuid
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-membership-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office.membership',
    $orderBy$
        validity
    $orderBy$,
    $updates$
        validity = new.validity,
        membershipFeeBillable = new.membershipFeeBillable,
        status = new.status
    $updates$);
--//

