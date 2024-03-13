--liquibase formatted sql


-- ============================================================================
--changeset hs-office-partner-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single partner test record.
 */
create or replace procedure createHsOfficePartnerTestData(
        mandantTradeName varchar,
        partnerNumber numeric(5),
        partnerPersonName varchar,
        contactLabel varchar )
    language plpgsql as $$
declare
    currentTask         varchar;
    idName              varchar;
    mandantPerson       hs_office_person;
    partnerRel          hs_office_relation;
    relatedPerson       hs_office_person;
    relatedContact      hs_office_contact;
    relatedDetailsUuid  uuid;
begin
    idName := cleanIdentifier( partnerPersonName|| '-' || contactLabel);
    currentTask := 'creating partner test-data ' || idName;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global.admin');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select p.* from hs_office_person p
               where p.tradeName = mandantTradeName
               into mandantPerson;
    if mandantPerson is null then
        raise exception 'mandant "%" not found', mandantTradeName;
    end if;

    select p.* from hs_office_person p
               where p.tradeName = partnerPersonName or p.familyName = partnerPersonName
               into relatedPerson;
    select c.* from hs_office_contact c
               where c.label = contactLabel
               into relatedContact;

    select r.* from hs_office_relation r
            where r.type = 'PARTNER'
                and r.anchoruuid = mandantPerson.uuid and r.holderuuid = relatedPerson.uuid
            into partnerRel;
    if partnerRel is null then
        raise exception 'partnerRel "%"-"%" not found', mandantPerson.tradename, partnerPersonName;
    end if;

    raise notice 'creating test partner: %', idName;
    raise notice '- using partnerRel (%): %', partnerRel.uuid, partnerRel;
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
            values (uuid_generate_v4(), 'Hamburg', 'RegNo123456789')
            returning uuid into relatedDetailsUuid;
    end if;

    insert
        into hs_office_partner (uuid, partnerNumber, partnerRelUuid, personuuid, contactuuid, detailsUuid)
        values (uuid_generate_v4(), partnerNumber, partnerRel.uuid, relatedPerson.uuid, relatedContact.uuid, relatedDetailsUuid);
end; $$;
--//



-- ============================================================================
--changeset hs-office-partner-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsOfficePartnerTestData('Hostsharing eG', 10001, 'First GmbH', 'first contact');
        call createHsOfficePartnerTestData('Hostsharing eG', 10002, 'Second e.K.', 'second contact');
        call createHsOfficePartnerTestData('Hostsharing eG', 10003, 'Third OHG', 'third contact');
        call createHsOfficePartnerTestData('Hostsharing eG', 10004, 'Fourth eG', 'fourth contact');
        call createHsOfficePartnerTestData('Hostsharing eG', 10010, 'Smith', 'fifth contact');
    end;
$$;
--//
