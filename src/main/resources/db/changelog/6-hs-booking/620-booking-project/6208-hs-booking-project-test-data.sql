--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-booking-project-TEST-DATA-GENERATOR endDelimiter:--//
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
    relatedDebitor      hs_office.debitor;
begin

    select debitor.* into relatedDebitor
                     from hs_office.debitor debitor
                              join hs_office.relation debitorRel on debitorRel.uuid = debitor.debitorRelUuid
                              join hs_office.relation partnerRel on partnerRel.holderUuid = debitorRel.anchorUuid
                              join hs_office.partner partner on partner.partnerRelUuid = partnerRel.uuid
                     where partner.partnerNumber = givenPartnerNumber and debitor.debitorNumberSuffix = givenDebitorSuffix;

    raise notice 'creating test booking-project: %', givenDebitorSuffix::text;
    raise notice '- using debitor (%): %', relatedDebitor.uuid, relatedDebitor;
    insert
        into hs_booking_project (uuid, debitoruuid, caption)
        values (uuid_generate_v4(), relatedDebitor.uuid, 'D-' || givenPartnerNumber::text || givenDebitorSuffix || ' default project');
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-booking-project-TEST-DATA-GENERATION –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating booking-project test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call createHsBookingProjectTransactionTestData(10001, '11');
        call createHsBookingProjectTransactionTestData(10002, '12');
        call createHsBookingProjectTransactionTestData(10003, '13');
    end;
$$;
--//
