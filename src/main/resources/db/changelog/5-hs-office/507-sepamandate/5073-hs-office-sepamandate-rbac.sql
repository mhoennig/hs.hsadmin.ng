--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset RbacObjectGenerator:hs-office-sepamandate-rbac-OBJECT endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRelatedRbacObject('hs_office.sepamandate');
--//


-- ============================================================================
--changeset RbacRoleDescriptorsGenerator:hs-office-sepamandate-rbac-ROLE-DESCRIPTORS endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRoleDescriptors('hs_office.sepamandate');
--//


-- ============================================================================
--changeset RolesGrantsAndPermissionsGenerator:hs-office-sepamandate-rbac-insert-trigger runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure hs_office.sepamandate_build_rbac_system(
    NEW hs_office.sepamandate
)
    language plpgsql as $$

declare
    newBankAccount hs_office.bankaccount;
    newDebitorRel hs_office.relation;

begin
    call rbac.enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office.bankaccount WHERE uuid = NEW.bankAccountUuid    INTO newBankAccount;
    assert newBankAccount.uuid is not null, format('newBankAccount must not be null for NEW.bankAccountUuid = %s of hs_office.sepamandate', NEW.bankAccountUuid);

    SELECT debitorRel.*
        FROM hs_office.relation debitorRel
        JOIN hs_office.debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
        WHERE debitor.uuid = NEW.debitorUuid
        INTO newDebitorRel;
    assert newDebitorRel.uuid is not null, format('newDebitorRel must not be null for NEW.debitorUuid = %s of hs_office.sepamandate', NEW.debitorUuid);


    perform rbac.defineRoleWithGrants(
        hs_office.sepamandate_OWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[rbac.global_ADMIN()],
            subjectUuids => array[rbac.currentSubjectUuid()]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.sepamandate_ADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hs_office.sepamandate_OWNER(NEW)]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.sepamandate_AGENT(NEW),
            incomingSuperRoles => array[hs_office.sepamandate_ADMIN(NEW)],
            outgoingSubRoles => array[
            	hs_office.bankaccount_REFERRER(newBankAccount),
            	hs_office.relation_AGENT(newDebitorRel)]
    );

    perform rbac.defineRoleWithGrants(
        hs_office.sepamandate_REFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hs_office.bankaccount_ADMIN(newBankAccount),
            	hs_office.relation_AGENT(newDebitorRel),
            	hs_office.sepamandate_AGENT(NEW)],
            outgoingSubRoles => array[hs_office.relation_TENANT(newDebitorRel)]
    );

    call rbac.leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office.sepamandate row.
 */

create or replace function hs_office.sepamandate_build_rbac_system_after_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call hs_office.sepamandate_build_rbac_system(NEW);
    return NEW;
end; $$;

create or replace trigger build_rbac_system_after_insert_tg
    after insert on hs_office.sepamandate
    for each row
execute procedure hs_office.sepamandate_build_rbac_system_after_insert_tf();
--//


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-sepamandate-rbac-GRANTING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to hs_office.relation ----------------------------

/*
    Grants INSERT INTO hs_office.sepamandate permissions to specified role of pre-existing hs_office.relation rows.
 */
do language plpgsql $$
    declare
        row hs_office.relation;
    begin
        call base.defineContext('create INSERT INTO hs_office.sepamandate permissions for pre-exising hs_office.relation rows');

        FOR row IN SELECT * FROM hs_office.relation
            WHERE type = 'DEBITOR'
            LOOP
                call rbac.grantPermissionToRole(
                        rbac.createPermission(row.uuid, 'INSERT', 'hs_office.sepamandate'),
                        hs_office.relation_ADMIN(row));
            END LOOP;
    end;
$$;

/**
    Grants hs_office.sepamandate INSERT permission to specified role of new relation rows.
*/
create or replace function hs_office.sepamandate_grants_insert_to_relation_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if NEW.type = 'DEBITOR' then
        call rbac.grantPermissionToRole(
            rbac.createPermission(NEW.uuid, 'INSERT', 'hs_office.sepamandate'),
            hs_office.relation_ADMIN(NEW));
    end if;
    return NEW;
end; $$;

-- ..._z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger sepamandate_z_grants_after_insert_tg
    after insert on hs_office.relation
    for each row
execute procedure hs_office.sepamandate_grants_insert_to_relation_tf();


-- ============================================================================
--changeset InsertTriggerGenerator:hs-office-sepamandate-rbac-CHECKING-INSERT-PERMISSION endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office.sepamandate.
*/
create or replace function hs_office.sepamandate_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT permission via indirect foreign key: NEW.debitorUuid
    superObjectUuid := (SELECT debitorRel.uuid
        FROM hs_office.relation debitorRel
        JOIN hs_office.debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
        WHERE debitor.uuid = NEW.debitorUuid
    );
    assert superObjectUuid is not null, 'object uuid fetched depending on hs_office.sepamandate.debitorUuid must not be null, also check fetchSql in RBAC DSL';
    if rbac.hasInsertPermission(superObjectUuid, 'hs_office.sepamandate') then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office.sepamandate values(%) not allowed for current subjects % (%)',
            NEW, base.currentSubjects(), rbac.currentSubjectOrAssumedRolesUuids();
end; $$;

create trigger sepamandate_insert_permission_check_tg
    before insert on hs_office.sepamandate
    for each row
        execute procedure hs_office.sepamandate_insert_permission_check_tf();
--//


-- ============================================================================
--changeset RbacIdentityViewGenerator:hs-office-sepamandate-rbac-IDENTITY-VIEW endDelimiter:--//
-- ----------------------------------------------------------------------------

call rbac.generateRbacIdentityViewFromQuery('hs_office.sepamandate',
    $idName$
        select sm.uuid as uuid, ba.iban || '-' || sm.validity as idName
            from hs_office.sepamandate sm
            join hs_office.bankaccount ba on ba.uuid = sm.bankAccountUuid
    $idName$);
--//


-- ============================================================================
--changeset RbacRestrictedViewGenerator:hs-office-sepamandate-rbac-RESTRICTED-VIEW runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------
call rbac.generateRbacRestrictedView('hs_office.sepamandate',
    $orderBy$
        validity
    $orderBy$,
    $updates$
        reference = new.reference,
        agreement = new.agreement,
        validity = new.validity
    $updates$);
--//


-- ============================================================================
--changeset RbacRbacSystemRebuildGenerator:hs-office-sepamandate-rbac-rebuild runOnChange:true validCheckSum:ANY endDelimiter:--//
-- ----------------------------------------------------------------------------

-- HOWTO: Rebuild RBAC-system for table hs_office.sepamandate after changing its RBAC specification.
--
-- begin transaction;
--  call base.defineContext('re-creating RBAC for table hs_office.sepamandate', null, <<insert executing global admin user here>>);
--  call hs_office.sepamandate_rebuild_rbac_system();
-- commit;
--
-- How it works:
-- 1. All grants previously created from the RBAC specification of this table will be deleted.
--    These grants are identified by `hs_office.sepamandate.grantedByTriggerOf IS NOT NULL`.
--    User-induced grants (`hs_office.sepamandate.grantedByTriggerOf IS NULL`) are NOT deleted.
-- 2. New role types will be created, but existing role types which are not specified anymore,
--    will NOT be deleted!
-- 3. All newly specified grants will be created.
--
-- IMPORTANT:
-- Make sure not to skip any previously defined role-types or you might break indirect grants!
-- E.g. If, in an updated version of the RBAC system for a table, you remove the AGENT role type
-- and now directly grant the TENANT role to the ADMIN role, all external grants to the AGENT role
-- of this table would be in a dead end.

create or replace procedure hs_office.sepamandate_rebuild_rbac_system()
    language plpgsql as $$
DECLARE
    DECLARE
    row hs_office.sepamandate;
    grantsAfter numeric;
    grantsBefore numeric;
BEGIN
    SELECT count(*) INTO grantsBefore FROM rbac.grant;

    FOR row IN SELECT * FROM hs_office.sepamandate LOOP
            -- first delete all generated grants for this row from the previously defined RBAC system
            DELETE FROM rbac.grant g
                   WHERE g.grantedbytriggerof = row.uuid;

            -- then build the grants according to the currently defined RBAC rules
            CALL hs_office.sepamandate_build_rbac_system(row);
        END LOOP;

    select count(*) into grantsAfter from rbac.grant;

    -- print how the total count of grants has changed
    raise notice 'total grant count before -> after: % -> %', grantsBefore, grantsAfter;
END;
$$;
--//

