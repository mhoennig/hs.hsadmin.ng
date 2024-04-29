--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-office-coopassetstransaction-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_coopassetstransaction');
--//


-- ============================================================================
--changeset hs-office-coopassetstransaction-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeCoopAssetsTransaction', 'hs_office_coopassetstransaction');
--//


-- ============================================================================
--changeset hs-office-coopassetstransaction-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficeCoopAssetsTransaction(
    NEW hs_office_coopassetstransaction
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
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_coopassetstransaction row.
 */

create or replace function insertTriggerForHsOfficeCoopAssetsTransaction_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficeCoopAssetsTransaction(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficeCoopAssetsTransaction_tg
    after insert on hs_office_coopassetstransaction
    for each row
execute procedure insertTriggerForHsOfficeCoopAssetsTransaction_tf();
--//


-- ============================================================================
--changeset hs-office-coopassetstransaction-rbac-GRANTING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to hs_office_membership ----------------------------

/*
    Grants INSERT INTO hs_office_coopassetstransaction permissions to specified role of pre-existing hs_office_membership rows.
 */
do language plpgsql $$
    declare
        row hs_office_membership;
    begin
        call defineContext('create INSERT INTO hs_office_coopassetstransaction permissions for pre-exising hs_office_membership rows');

        FOR row IN SELECT * FROM hs_office_membership
            -- unconditional for all rows in that table
            LOOP
                call grantPermissionToRole(
                        createPermission(row.uuid, 'INSERT', 'hs_office_coopassetstransaction'),
                        hsOfficeMembershipADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_office_coopassetstransaction INSERT permission to specified role of new hs_office_membership rows.
*/
create or replace function new_hs_office_coopassetstransaction_grants_insert_to_hs_office_membership_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_coopassetstransaction'),
            hsOfficeMembershipADMIN(NEW));
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_office_coopassetstransaction_grants_insert_to_hs_office_membership_tg
    after insert on hs_office_membership
    for each row
execute procedure new_hs_office_coopassetstransaction_grants_insert_to_hs_office_membership_tf();


-- ============================================================================
--changeset hs_office_coopassetstransaction-rbac-CHECKING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office_coopassetstransaction.
*/
create or replace function hs_office_coopassetstransaction_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via direct foreign key: NEW.membershipUuid
    if hasInsertPermission(NEW.membershipUuid, 'hs_office_coopassetstransaction') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office_coopassetstransaction not allowed for current subjects % (%)',
            currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_coopassetstransaction_insert_permission_check_tg
    before insert on hs_office_coopassetstransaction
    for each row
        execute procedure hs_office_coopassetstransaction_insert_permission_check_tf();
--//


-- ============================================================================
--changeset hs-office-coopassetstransaction-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_coopassetstransaction',
    $idName$
        reference
    $idName$);
--//


-- ============================================================================
--changeset hs-office-coopassetstransaction-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_coopassetstransaction',
    $orderBy$
        reference
    $orderBy$,
    $updates$
        comment = new.comment
    $updates$);
--//

