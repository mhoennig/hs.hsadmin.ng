--liquibase formatted sql


-- ============================================================================
--changeset hs-booking-project-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single hs_booking_project test record.
 */
create or replace procedure createHsBookingProjectTransactionTestData(
    givenPartnerNumber numeric,
    givenDebitorSuffix char(2)
    )
    language plpgsql as $$
declare
    relatedDebitor      hs_office_debitor;
begin

    select debitor.* into relatedDebitor
                     from hs_office_debitor debitor
                              join hs_office_relation debitorRel on debitorRel.uuid = debitor.debitorRelUuid
                              join hs_office_relation partnerRel on partnerRel.holderUuid = debitorRel.anchorUuid
                              join hs_office_partner partner on partner.partnerRelUuid = partnerRel.uuid
                     where partner.partnerNumber = givenPartnerNumber and debitor.debitorNumberSuffix = givenDebitorSuffix;

    raise notice 'creating test booking-project: %', givenDebitorSuffix::text;
    raise notice '- using debitor (%): %', relatedDebitor.uuid, relatedDebitor;
    insert
        into hs_booking_project (uuid, debitoruuid, caption)
        values (uuid_generate_v4(), relatedDebitor.uuid, 'D-' || givenPartnerNumber::text || givenDebitorSuffix || ' default project');
end; $$;
--//


-- ============================================================================
--changeset hs-booking-project-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call defineContext('creating booking-project test-data', null, 'superuser-alex@hostsharing.net', 'global#global:ADMIN');

        call createHsBookingProjectTransactionTestData(10001, '11');
        call createHsBookingProjectTransactionTestData(10002, '12');
        call createHsBookingProjectTransactionTestData(10003, '13');
    end;
$$;
--//
