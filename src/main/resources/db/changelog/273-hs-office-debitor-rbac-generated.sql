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

    SELECT * FROM hs_office_relation WHERE uuid = NEW.partnerRelUuid    INTO newPartnerRel;

    SELECT * FROM hs_office_relation WHERE uuid = NEW.debitorRelUuid    INTO newDebitorRel;
    assert newDebitorRel.uuid is not null, format('newDebitorRel must not be null for NEW.debitorRelUuid = %s', NEW.debitorRelUuid);

    SELECT * FROM hs_office_bankaccount WHERE uuid = NEW.refundBankAccountUuid    INTO newRefundBankAccount;

    call grantRoleToRole(hsOfficeBankAccountReferrer(newRefundBankAccount), hsOfficeRelationAgent(newDebitorRel));
    call grantRoleToRole(hsOfficeRelationAdmin(newDebitorRel), hsOfficeRelationAdmin(newPartnerRel));
    call grantRoleToRole(hsOfficeRelationAgent(newDebitorRel), hsOfficeBankAccountAdmin(newRefundBankAccount));
    call grantRoleToRole(hsOfficeRelationAgent(newDebitorRel), hsOfficeRelationAgent(newPartnerRel));
    call grantRoleToRole(hsOfficeRelationTenant(newPartnerRel), hsOfficeRelationAgent(newDebitorRel));

    call grantPermissionToRole(createPermission(NEW.uuid, 'DELETE'), hsOfficeRelationOwner(newDebitorRel));
    call grantPermissionToRole(createPermission(NEW.uuid, 'SELECT'), hsOfficeRelationTenant(newDebitorRel));
    call grantPermissionToRole(createPermission(NEW.uuid, 'UPDATE'), hsOfficeRelationAdmin(newDebitorRel));

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

    if NEW.refundBankAccountUuid is distinct from OLD.refundBankAccountUuid then
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
--changeset hs-office-debitor-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO hs_office_debitor permissions for the related global rows.
 */
do language plpgsql $$
    declare
        row global;
        permissionUuid uuid;
        roleUuid uuid;
    begin
        call defineContext('create INSERT INTO hs_office_debitor permissions for the related global rows');

        FOR row IN SELECT * FROM global
            LOOP
                roleUuid := findRoleId(globalAdmin());
                permissionUuid := createPermission(row.uuid, 'INSERT', 'hs_office_debitor');
                call grantPermissionToRole(permissionUuid, roleUuid);
            END LOOP;
    END;
$$;

/**
    Adds hs_office_debitor INSERT permission to specified role of new global rows.
*/
create or replace function hs_office_debitor_global_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_debitor'),
            globalAdmin());
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_hs_office_debitor_global_insert_tg
    after insert on global
    for each row
execute procedure hs_office_debitor_global_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_debitor,
    where only global-admin has that permission.
*/
create or replace function hs_office_debitor_insert_permission_missing_tf()
    returns trigger
    language plpgsql as $$
begin
    raise exception '[403] insert into hs_office_debitor not allowed for current subjects % (%)',
        currentSubjects(), currentSubjectsUuids();
end; $$;

create trigger hs_office_debitor_insert_permission_check_tg
    before insert on hs_office_debitor
    for each row
    when ( not isGlobalAdmin() )
        execute procedure hs_office_debitor_insert_permission_missing_tf();
--//

-- ============================================================================
--changeset hs-office-debitor-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

    call generateRbacIdentityViewFromQuery('hs_office_debitor',
        $idName$
                SELECT debitor.uuid,
                        'D-' || (SELECT partner.partnerNumber
                                FROM hs_office_partner partner
                                JOIN hs_office_relation partnerRel
                                    ON partnerRel.uuid = partner.partnerRelUUid AND partnerRel.type = 'PARTNER'
                                JOIN hs_office_relation debitorRel
                                    ON debitorRel.anchorUuid = partnerRel.holderUuid AND partnerRel.type = 'DEBITOR'
                                WHERE debitorRel.uuid = debitor.debitorRelUuid)
                             || to_char(debitorNumberSuffix, 'fm00')
                from hs_office_debitor as debitor
        $idName$);
--//

-- ============================================================================
--changeset hs-office-debitor-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_debitor',
    $orderBy$
            SELECT debitor.uuid,
                        'D-' || (SELECT partner.partnerNumber
                                FROM hs_office_partner partner
                                JOIN hs_office_relation partnerRel
                                    ON partnerRel.uuid = partner.partnerRelUUid AND partnerRel.type = 'PARTNER'
                                JOIN hs_office_relation debitorRel
                                    ON debitorRel.anchorUuid = partnerRel.holderUuid AND partnerRel.type = 'DEBITOR'
                                WHERE debitorRel.uuid = debitor.debitorRelUuid)
                             || to_char(debitorNumberSuffix, 'fm00')
                from hs_office_debitor as debitor
    $orderBy$,
    $updates$
        debitorRel = new.debitorRel,
        billable = new.billable,
        debitorUuid = new.debitorUuid,
        refundBankAccountUuid = new.refundBankAccountUuid,
        vatId = new.vatId,
        vatCountryCode = new.vatCountryCode,
        vatBusiness = new.vatBusiness,
        vatReverseCharge = new.vatReverseCharge,
        defaultPrefix = new.defaultPrefix
    $updates$);
--//

