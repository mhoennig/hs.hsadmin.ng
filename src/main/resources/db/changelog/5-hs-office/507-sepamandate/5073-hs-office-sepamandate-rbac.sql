--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-office-sepamandate-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_sepamandate');
--//


-- ============================================================================
--changeset hs-office-sepamandate-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficeSepaMandate', 'hs_office_sepamandate');
--//


-- ============================================================================
--changeset hs-office-sepamandate-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficeSepaMandate(
    NEW hs_office_sepamandate
)
    language plpgsql as $$

declare
    newBankAccount hs_office_bankaccount;
    newDebitorRel hs_office_relation;

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    SELECT * FROM hs_office_bankaccount WHERE uuid = NEW.bankAccountUuid    INTO newBankAccount;
    assert newBankAccount.uuid is not null, format('newBankAccount must not be null for NEW.bankAccountUuid = %s', NEW.bankAccountUuid);

    SELECT debitorRel.*
        FROM hs_office_relation debitorRel
        JOIN hs_office_debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
        WHERE debitor.uuid = NEW.debitorUuid
        INTO newDebitorRel;
    assert newDebitorRel.uuid is not null, format('newDebitorRel must not be null for NEW.debitorUuid = %s', NEW.debitorUuid);


    perform createRoleWithGrants(
        hsOfficeSepaMandateOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalADMIN()],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficeSepaMandateADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficeSepaMandateOWNER(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficeSepaMandateAGENT(NEW),
            incomingSuperRoles => array[hsOfficeSepaMandateADMIN(NEW)],
            outgoingSubRoles => array[
            	hsOfficeBankAccountREFERRER(newBankAccount),
            	hsOfficeRelationAGENT(newDebitorRel)]
    );

    perform createRoleWithGrants(
        hsOfficeSepaMandateREFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[
            	hsOfficeBankAccountADMIN(newBankAccount),
            	hsOfficeRelationAGENT(newDebitorRel),
            	hsOfficeSepaMandateAGENT(NEW)],
            outgoingSubRoles => array[hsOfficeRelationTENANT(newDebitorRel)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_sepamandate row.
 */

create or replace function insertTriggerForHsOfficeSepaMandate_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficeSepaMandate(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficeSepaMandate_tg
    after insert on hs_office_sepamandate
    for each row
execute procedure insertTriggerForHsOfficeSepaMandate_tf();
--//


-- ============================================================================
--changeset hs-office-sepamandate-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO hs_office_sepamandate permissions for the related hs_office_relation rows.
 */
do language plpgsql $$
    declare
        row hs_office_relation;
    begin
        call defineContext('create INSERT INTO hs_office_sepamandate permissions for the related hs_office_relation rows');

        FOR row IN SELECT * FROM hs_office_relation
			WHERE type = 'DEBITOR'
            LOOP
                call grantPermissionToRole(
                    createPermission(row.uuid, 'INSERT', 'hs_office_sepamandate'),
                    hsOfficeRelationADMIN(row));
            END LOOP;
    END;
$$;

/**
    Adds hs_office_sepamandate INSERT permission to specified role of new hs_office_relation rows.
*/
create or replace function hs_office_sepamandate_hs_office_relation_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if NEW.type = 'DEBITOR' then
		call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_sepamandate'),
            hsOfficeRelationADMIN(NEW));
	end if;
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_hs_office_sepamandate_hs_office_relation_insert_tg
    after insert on hs_office_relation
    for each row
execute procedure hs_office_sepamandate_hs_office_relation_insert_tf();

/**
    Checks if the user or assumed roles are allowed to insert a row to hs_office_sepamandate,
    where the check is performed by an indirect role.

    An indirect role is a role which depends on an object uuid which is not a direct foreign key
    of the source entity, but needs to be fetched via joined tables.
*/
create or replace function hs_office_sepamandate_insert_permission_check_tf()
    returns trigger
    language plpgsql as $$

declare
    superRoleObjectUuid uuid;

begin
        superRoleObjectUuid := (SELECT debitorRel.uuid
            FROM hs_office_relation debitorRel
            JOIN hs_office_debitor debitor ON debitor.debitorRelUuid = debitorRel.uuid
            WHERE debitor.uuid = NEW.debitorUuid
        );
        assert superRoleObjectUuid is not null, 'superRoleObjectUuid must not be null';

        if ( not hasInsertPermission(superRoleObjectUuid, 'INSERT', 'hs_office_sepamandate') ) then
            raise exception
                '[403] insert into hs_office_sepamandate not allowed for current subjects % (%)',
                currentSubjects(), currentSubjectsUuids();
    end if;
    return NEW;
end; $$;

create trigger hs_office_sepamandate_insert_permission_check_tg
    before insert on hs_office_sepamandate
    for each row
        execute procedure hs_office_sepamandate_insert_permission_check_tf();
--//

-- ============================================================================
--changeset hs-office-sepamandate-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

    call generateRbacIdentityViewFromQuery('hs_office_sepamandate',
        $idName$
            select sm.uuid as uuid, ba.iban || '-' || sm.validity as idName
            from hs_office_sepamandate sm
            join hs_office_bankaccount ba on ba.uuid = sm.bankAccountUuid
        $idName$);
--//

-- ============================================================================
--changeset hs-office-sepamandate-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_sepamandate',
    $orderBy$
        validity
    $orderBy$,
    $updates$
        reference = new.reference,
        agreement = new.agreement,
        validity = new.validity
    $updates$);
--//

