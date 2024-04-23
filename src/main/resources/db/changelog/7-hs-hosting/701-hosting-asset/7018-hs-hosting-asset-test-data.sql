--liquibase formatted sql


-- ============================================================================
--changeset hs-hosting-asset-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single hs_hosting_asset test record.
 */
create or replace procedure createHsHostingAssetTestData(
    givenPartnerNumber numeric,
    givenDebitorSuffix char(2),
    givenWebspacePrefix char(3)
    )
    language plpgsql as $$
declare
    currentTask         varchar;
    relatedDebitor      hs_office_debitor;
    relatedBookingItem  hs_booking_item;
begin
    currentTask := 'creating hosting-asset test-data ' || givenPartnerNumber::text || givenDebitorSuffix;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global:ADMIN');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select debitor.* into relatedDebitor
        from hs_office_debitor debitor
        join hs_office_relation debitorRel on debitorRel.uuid = debitor.debitorRelUuid
        join hs_office_relation partnerRel on partnerRel.holderUuid = debitorRel.anchorUuid
        join hs_office_partner partner on partner.partnerRelUuid = partnerRel.uuid
        where partner.partnerNumber = givenPartnerNumber and debitor.debitorNumberSuffix = givenDebitorSuffix;
    select item.* into relatedBookingItem
        from hs_booking_item item
        where item.debitoruuid = relatedDebitor.uuid
            and item.caption = 'some PrivateCloud';

    raise notice 'creating test hosting-asset: %', givenPartnerNumber::text || givenDebitorSuffix::text;
    raise notice '- using debitor (%): %', relatedDebitor.uuid, relatedDebitor;
    insert
        into hs_hosting_asset (uuid, bookingitemuuid, type, identifier,  caption, config)
        values (uuid_generate_v4(), relatedBookingItem.uuid, 'MANAGED_SERVER'::HsHostingAssetType,   'vm10' || givenDebitorSuffix, 'some ManagedServer', '{ "CPU": 2, "SDD": 512, "extra": 42 }'::jsonb),
               (uuid_generate_v4(), relatedBookingItem.uuid, 'CLOUD_SERVER'::HsHostingAssetType,     'vm20' || givenDebitorSuffix, 'another CloudServer', '{ "CPU": 2, "HDD": 1024, "extra": 42 }'::jsonb),
               (uuid_generate_v4(), relatedBookingItem.uuid, 'MANAGED_WEBSPACE'::HsHostingAssetType, givenWebspacePrefix || '01', 'some Webspace', '{ "RAM": 1, "SDD": 512, "HDD": 2048, "extra": 42 }'::jsonb);
end; $$;
--//


-- ============================================================================
--changeset hs-hosting-asset-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsHostingAssetTestData(10001, '11', 'aaa');
        call createHsHostingAssetTestData(10002, '12', 'bbb');
        call createHsHostingAssetTestData(10003, '13', 'ccc');
    end;
$$;
