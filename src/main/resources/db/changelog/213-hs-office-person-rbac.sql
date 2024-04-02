--liquibase formatted sql
-- This code generated was by RbacViewPostgresGenerator, do not amend manually.


-- ============================================================================
--changeset hs-office-person-rbac-OBJECT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRelatedRbacObject('hs_office_person');
--//


-- ============================================================================
--changeset hs-office-person-rbac-ROLE-DESCRIPTORS:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRoleDescriptors('hsOfficePerson', 'hs_office_person');
--//


-- ============================================================================
--changeset hs-office-person-rbac-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates the roles, grants and permission for the AFTER INSERT TRIGGER.
 */

create or replace procedure buildRbacSystemForHsOfficePerson(
    NEW hs_office_person
)
    language plpgsql as $$

declare

begin
    call enterTriggerForObjectUuid(NEW.uuid);

    perform createRoleWithGrants(
        hsOfficePersonOWNER(NEW),
            permissions => array['DELETE'],
            incomingSuperRoles => array[globalADMIN()],
            userUuids => array[currentUserUuid()]
    );

    perform createRoleWithGrants(
        hsOfficePersonADMIN(NEW),
            permissions => array['UPDATE'],
            incomingSuperRoles => array[hsOfficePersonOWNER(NEW)]
    );

    perform createRoleWithGrants(
        hsOfficePersonREFERRER(NEW),
            permissions => array['SELECT'],
            incomingSuperRoles => array[hsOfficePersonADMIN(NEW)]
    );

    call leaveTriggerForObjectUuid(NEW.uuid);
end; $$;

/*
    AFTER INSERT TRIGGER to create the role+grant structure for a new hs_office_person row.
 */

create or replace function insertTriggerForHsOfficePerson_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call buildRbacSystemForHsOfficePerson(NEW);
    return NEW;
end; $$;

create trigger insertTriggerForHsOfficePerson_tg
    after insert on hs_office_person
    for each row
execute procedure insertTriggerForHsOfficePerson_tf();
--//


-- ============================================================================
--changeset hs-office-person-rbac-INSERT:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates INSERT INTO hs_office_person permissions for the related global rows.
 */
do language plpgsql $$
    declare
        row global;
    begin
        call defineContext('create INSERT INTO hs_office_person permissions for the related global rows');

        FOR row IN SELECT * FROM global
            LOOP
                call grantPermissionToRole(
                    createPermission(row.uuid, 'INSERT', 'hs_office_person'),
                    globalGUEST());
            END LOOP;
    END;
$$;

/**
    Adds hs_office_person INSERT permission to specified role of new global rows.
*/
create or replace function hs_office_person_global_insert_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    call grantPermissionToRole(
            createPermission(NEW.uuid, 'INSERT', 'hs_office_person'),
            globalGUEST());
    return NEW;
end; $$;

-- z_... is to put it at the end of after insert triggers, to make sure the roles exist
create trigger z_hs_office_person_global_insert_tg
    after insert on global
    for each row
execute procedure hs_office_person_global_insert_tf();
--//

-- ============================================================================
--changeset hs-office-person-rbac-IDENTITY-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call generateRbacIdentityViewFromProjection('hs_office_person',
    $idName$
        concat(tradeName, familyName, givenName)
    $idName$);
--//

-- ============================================================================
--changeset hs-office-person-rbac-RESTRICTED-VIEW:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
call generateRbacRestrictedView('hs_office_person',
    $orderBy$
        concat(tradeName, familyName, givenName)
    $orderBy$,
    $updates$
        personType = new.personType,
        tradeName = new.tradeName,
        givenName = new.givenName,
        familyName = new.familyName
    $updates$);
--//

