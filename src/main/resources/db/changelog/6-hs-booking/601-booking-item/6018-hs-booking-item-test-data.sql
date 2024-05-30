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
        into hs_booking_item (uuid, debitoruuid, type, caption, validity, resources)
        values (uuid_generate_v4(), relatedDebitor.uuid, 'MANAGED_SERVER', 'some ManagedServer', daterange('20221001', null, '[]'), '{ "CPUs": 2, "RAM": 8, "SDD": 512, "Traffic": 42 }'::jsonb),
               (uuid_generate_v4(), relatedDebitor.uuid, 'CLOUD_SERVER', 'some CloudServer', daterange('20230115', '20240415', '[)'), '{ "CPUs": 2, "RAM": 4, "HDD": 1024, "Traffic": 42 }'::jsonb),
               (uuid_generate_v4(), relatedDebitor.uuid, 'PRIVATE_CLOUD', 'some PrivateCloud', daterange('20240401', null, '[]'), '{ "CPUs": 10, "SDD": 10240, "HDD": 10240, "Traffic": 42 }'::jsonb);
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
--//
