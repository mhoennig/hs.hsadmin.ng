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
        hsOfficeMembershipOwner(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[hsOfficeRelationAdmin(newPartnerRel)],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficeMembershipAdmin(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[
            	hsOfficeMembershipOwner(NEW),
            	hsOfficeRelationAgent(newPartnerRel)]
    );

    perform createRoleWithGrants(
        hsOfficeMembershipReferrer(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsOfficeMembershipAdmin(NEW)],
            outgoingSubRoles => array[hsOfficeRelationTenant(newPartnerRel)]
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
--changeset hs-office-membership-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO hs_office_membership permissions for the related global rows.
 */
do language plpgsql $$
    declare
        row global;
    begin
        call defineContext('create INSERT INTO hs_office_membership permissions for the related global rows');

        FOR row IN SELECT * FROM global
            LOOP
                call grantPermissionToRole(
                    createPermission(row.uuid, 'INSERT', 'hs_office_membership'),
                    globalAdmin());
            END LOOP;
    END;
$$;

/**
    Adds hs_office_membership INSERT permission to specified role of new global rows.
*/
create or replace function hs_office_membership_global_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_membership'),
            globalAdmin());
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_hs_office_membership_global_insert_tg
    after insert on global
    for each row
execute procedure hs_office_membership_global_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_membership,
    where only global-admin has that permission.
*/
create or replace function hs_office_membership_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into hs_office_membership not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_membership_insert_permission_check_tg
    before insert on hs_office_membership
    for each row
    when ( not isGlobalAdmin() )
        execute procedure hs_office_membership_insert_permission_missing_tf();
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
        reasonForTermination = new.reasonForTermination
    $updates$);
--//

