--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single partner test record.
 */
create or replace procedure hs_office.partner_create_test_data(
        mandantTradeName  varchar,
        newPartnerNumber  numeric(5),
        partnerPersonName varchar,
        contactCaption      varchar )
    language plpgsql as $$
declare
    idName              varchar;
    mandantPerson       hs_office.person;
    partnerRel         hs_office.relation;
    relatedPerson       hs_office.person;
    relatedDetailsUuid  uuid;
begin
    idName := base.cleanIdentifier( partnerPersonName|| '-' || contactCaption);

    select p.* from hs_office.person p
               where p.tradeName = mandantTradeName
               into mandantPerson;
    if mandantPerson is null then
        raise exception 'mandant "%" not found', mandantTradeName;
    end if;

    select p.* from hs_office.person p
               where p.tradeName = partnerPersonName or p.familyName = partnerPersonName
               into relatedPerson;

    select r.* from hs_office.relation r
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
            into hs_office.partner_details (uuid, birthName, birthday, birthPlace)
            values (uuid_generate_v4(), 'Meyer', '1987-10-31', 'Hamburg')
            returning uuid into relatedDetailsUuid;
    else
        insert
            into hs_office.partner_details (uuid, registrationOffice, registrationNumber)
            values (uuid_generate_v4(), 'Hamburg', 'RegNo123456789')
            returning uuid into relatedDetailsUuid;
    end if;

    insert
        into hs_office.partner (uuid, partnerNumber, partnerRelUuid, detailsUuid)
        values (uuid_generate_v4(), newPartnerNumber, partnerRel.uuid, relatedDetailsUuid);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating partner test-data ', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call hs_office.partner_create_test_data('Hostsharing eG', 10001, 'First GmbH', 'first contact');
        call hs_office.partner_create_test_data('Hostsharing eG', 10002, 'Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.', 'second contact');
        call hs_office.partner_create_test_data('Hostsharing eG', 10003, 'Third OHG', 'third contact');
        call hs_office.partner_create_test_data('Hostsharing eG', 10004, 'Fourth eG', 'fourth contact');
        call hs_office.partner_create_test_data('Hostsharing eG', 10010, 'Smith', 'fifth contact');
    end;
$$;
--//
