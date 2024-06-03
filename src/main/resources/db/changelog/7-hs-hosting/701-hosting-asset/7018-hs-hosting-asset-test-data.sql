--liquibase formatted sql


-- ============================================================================
--changeset hs-hosting-asset-TEST-DATA-GENERATOR:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single hs_hosting_asset test record.
 */
create or replace procedure createHsHostingAssetTestData(givenProjectCaption varchar)
    language plpgsql as $$
declare
    currentTask                     varchar;
    relatedProject                  hs_booking_project;
    relatedDebitor                  hs_office_debitor;
    relatedPrivateCloudBookingItem  hs_booking_item;
    relatedManagedServerBookingItem hs_booking_item;
    managedServerUuid               uuid;
begin
    currentTask := 'creating hosting-asset test-data ' || givenProjectCaption;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global:ADMIN');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select project.* into relatedProject
                  from hs_booking_project project
                  where project.caption = givenProjectCaption;
    assert relatedProject.uuid is not null, 'relatedProject for "' || givenProjectCaption || '" must not be null';

    select debitor.* into relatedDebitor
                     from hs_office_debitor debitor
                     where debitor.uuid = relatedProject.debitorUuid;
    assert relatedDebitor.uuid is not null, 'relatedDebitor for "' || givenProjectCaption || '" must not be null';

    select item.* into relatedPrivateCloudBookingItem
        from hs_booking_item item
        where item.projectUuid = relatedProject.uuid
          and item.type = 'PRIVATE_CLOUD';
    assert relatedPrivateCloudBookingItem.uuid is not null, 'relatedPrivateCloudBookingItem for "' || givenProjectCaption|| '" must not be null';

    select item.* into relatedManagedServerBookingItem
          from hs_booking_item item
          where item.projectUuid = relatedProject.uuid
            and item.type = 'MANAGED_SERVER';
    assert relatedManagedServerBookingItem.uuid is not null, 'relatedManagedServerBookingItem for "' || givenProjectCaption|| '" must not be null';

    select uuid_generate_v4() into managedServerUuid;

    insert into hs_hosting_asset
           (uuid,               bookingitemuuid,                      type,               parentAssetUuid,   identifier,                                    caption,                config)
    values (managedServerUuid,  relatedPrivateCloudBookingItem.uuid,  'MANAGED_SERVER',   null,             'vm10' || relatedDebitor.debitorNumberSuffix,   'some ManagedServer',   '{ "CPU": 2, "SDD": 512, "extra": 42 }'::jsonb),
           (uuid_generate_v4(), relatedPrivateCloudBookingItem.uuid,  'CLOUD_SERVER',     null,              'vm20' || relatedDebitor.debitorNumberSuffix,  'another CloudServer',  '{ "CPU": 2, "HDD": 1024, "extra": 42 }'::jsonb),
           (uuid_generate_v4(), relatedManagedServerBookingItem.uuid, 'MANAGED_WEBSPACE', managedServerUuid, relatedDebitor.defaultPrefix || '01',          'some Webspace',        '{ "RAM": 1, "SDD": 512, "HDD": 2048, "extra": 42 }'::jsonb);
end; $$;
--//


-- ============================================================================
--changeset hs-hosting-asset-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call createHsHostingAssetTestData('D-1000111 default project');
        call createHsHostingAssetTestData('D-1000212 default project');
        call createHsHostingAssetTestData('D-1000313 default project');
    end;
$$;
--//
