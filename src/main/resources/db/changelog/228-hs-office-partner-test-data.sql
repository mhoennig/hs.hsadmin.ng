--liquibase formatted sql


-- ============================================================================
--changeset hs-office-partner-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single partner test record.
 */
create or replace procedure createHsOfficePartnerTestData(
        debitorNumberPrefix numeric(5),
        personTradeOrFamilyName varchar,
        contactLabel varchar )
    language plpgsql as $$
declare
    currentTask         varchar;
    idName              varchar;
    relatedPerson       hs_office_person;
    relatedContact      hs_office_contact;
    relatedDetailsUuid  uuid;
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

    raise notice 'creating test partner: %', idName;
    raise notice '- using person (%): %', relatedPerson.uuid, relatedPerson;
    raise notice '- using contact (%): %', relatedContact.uuid, relatedContact;

    if relatedPerson.persontype = 'NP' then
        insert
            into hs_office_partner_details (uuid, birthName, birthday, birthPlace)
            values (uuid_generate_v4(), 'Meyer', '1987-10-31', 'Hamburg')
            returning uuid into relatedDetailsUuid;
    else
        insert
            into hs_office_partner_details (uuid, registrationOffice, registrationNumber)
            values (uuid_generate_v4(), 'Hamburg', '12345')
            returning uuid into relatedDetailsUuid;
    end if;

    insert
        into hs_office_partner (uuid, debitorNumberPrefix, personuuid, contactuuid, detailsUuid)
        values (uuid_generate_v4(), debitorNumberPrefix, relatedPerson.uuid, relatedContact.uuid, relatedDetailsUuid);
end; $$;
--//



-- ============================================================================
--changeset hs-office-partner-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficePartnerTestData(10001, 'First GmbH', 'first contact');
        call createHsOfficePartnerTestData(10002, 'Second e.K.', 'second contact');
        call createHsOfficePartnerTestData(10003, 'Third OHG', 'third contact');
        call createHsOfficePartnerTestData(10004, 'Fourth e.G.', 'forth contact');
        call createHsOfficePartnerTestData(10010, 'Smith', 'fifth contact');
    end;
$$;
--//
