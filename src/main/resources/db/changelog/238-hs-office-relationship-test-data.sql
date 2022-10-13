--liquibase formatted sql


-- ============================================================================
--changeset hs-office-relationship-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single relationship test record.
 */
create or replace procedure createHsOfficeRelationshipTestData(
        anchorPersonTradeName varchar,
        holderPersonFamilyName varchar,
        relationshipType HsOfficeRelationshipType,
        contactLabel varchar)
    language plpgsql as $$
declare
    currentTask     varchar;
    idName          varchar;
    anchorPerson    hs_office_person;
    holderPerson    hs_office_person;
    contact         hs_office_contact;

begin
    idName := cleanIdentifier( anchorPersonTradeName || '-' || holderPersonFamilyName);
    currentTask := 'creating relationship test-data ' || idName;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select p.* from hs_office_person p where p.tradeName = anchorPersonTradeName into anchorPerson;
    select p.* from hs_office_person p where p.familyName = holderPersonFamilyName into holderPerson;
    select c.* from hs_office_contact c where c.label = contactLabel into contact;

    raise notice 'creating test relationship: %', idName;
    raise notice '- using anchor person (%): %', anchorPerson.uuid, anchorPerson;
    raise notice '- using holder person (%): %', holderPerson.uuid, holderPerson;
    raise notice '- using contact (%): %', contact.uuid, contact;
    insert
        into hs_office_relationship (uuid, relanchoruuid, relholderuuid, reltype, contactUuid)
        values (uuid_generate_v4(), anchorPerson.uuid, holderPerson.uuid, relationshipType, contact.uuid);
end; $$;
--//

/*
    Creates a range of test relationship for mass data generation.
 */
create or replace procedure createHsOfficeRelationshipTestData(
    startCount integer,  -- count of auto generated rows before the run
    endCount integer     -- count of auto generated rows after the run
)
    language plpgsql as $$
declare
    person hs_office_person;
    contact hs_office_contact;
begin
    for t in startCount..endCount
        loop
            select p.* from hs_office_person p where tradeName = intToVarChar(t, 4) into person;
            select c.* from hs_office_contact c where c.label = intToVarChar(t, 4) || '#' || t into contact;

            call createHsOfficeRelationshipTestData(person.uuid, contact.uuid, 'SOLE_AGENT');
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset hs-office-relationship-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeRelationshipTestData('First GmbH', 'Smith', 'SOLE_AGENT', 'first contact');

        call createHsOfficeRelationshipTestData('Second e.K.', 'Smith', 'SOLE_AGENT', 'second contact');

        call createHsOfficeRelationshipTestData('Third OHG', 'Smith', 'SOLE_AGENT', 'third contact');
    end;
$$;
--//
