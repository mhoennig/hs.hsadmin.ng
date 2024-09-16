--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single partner test record.
 */
create or replace procedure createHsOfficePartnerTestData(
        mandantTradeName  varchar,
        newPartnerNumber  numeric(5),
        partnerPersonName varchar,
        contactCaption      varchar )
    language plpgsql as $$
declare
    idName              varchar;
    mandantPerson       hs_office_person;
    partnerRel         hs_office_relation;
    relatedPerson       hs_office_person;
    relatedDetailsUuid  uuid;
begin
    idName := base.cleanIdentifier( partnerPersonName|| '-' || contactCaption);

    select p.* from hs_office_person p
               where p.tradeName = mandantTradeName
               into mandantPerson;
    if mandantPerson is null then
        raise exception 'mandant "%" not found', mandantTradeName;
    end if;

    select p.* from hs_office_person p
               where p.tradeName = partnerPersonName or p.familyName = partnerPersonName
               into relatedPerson;

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
        into hs_office_partner (uuid, partnerNumber, partnerRelUuid, detailsUuid)
        values (uuid_generate_v4(), newPartnerNumber, partnerRel.uuid, relatedDetailsUuid);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating partner test-data ', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call createHsOfficePartnerTestData('Hostsharing eG', 10001, 'First GmbH', 'first contact');
        call createHsOfficePartnerTestData('Hostsharing eG', 10002, 'Second e.K.', 'second contact');
        call createHsOfficePartnerTestData('Hostsharing eG', 10003, 'Third OHG', 'third contact');
        call createHsOfficePartnerTestData('Hostsharing eG', 10004, 'Fourth eG', 'fourth contact');
        call createHsOfficePartnerTestData('Hostsharing eG', 10010, 'Smith', 'fifth contact');
    end;
$$;
--//
