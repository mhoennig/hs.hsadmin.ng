--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-coopsharestransaction-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office_coopsharestransaction');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-coopsharestransaction-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hsOfficeCoopSharesTransaction', 'hs_office_coopsharestransaction');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-coopsharestransaction-rbac-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficeCoopSharesTransaction(
    NEW hs_office_coopsharestransaction
)
    language plpgsql as $$

declare
    newMembership hs_office_membership;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office_membership WHERE uuid = NEW.membershipUuid    INTO newMembership;
    assert newMembership.uuid is not null, format('newMembership must not be null for NEW.membershipUuid = %s', NEW.membershipUuid);

    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'SELECT'), hsOfficeMembershipAGENT(newMembership));
    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'UPDATE'), hsOfficeMembershipADMIN(newMembership));

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_coopsharestransaction row.
 */

create or replace function insertTriggerForHsOfficeCoopSharesTransaction_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficeCoopSharesTransaction(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficeCoopSharesTransaction_tg
    after insert on hs_office_coopsharestransaction
    for each row
execute procedure insertTriggerForHsOfficeCoopSharesTransaction_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-coopsharestransaction-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to hs_office_membership ----------------------------

/*
    Grants INSERT INTO hs_office_coopsharestransaction permissions to specified role of pre-existing hs_office_membership rows.
 */
do language plpgsql $$
    declare
        row hs_office_membership;
    begin
        call base.defineContext('create INSERT INTO hs_office_coopsharestransaction permissions for pre-exising hs_office_membership rows');

        FOR row IN SELECT * FROM hs_office_membership
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office_coopsharestransaction'),
                        hsOfficeMembershipADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_office_coopsharestransaction INSERT permission to specified role of new hs_office_membership rows.
*/
create or replace function new_hsof_coopsharetx_grants_insert_to_hsof_membership_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office_coopsharestransaction'),
            hsOfficeMembershipADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_office_coopsharestransaction_grants_after_insert_tg
    after insert on hs_office_membership
    for each row
execute procedure new_hsof_coopsharetx_grants_insert_to_hsof_membership_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs_office_coopsharestransaction-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office_coopsharestransaction.
*/
create or replace function hs_office_coopsharestransaction_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via direct foreign key: NEW.membershipUuid
    if rbac.hasInsertPermission(NEW.membershipUuid, 'hs_office_coopsharestransaction') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office_coopsharestransaction values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger hs_office_coopsharestransaction_insert_permission_check_tg
    before insert on hs_office_coopsharestransaction
    for each row
        execute procedure hs_office_coopsharestransaction_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-coopsharestransaction-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromProjection('hs_office_coopsharestransaction',
    $idName$
        reference
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-coopsharestransaction-rbac-RESTRICTED-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office_coopsharestransaction',
    $orderBy$
        reference
    $orderBy$,
    $updates$
        comment = new.comment
    $updates$);
--//

