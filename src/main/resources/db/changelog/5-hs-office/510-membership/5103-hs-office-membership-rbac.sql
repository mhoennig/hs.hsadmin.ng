--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-office-membership-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_membership');
--//


-- ============================================================================
--changeset hs-office-membership-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeMembership', 'hs_office_membership');
--//


-- ============================================================================
--changeset hs-office-membership-rbac-insert-trigger:1 endDelimiter:--//
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
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT partnerRel.*
        FROM hs_office_partner AS partner
        JOIN hs_office_relation AS partnerRel ON partnerRel.uuid = partner.partnerRelUuid
        WHERE partner.uuid = NEW.partnerUuid
        INTO newPartnerRel;
    assert newPartnerRel.uuid is not null, format('newPartnerRel must not be null for NEW.partnerUuid = %s', NEW.partnerUuid);


    perform createRoleWithGrants(
        hsOfficeMembershipOWNER(NEW),
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficeMembershipADMIN(NEW),
            permissions => array['DELETE', 'UPDATE'],
            incomingSuperRoles => array[
            	hsOfficeMembershipOWNER(NEW),
            	hsOfficeRelationADMIN(newPartnerRel)]
    );

    perform createRoleWithGrants(
        hsOfficeMembershipAGENT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hsOfficeMembershipADMIN(NEW),
            	hsOfficeRelationAGENT(newPartnerRel)],
            outgoingSubRoles => array[hsOfficeRelationTENANT(newPartnerRel)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
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
--changeset hs-office-membership-rbac-GRANTING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to global ----------------------------

/*
    Grants INSERT INTO hs_office_membership permissions to specified role of pre-existing global rows.
 */
do language plpgsql $$
    declare
        row global;
    begin
        call defineContext('create INSERT INTO hs_office_membership permissions for pre-exising global rows');

        FOR row IN SELECT * FROM global
            -- unconditional for all rows in that table
            LOOP
                call grantPermissionToRole(
                        createPermission(row.uuid, 'INSERT', 'hs_office_membership'),
                        globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office_membership INSERT permission to specified role of new global rows.
*/
create or replace function new_hs_office_membership_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_membership'),
            globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_office_membership_grants_insert_to_global_tg
    after insert on global
    for each row
execute procedure new_hs_office_membership_grants_insert_to_global_tf();


-- ============================================================================
--changeset hs_office_membership-rbac-CHECKING-INSERT-PERMISSION:1 endDelimiter:--//
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
    -- check INSERT INSERT if global ADMIN
    if isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office_membership values(%) not allowed for current subjects % (%)',
            NEW, currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_membership_insert_permission_check_tg
    before insert on hs_office_membership
    for each row
        execute procedure hs_office_membership_insert_permission_check_tf();
--//


-- ============================================================================
--changeset hs-office-membership-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromQuery('hs_office_membership',
    $idName$
        SELECT m.uuid AS uuid,
                'M-' || p.partnerNumber || m.memberNumberSuffix as idName
        FROM hs_office_membership AS m
        JOIN hs_office_partner AS p ON p.uuid = m.partnerUuid
    $idName$);
--//


-- ============================================================================
--changeset hs-office-membership-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_membership',
    $orderBy$
        validity
    $orderBy$,
    $updates$
        validity = new.validity,
        membershipFeeBillable = new.membershipFeeBillable,
        status = new.status
    $updates$);
--//

