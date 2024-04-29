--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-office-debitor-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_debitor');
--//


-- ============================================================================
--changeset hs-office-debitor-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeDebitor', 'hs_office_debitor');
--//


-- ============================================================================
--changeset hs-office-debitor-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficeDebitor(
    NEW hs_office_debitor
)
    language plpgsql as $$

declare
    newPartnerRel hs_office_relation;
    newDebitorRel hs_office_relation;
    newRefundBankAccount hs_office_bankaccount;

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT partnerRel.*
        FROM hs_office_relation AS partnerRel
        JOIN hs_office_relation AS debitorRel
            ON debitorRel.type = 'DEBITOR' AND debitorRel.anchorUuid = partnerRel.holderUuid
        WHERE partnerRel.type = 'PARTNER'
            AND NEW.debitorRelUuid = debitorRel.uuid
        INTO newPartnerRel;
    assert newPartnerRel.uuid is not null, format('newPartnerRel must not be null for NEW.debitorRelUuid = %s', NEW.debitorRelUuid);

    SELECT * FROM hs_office_relation WHERE uuid = NEW.debitorRelUuid    INTO newDebitorRel;
    assert newDebitorRel.uuid is not null, format('newDebitorRel must not be null for NEW.debitorRelUuid = %s', NEW.debitorRelUuid);

    SELECT * FROM hs_office_bankaccount WHERE uuid = NEW.refundBankAccountUuid    INTO newRefundBankAccount;

    call grantRoleToRole(hsOfficeBankAccountREFERRER(newRefundBankAccount), hsOfficeRelationAGENT(newDebitorRel));
    call grantRoleToRole(hsOfficeRelationADMIN(newDebitorRel), hsOfficeRelationADMIN(newPartnerRel));
    call grantRoleToRole(hsOfficeRelationAGENT(newDebitorRel), hsOfficeBankAccountADMIN(newRefundBankAccount));
    call grantRoleToRole(hsOfficeRelationAGENT(newDebitorRel), hsOfficeRelationAGENT(newPartnerRel));
    call grantRoleToRole(hsOfficeRelationTENANT(newPartnerRel), hsOfficeRelationAGENT(newDebitorRel));

    call grantPermissionToRole(createPermission(NEW.uuid, 'DELETE'), hsOfficeRelationOWNER(newDebitorRel));
    call grantPermissionToRole(createPermission(NEW.uuid, 'SELECT'), hsOfficeRelationTENANT(newDebitorRel));
    call grantPermissionToRole(createPermission(NEW.uuid, 'UPDATE'), hsOfficeRelationADMIN(newDebitorRel));

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_debitor row.
 */

create or replace function insertTriggerForHsOfficeDebitor_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficeDebitor(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficeDebitor_tg
    after insert on hs_office_debitor
    for each row
execute procedure insertTriggerForHsOfficeDebitor_tf();
--//


-- ============================================================================
--changeset hs-office-debitor-rbac-update-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Called from the AFTER UPDATE TRIGGER to re-wire the grants.
 */

create or replace procedure updateRbacRulesForHsOfficeDebitor(
    OLD hs_office_debitor,
    NEW hs_office_debitor
)
    language plpgsql as $$
begin

    if NEW.debitorRelUuid is distinct from OLD.debitorRelUuid
    or NEW.refundBankAccountUuid is distinct from OLD.refundBankAccountUuid then
        delete from rbacgrants g where g.grantedbytriggerof = OLD.uuid;
        call buildRbacSystemForHsOfficeDebitor(NEW);
    end if;
end; $$;

/*
    AFTER INSERT TRIGGER to re-wire the grant structure for a new hs_office_debitor row.
 */

create or replace function updateTriggerForHsOfficeDebitor_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call updateRbacRulesForHsOfficeDebitor(OLD, NEW);
    return NEW;
end; $$;

create trigger updateTriggerForHsOfficeDebitor_tg
    after update on hs_office_debitor
    for each row
execute procedure updateTriggerForHsOfficeDebitor_tf();
--//


-- ============================================================================
--changeset hs-office-debitor-rbac-GRANTING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

-- granting INSERT permission to global ----------------------------

/*
    Grants INSERT INTO hs_office_debitor permissions to specified role of pre-existing global rows.
 */
do language plpgsql $$
    declare
        row global;
    begin
        call defineContext('create INSERT INTO hs_office_debitor permissions for pre-exising global rows');

        FOR row IN SELECT * FROM global
            -- unconditional for all rows in that table
            LOOP
                call grantPermissionToRole(
                        createPermission(row.uuid, 'INSERT', 'hs_office_debitor'),
                        globalADMIN());
            END LOOP;
    end;
$$;

/**
    Grants hs_office_debitor INSERT permission to specified role of new global rows.
*/
create or replace function new_hs_office_debitor_grants_insert_to_global_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    -- unconditional for all rows in that table
        call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_debitor'),
            globalADMIN());
    -- end.
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_new_hs_office_debitor_grants_insert_to_global_tg
    after insert on global
    for each row
execute procedure new_hs_office_debitor_grants_insert_to_global_tf();


-- ============================================================================
--changeset hs_office_debitor-rbac-CHECKING-INSERT-PERMISSION:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Checks if the user respectively the assumed roles are allowed to insert a row to hs_office_debitor.
*/
create or replace function hs_office_debitor_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$
declare
    superObjectUuid uuid;
begin
    -- check INSERT INSERT if global ADMIN
    if isGlobalAdmin() then
        return NEW;
    end if;

    raise exception '[403] insert into hs_office_debitor not allowed for current subjects % (%)',
            currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_debitor_insert_permission_check_tg
    before insert on hs_office_debitor
    for each row
        execute procedure hs_office_debitor_insert_permission_check_tf();
--//


-- ============================================================================
--changeset hs-office-debitor-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromQuery('hs_office_debitor',
    $idName$
        SELECT debitor.uuid AS uuid,
                    'D-' || (SELECT partner.partnerNumber
                            FROM hs_office_partner partner
                            JOIN hs_office_relation partnerRel
                                ON partnerRel.uuid = partner.partnerRelUUid AND partnerRel.type = 'PARTNER'
                            JOIN hs_office_relation debitorRel
                                ON debitorRel.anchorUuid = partnerRel.holderUuid AND debitorRel.type = 'DEBITOR'
                            WHERE debitorRel.uuid = debitor.debitorRelUuid)
                         || debitorNumberSuffix as idName
        FROM hs_office_debitor AS debitor
    $idName$);
--//


-- ============================================================================
--changeset hs-office-debitor-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_debitor',
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

