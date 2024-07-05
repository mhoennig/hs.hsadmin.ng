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
    currentTask                         varchar;
    relatedProject                      hs_booking_project;
    relatedDebitor                      hs_office_debitor;
    relatedPrivateCloudBookingItem      hs_booking_item;
    relatedManagedServerBookingItem     hs_booking_item;
    relatedCloudServerBookingItem       hs_booking_item;
    relatedManagedWebspaceBookingItem   hs_booking_item;
    debitorNumberSuffix                 varchar;
    defaultPrefix                       varchar;
    managedServerUuid                   uuid;
    managedWebspaceUuid                 uuid;
    webUnixUserUuid                     uuid;
    domainSetupUuid                     uuid;
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

    select item.* into relatedCloudServerBookingItem
          from hs_booking_item item
          where item.parentItemuuid = relatedPrivateCloudBookingItem.uuid
            and item.type = 'CLOUD_SERVER';
    assert relatedCloudServerBookingItem.uuid is not null, 'relatedCloudServerBookingItem for "' || givenProjectCaption|| '" must not be null';

    select item.* into relatedManagedWebspaceBookingItem
          from hs_booking_item item
          where item.projectUuid = relatedProject.uuid
            and item.type = 'MANAGED_WEBSPACE';
    assert relatedManagedWebspaceBookingItem.uuid is not null, 'relatedManagedWebspaceBookingItem for "' || givenProjectCaption|| '" must not be null';

    select uuid_generate_v4() into managedServerUuid;
    select uuid_generate_v4() into managedWebspaceUuid;
    select uuid_generate_v4() into webUnixUserUuid;
    select uuid_generate_v4() into domainSetupUuid;
    debitorNumberSuffix := relatedDebitor.debitorNumberSuffix;
    defaultPrefix := relatedDebitor.defaultPrefix;

    insert into hs_hosting_asset
           (uuid,                bookingitemuuid,                        type,                parentAssetUuid,     assignedToAssetUuid,   identifier,                      caption,                     config)
    values (managedServerUuid,   relatedManagedServerBookingItem.uuid,   'MANAGED_SERVER',    null,                null,                  'vm10' || debitorNumberSuffix,   'some ManagedServer',        '{ "monit_max_cpu_usage": 90, "monit_max_ram_usage": 80, "monit_max_ssd_usage": 70 }'::jsonb),
           (uuid_generate_v4(),  relatedCloudServerBookingItem.uuid,     'CLOUD_SERVER',      null,                null,                  'vm20' || debitorNumberSuffix,   'another CloudServer',       '{}'::jsonb),
           (managedWebspaceUuid, relatedManagedWebspaceBookingItem.uuid, 'MANAGED_WEBSPACE',  managedServerUuid,   null,                  defaultPrefix || '01',           'some Webspace',             '{}'::jsonb),
           (uuid_generate_v4(),  null,                                   'EMAIL_ALIAS',       managedWebspaceUuid, null,                  defaultPrefix || '01-web',       'some E-Mail-Alias',         '{ "target": [ "office@example.org", "archive@example.com" ] }'::jsonb),
           (webUnixUserUuid,     null,                                   'UNIX_USER',         managedWebspaceUuid, null,                  defaultPrefix || '01-web',       'some UnixUser for Website', '{ "SSD-soft-quota": "128", "SSD-hard-quota": "256", "HDD-soft-quota": "512", "HDD-hard-quota": "1024"}'::jsonb),
           (domainSetupUuid,     null,                                   'DOMAIN_SETUP',      null,                null,                  defaultPrefix || '.example.org', 'some Domain-Setup',         '{}'::jsonb),
           (uuid_generate_v4(),  null,                                   'DOMAIN_DNS_SETUP',  domainSetupUuid,     null,                  defaultPrefix || '.example.org', 'some Domain-DNS-Setup',     '{}'::jsonb),
           (uuid_generate_v4(),  null,                                   'DOMAIN_HTTP_SETUP', domainSetupUuid,     webUnixUserUuid,       defaultPrefix || '.example.org', 'some Domain-HTTP-Setup',    '{ "option-htdocsfallback": true, "use-fcgiphpbin": "/usr/lib/cgi-bin/php", "validsubdomainnames": "*"}'::jsonb);
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
