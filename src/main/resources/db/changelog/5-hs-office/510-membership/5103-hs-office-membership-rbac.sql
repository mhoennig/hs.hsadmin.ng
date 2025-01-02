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
call rbac.generateRbacRoleDescriptors('hs_office.membership');
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
    assert newPartnerRel.uuid is not null, format('newPartnerRel must not be null for NEW.partnerUuid = %s of hs_office.membership', NEW.partnerUuid);


    perform rbac.defineRoleWithGrants(
        hs_office.membership_OWNER(NEW),
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.membership_ADMIN(NEW),
            permissions => array['DELETE', 'UPDATE'],
            incomingSuperRoles => array[
            	hs_office.membership_OWNER(NEW),
            	hs_office.relation_ADMIN(newPartnerRel)]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.membership_AGENT(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hs_office.membership_ADMIN(NEW),
            	hs_office.relation_AGENT(newPartnerRel)],
            outgoingSubRoles => array[hs_office.relation_TENANT(newPartnerRel)]
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
                        rbac.global_ADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office.membership INSERT permission to specified role of new global rows.
*/
create or replace function hs_office.membership_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office.membership'),
            rbac.global_ADMIN());
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger membership_z_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure hs_office.membership_grants_insert_to_global_tf();


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


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-office-membership-rbac-rebuild endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_office.membership after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_office.membership', null, <<insert executing global admin user here>>);
--  call hs_office.membership_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_office.membership.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_office.membership.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_office.membership_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_office.membership;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grants;

    FOR row IN SELECT * FROM hs_office.membership LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grants g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_office.membership_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grants;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

