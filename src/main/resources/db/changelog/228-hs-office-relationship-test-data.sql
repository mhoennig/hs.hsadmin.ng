--liquibase formatted sql


-- ============================================================================
--changeset hs-office-relationship-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single relationship test record.
 */
create or replace procedure createHsOfficeRelationshipTestData(
        holderPersonName varchar,
        relationshipType HsOfficeRelationshipType,
        anchorPersonTradeName varchar,
        contactLabel varchar,
        mark varchar default null)
    language plpgsql as $$
declare
    currentTask     varchar;
    idName          varchar;
    anchorPerson    hs_office_person;
    holderPerson    hs_office_person;
    contact         hs_office_contact;

begin
    idName := cleanIdentifier( anchorPersonTradeName || '-' || holderPersonName);
    currentTask := 'creating relationship test-data ' || idName;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select p.* from hs_office_person p where p.tradeName = anchorPersonTradeName into anchorPerson;
    if anchorPerson is null then
        raise exception 'anchorPerson "%" not found', anchorPersonTradeName;
    end if;

    select p.* from hs_office_person p
               where p.tradeName = holderPersonName or p.familyName = holderPersonName
               into holderPerson;
    if holderPerson is null then
        raise exception 'holderPerson "%" not found', holderPersonName;
    end if;

    select c.* from hs_office_contact c where c.label = contactLabel into contact;
    if contact is null then
        raise exception 'contact "%" not found', contactLabel;
    end if;

    raise notice 'creating test relationship: %', idName;
    raise notice '- using anchor person (%): %', anchorPerson.uuid, anchorPerson;
    raise notice '- using holder person (%): %', holderPerson.uuid, holderPerson;
    raise notice '- using contact (%): %', contact.uuid, contact;
    insert
        into hs_office_relationship (uuid, relanchoruuid, relholderuuid, reltype, relmark, contactUuid)
        values (uuid_generate_v4(), anchorPerson.uuid, holderPerson.uuid, relationshipType, mark, contact.uuid);
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

            call createHsOfficeRelationshipTestData(person.uuid, contact.uuid, 'REPRESENTATIVE');
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset hs-office-relationship-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficeRelationshipTestData('First GmbH', 'PARTNER', 'Hostsharing eG', 'first contact');
        call createHsOfficeRelationshipTestData('Firby', 'REPRESENTATIVE', 'First GmbH', 'first contact');

        call createHsOfficeRelationshipTestData('Second e.K.', 'PARTNER', 'Hostsharing eG', 'second contact');
        call createHsOfficeRelationshipTestData('Smith', 'REPRESENTATIVE', 'Second e.K.', 'second contact');

        call createHsOfficeRelationshipTestData('Third OHG', 'PARTNER', 'Hostsharing eG', 'third contact');
        call createHsOfficeRelationshipTestData('Tucker', 'REPRESENTATIVE', 'Third OHG', 'third contact');

        call createHsOfficeRelationshipTestData('Fourth eG', 'PARTNER', 'Hostsharing eG', 'fourth contact');
        call createHsOfficeRelationshipTestData('Fouler', 'REPRESENTATIVE', 'Third OHG', 'third contact');

        call createHsOfficeRelationshipTestData('Smith', 'PARTNER', 'Hostsharing eG', 'sixth contact');
        call createHsOfficeRelationshipTestData('Smith', 'SUBSCRIBER', 'Third OHG', 'third contact', 'members-announce');
    end;
$$;
--//
