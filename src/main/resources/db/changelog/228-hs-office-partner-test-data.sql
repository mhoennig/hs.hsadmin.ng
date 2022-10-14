--liquibase formatted sql


-- ============================================================================
--changeset hs-office-partner-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single partner test record.
 */
create or replace procedure createHsOfficePartnerTestData( personTradeOrFamilyName varchar, contactLabel varchar )
    language plpgsql as $$
declare
    currentTask     varchar;
    idName          varchar;
    relatedPerson   hs_office_person;
    relatedContact  hs_office_contact;
    birthday        date;
begin
    idName := cleanIdentifier( personTradeOrFamilyName|| '-' || contactLabel);
    currentTask := 'creating partner test-data ' || idName;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select p.* from hs_office_person p
               where p.tradeName = personTradeOrFamilyName or p.familyName = personTradeOrFamilyName
               into relatedPerson;
    select c.* from hs_office_contact c
               where c.label = contactLabel
               into relatedContact;

    if relatedPerson.persontype = 'NATURAL' then
        birthday := '1987-10-31'::date;
    end if;

    raise notice 'creating test partner: %', idName;
    raise notice '- using person (%): %', relatedPerson.uuid, relatedPerson;
    raise notice '- using contact (%): %', relatedContact.uuid, relatedContact;
    insert
        into hs_office_partner (uuid, personuuid, contactuuid, birthday)
        values (uuid_generate_v4(), relatedPerson.uuid, relatedContact.uuid, birthDay);
end; $$;
--//

/*
    Creates a range of test partner for mass data generation.
 */
create or replace procedure createHsOfficePartnerTestData(
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

            call createHsOfficePartnerTestData(person.uuid, contact.uuid);
            commit;
        end loop;
end; $$;
--//


-- ============================================================================
--changeset hs-office-partner-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficePartnerTestData('First GmbH', 'first contact');
        call createHsOfficePartnerTestData('Second e.K.', 'second contact');
        call createHsOfficePartnerTestData('Third OHG', 'third contact');
        call createHsOfficePartnerTestData('Fourth e.G.', 'forth contact');
        call createHsOfficePartnerTestData('Smith', 'fifth contact');
    end;
$$;
--//
