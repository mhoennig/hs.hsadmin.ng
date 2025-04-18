--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-debitor-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office.debitor');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-debitor-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_office.debitor');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-debitor-rbac-insert-trigger runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_office.debitor_build_rbac_system(
    NEW hs_office.debitor
)
    language plpgsql as $$

declare
    newPartnerRel hs_office.relation;
    newDebitorRel hs_office.relation;
    newRefundBankAccount hs_office.bankaccount;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT partnerRel.*
        FROM hs_office.relation AS partnerRel
        JOIN hs_office.relation AS debitorRel
            ON debitorRel.type = 'DEBITOR' AND debitorRel.anchorUuid = partnerRel.holderUuid
        WHERE partnerRel.type = 'PARTNER'
            AND NEW.debitorRelUuid = debitorRel.uuid
        INTO newPartnerRel;
    assert newPartnerRel.uuid is not null, format('newPartnerRel must not be null for NEW.debitorRelUuid = %s of hs_office.debitor', NEW.debitorRelUuid);

    SELECT * FROM hs_office.relation WHERE uuid = NEW.debitorRelUuid    INTO newDebitorRel;
    assert newDebitorRel.uuid is not null, format('newDebitorRel must not be null for NEW.debitorRelUuid = %s of hs_office.debitor', NEW.debitorRelUuid);

    SELECT * FROM hs_office.bankaccount WHERE uuid = NEW.refundBankAccountUuid    INTO newRefundBankAccount;

    call rbac.grantRoleToRole(hs_office.bankaccount_REFERRER(newRefundBankAccount), hs_office.relation_AGENT(newDebitorRel));
    call rbac.grantRoleToRole(hs_office.relation_ADMIN(newDebitorRel), hs_office.relation_ADMIN(newPartnerRel));
    call rbac.grantRoleToRole(hs_office.relation_AGENT(newDebitorRel), hs_office.bankaccount_ADMIN(newRefundBankAccount));
    call rbac.grantRoleToRole(hs_office.relation_AGENT(newDebitorRel), hs_office.relation_AGENT(newPartnerRel));
    call rbac.grantRoleToRole(hs_office.relation_TENANT(newPartnerRel), hs_office.relation_AGENT(newDebitorRel));

    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'DELETE'), hs_office.relation_OWNER(newDebitorRel));
    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'SELECT'), hs_office.relation_TENANT(newDebitorRel));
    call rbac.grantPermissionToRole(rbac.createPermission(NEW.uuid, 'UPDATE'), hs_office.relation_ADMIN(newDebitorRel));

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office.debitor row.
 */

create or replace function hs_office.debitor_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.debitor_build_rbac_system(NEW);
    return NEW;
end; $$;

create or replace trigger build_rbac_system_after_insert_tg
    after insert on hs_office.debitor
    for each row
execute procedure hs_office.debitor_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-debitor-rbac-update-trigger runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure hs_office.debitor_update_rbac_system(
    OLD hs_office.debitor,
    NEW hs_office.debitor
)
    language plpgsql as $$
begin

    if NEW.debitorRelUuid is distinct from OLD.debitorRelUuid
    or NEW.refundBankAccountUuid is distinct from OLD.refundBankAccountUuid then
        delete from rbac.grant g where g.grantedbytriggerof = OLD.uuid;
        call hs_office.debitor_build_rbac_system(NEW);
    end if;
end; $$;

/*
    AFTER UPDATE TRIGGER to re-wire the grant structure for a new hs_office.debitor row.
 */

create or replace function hs_office.debitor_update_rbac_system_after_update_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.debitor_update_rbac_system(OLD, NEW);
    return NEW;
end; $$;

create or replace trigger update_rbac_system_after_update_tg
    after update on hs_office.debitor
    for each row
execute procedure hs_office.debitor_update_rbac_system_after_update_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-debitor-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to rbac.global ----------------------------

/*
    Grants INSERT INTO hs_office.debitor permissions to specified role of pre-existing rbac.global rows.
 */
do language plpgsql $$
    declare
        row rbac.global;
    begin
        call base.defineContext('create INSERT INTO hs_office.debitor permissions for pre-exising rbac.global rows');

        FOR row IN SELECT * FROM rbac.global
            -- unconditional for all rows in that table
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office.debitor'),
                        rbac.global_ADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office.debitor INSERT permission to specified role of new global rows.
*/
create or replace function hs_office.debitor_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office.debitor'),
            rbac.global_ADMIN());
    -- end.
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger debitor_z_grants_after_insert_tg
    after insert on rbac.global
    for each row
execute procedure hs_office.debitor_grants_insert_to_global_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-debitor-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office.debitor.
*/
create or replace function hs_office.debitor_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission if rbac.global ADMIN
    if rbac.isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office.debitor values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger debitor_insert_permission_check_tg
    before insert on hs_office.debitor
    for each row
        execute procedure hs_office.debitor_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-debitor-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromQuery('hs_office.debitor',
    $idName$
        SELECT debitor.uuid AS uuid,
                    'D-' || (SELECT partner.partnerNumber
                            FROM hs_office.partner partner
                            JOIN hs_office.relation partnerRel
                                ON partnerRel.uuid = partner.partnerRelUUid AND partnerRel.type = 'PARTNER'
                            JOIN hs_office.relation debitorRel
                                ON debitorRel.anchorUuid = partnerRel.holderUuid AND debitorRel.type = 'DEBITOR'
                            WHERE debitorRel.uuid = debitor.debitorRelUuid)
                         || debitorNumberSuffix as idName
        FROM hs_office.debitor AS debitor
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-debitor-rbac-RESTRICTED-VIEW runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office.debitor',
    $orderBy$
        defaultPrefix
    $orderBy$,
    $updates$
        debitorRelUuid = new.debitorRelUuid,
        billable = new.billable,
        refundBankAccountUuid = new.refundBankAccountUuid,
        vatId = new.vatId,
        vatCountryCode = new.vatCountryCode,
        vatBusiness = new.vatBusiness,
        vatReverseCharge = new.vatReverseCharge,
        defaultPrefix = new.defaultPrefix
    $updates$);
--//


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-office-debitor-rbac-rebuild runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_office.debitor after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_office.debitor', null, <<insert executing global admin user here>>);
--  call hs_office.debitor_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_office.debitor.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_office.debitor.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_office.debitor_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_office.debitor;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grant;

    FOR row IN SELECT * FROM hs_office.debitor LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grant g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_office.debitor_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grant;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

