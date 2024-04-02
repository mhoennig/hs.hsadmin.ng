--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-office-coopsharestransaction-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_coopsharestransaction');
--//


-- ============================================================================
--changeset hs-office-coopsharestransaction-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeCoopSharesTransaction', 'hs_office_coopsharestransaction');
--//


-- ============================================================================
--changeset hs-office-coopsharestransaction-rbac-insert-trigger:1 endDelimiter:--//
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
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office_membership WHERE uuid = NEW.membershipUuid    INTO newMembership;
    assert newMembership.uuid is not null, format('newMembership must not be null for NEW.membershipUuid = %s', NEW.membershipUuid);

    call grantPermissionToRole(createPermission(NEW.uuid, 'SELECT'), hsOfficeMembershipAGENT(newMembership));
    call grantPermissionToRole(createPermission(NEW.uuid, 'UPDATE'), hsOfficeMembershipADMIN(newMembership));

    call leaveTriggerForObjectUuid(NEW.uuid);
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
--changeset hs-office-coopsharestransaction-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO hs_office_coopsharestransaction permissions for the related hs_office_membership rows.
 */
do language plpgsql $$
    declare
        row hs_office_membership;
    begin
        call defineContext('create INSERT INTO hs_office_coopsharestransaction permissions for the related hs_office_membership rows');

        FOR row IN SELECT * FROM hs_office_membership
            LOOP
                call grantPermissionToRole(
                    createPermission(row.uuid, 'INSERT', 'hs_office_coopsharestransaction'),
                    hsOfficeMembershipADMIN(row));
            END LOOP;
    END;
$$;

/**
    Adds hs_office_coopsharestransaction INSERT permission to specified role of new hs_office_membership rows.
*/
create or replace function hs_office_coopsharestransaction_hs_office_membership_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_coopsharestransaction'),
            hsOfficeMembershipADMIN(NEW));
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_hs_office_coopsharestransaction_hs_office_membership_insert_tg
    after insert on hs_office_membership
    for each row
execute procedure hs_office_coopsharestransaction_hs_office_membership_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_coopsharestransaction,
    where the check is performed by a direct role.

    A direct role is a role depending on a foreign key directly available in the NEW row.
*/
create or replace function hs_office_coopsharestransaction_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into hs_office_coopsharestransaction not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_coopsharestransaction_insert_permission_check_tg
    before insert on hs_office_coopsharestransaction
    for each row
    when ( not hasInsertPermission(NEW.membershipUuid, 'INSERT', 'hs_office_coopsharestransaction') )
        execute procedure hs_office_coopsharestransaction_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset hs-office-coopsharestransaction-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_coopsharestransaction',
    $idName$
        reference
    $idName$);
--//

-- ============================================================================
--changeset hs-office-coopsharestransaction-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_coopsharestransaction',
    $orderBy$
        reference
    $orderBy$,
    $updates$
        comment = new.comment
    $updates$);
--//

