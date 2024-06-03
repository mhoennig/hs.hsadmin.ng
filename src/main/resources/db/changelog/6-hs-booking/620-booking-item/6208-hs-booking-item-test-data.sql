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
    relatedProject      hs_booking_project;
    privateCloudUuid    uuid;
    managedServerUuid   uuid;
begin
    currentTask := 'creating booking-item test-data ' || givenPartnerNumber::text || givenDebitorSuffix;
    call defineContext(currentTask, null, 'superuser-alex@hostsharing.net', 'global#global:ADMIN');
    execute format('set local hsadminng.currentTask to %L', currentTask);

    select project.* into relatedProject
                     from hs_booking_project project
                     where project.caption = 'D-' || givenPartnerNumber || givenDebitorSuffix || ' default project';

    raise notice 'creating test booking-item: %', givenPartnerNumber::text || givenDebitorSuffix::text;
    raise notice '- using project (%): %', relatedProject.uuid, relatedProject;
    privateCloudUuid := uuid_generate_v4();
    managedServerUuid := uuid_generate_v4();
    insert
        into hs_booking_item (uuid, projectuuid,            type,               parentitemuuid,     caption,                    validity,                           resources)
        values (privateCloudUuid,   relatedProject.uuid,    'PRIVATE_CLOUD',    null,               'some PrivateCloud',        daterange('20240401', null, '[]'),  '{ "CPUs": 10, "SDD": 10240, "HDD": 10240, "Traffic": 42 }'::jsonb),
               (uuid_generate_v4(), null,                   'MANAGED_SERVER',   privateCloudUuid,   'some ManagedServer',       daterange('20230115', '20240415',   '[)'), '{ "CPUs": 2, "RAM": 4, "HDD": 1024, "Traffic": 42 }'::jsonb),
               (uuid_generate_v4(), null,                   'CLOUD_SERVER',     privateCloudUuid,   'test CloudServer',         daterange('20230115', '20240415',   '[)'), '{ "CPUs": 2, "RAM": 4, "HDD": 1024, "Traffic": 42 }'::jsonb),
               (uuid_generate_v4(), null,                   'CLOUD_SERVER',     privateCloudUuid,   'prod CloudServer',         daterange('20230115', '20240415',   '[)'), '{ "CPUs": 4, "RAM": 16, "HDD": 2924, "Traffic": 420 }'::jsonb),
               (managedServerUuid,  relatedProject.uuid,    'MANAGED_SERVER',   null,               'separate ManagedServer',   daterange('20221001', null, '[]'),  '{ "CPUs": 2, "RAM": 8, "SDD": 512, "Traffic": 42 }'::jsonb),
               (uuid_generate_v4(), null,                   'MANAGED_WEBSPACE', managedServerUuid,  'some ManagedWebspace',     daterange('20221001', null, '[]'),  '{ "SDD": 512, "Traffic": 12, "Daemons": 2, "Multi": 4 }'::jsonb),
               (uuid_generate_v4(), relatedProject.uuid,    'MANAGED_WEBSPACE', null,               'some ManagedWebspace',     daterange('20221001', null, '[]'),  '{ "SDD": 512, "Traffic": 12, "Daemons": 2, "Multi": 4 }'::jsonb);
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
