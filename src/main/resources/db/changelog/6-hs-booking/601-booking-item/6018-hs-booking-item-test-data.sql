--liquibase formatted sql


-- ============================================================================
--changeset hs-booking-item-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single hs_booking_item test record.
 */
create or replace procedure createHsBookingItemTransactionTestData(
    givenPartnerNumber numeric,
    givenDebitorSuffix char(2)
    )
    language plpgsql as $$
declare
    currentTask         varchar;
    relatedDebitor      hs_office_debitor;
begin
    currentTask := 'creating booking-item test-data ' || givenPartnerNumber::text || givenDebitorSuffix;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global:ADMIN');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select debitor.* into relatedDebitor
                     from hs_office_debitor debitor
                              join hs_office_relation debitorRel on debitorRel.uuid = debitor.debitorRelUuid
                              join hs_office_relation partnerRel on partnerRel.holderUuid = debitorRel.anchorUuid
                              join hs_office_partner partner on partner.partnerRelUuid = partnerRel.uuid
                     where partner.partnerNumber = givenPartnerNumber and debitor.debitorNumberSuffix = givenDebitorSuffix;

    raise notice 'creating test booking-item: %', givenPartnerNumber::text || givenDebitorSuffix::text;
    raise notice '- using debitor (%): %', relatedDebitor.uuid, relatedDebitor;
    insert
        into hs_booking_item (uuid, debitoruuid, caption, validity, resources)
        values (uuid_generate_v4(), relatedDebitor.uuid, 'some ManagedServer', daterange('20221001', null, '[]'), '{ "CPU": 2, "SDD": 512, "extra": 42 }'::jsonb),
               (uuid_generate_v4(), relatedDebitor.uuid, 'some CloudServer', daterange('20230115', '20240415', '[)'), '{ "CPU": 2, "HDD": 1024, "extra": 42 }'::jsonb),
               (uuid_generate_v4(), relatedDebitor.uuid, 'some PrivateCloud', daterange('20240401', null, '[]'), '{ "CPU": 10, "SDD": 10240, "HDD": 10240, "extra": 42 }'::jsonb);
end; $$;
--//


-- ============================================================================
--changeset hs-booking-item-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsBookingItemTransactionTestData(10001, '11');
        call createHsBookingItemTransactionTestData(10002, '12');
        call createHsBookingItemTransactionTestData(10003, '13');
    end;
$$;
