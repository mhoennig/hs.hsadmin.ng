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
    relatedProject      hs_booking_project;
    privateCloudUuid    uuid;
    managedServerUuid   uuid;
begin
    select project.* into relatedProject
                     from hs_booking_project project
                     where project.caption = 'D-' || givenPartnerNumber || givenDebitorSuffix || ' default project';

    raise notice 'creating test booking-item: %', givenPartnerNumber::text || givenDebitorSuffix::text;
    raise notice '- using project (%): %', relatedProject.uuid, relatedProject;
    privateCloudUuid := uuid_generate_v4();
    managedServerUuid := uuid_generate_v4();
    insert
        into hs_booking_item (uuid, projectuuid,            type,               parentitemuuid,     caption,                    validity,                           resources)
        values (privateCloudUuid,   relatedProject.uuid,    'PRIVATE_CLOUD',    null,               'some PrivateCloud',        daterange('20240401', null, '[]'),  '{ "CPU": 10, "RAM": 32, "SSD": 4000, "HDD": 10000, "Traffic": 2000 }'::jsonb),
               (uuid_generate_v4(), null,                   'MANAGED_SERVER',   privateCloudUuid,   'some ManagedServer',       daterange('20230115', '20240415',   '[)'), '{ "CPU": 2, "RAM": 4, "SSD": 500, "Traffic": 500 }'::jsonb),
               (uuid_generate_v4(), null,                   'CLOUD_SERVER',     privateCloudUuid,   'test CloudServer',         daterange('20230115', '20240415',   '[)'), '{ "CPU": 2, "RAM": 4, "SSD": 750, "Traffic": 500 }'::jsonb),
               (uuid_generate_v4(), null,                   'CLOUD_SERVER',     privateCloudUuid,   'prod CloudServer',         daterange('20230115', '20240415',   '[)'), '{ "CPU": 4, "RAM": 16, "SSD": 1000, "Traffic": 500 }'::jsonb),
               (managedServerUuid,  relatedProject.uuid,    'MANAGED_SERVER',   null,               'separate ManagedServer',   daterange('20221001', null, '[]'),  '{ "CPU": 2, "RAM": 8, "SSD": 500, "Traffic": 500 }'::jsonb),
               (uuid_generate_v4(), null,                   'MANAGED_WEBSPACE', managedServerUuid,  'some ManagedWebspace',     daterange('20221001', null, '[]'),  '{ "SSD": 50, "Traffic": 20, "Daemons": 2, "Multi": 4 }'::jsonb),
               (uuid_generate_v4(), relatedProject.uuid,    'MANAGED_WEBSPACE', null,               'separate ManagedWebspace', daterange('20221001', null, '[]'),  '{ "SSD": 100, "Traffic": 50, "Daemons": 0, "Multi": 1 }'::jsonb);
end; $$;
--//


-- ============================================================================
--changeset hs-booking-item-TEST-DATA-GENERATION:1 –context=dev,tc endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    declare
        currentTask text;
    begin
        call defineContext('creating booking-item test-data', null, 'superuser-alex@hostsharing.net', 'global#global:ADMIN');

        call createHsBookingItemTransactionTestData(10001, '11');
        call createHsBookingItemTransactionTestData(10002, '12');
        call createHsBookingItemTransactionTestData(10003, '13');
    end;
$$;
--//
