--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-hosting-asset-TEST-DATA-GENERATOR endDelimiter:--//
-- ----------------------------------------------------------------------------

/*
    Creates a single hs_hosting.asset test record.
 */
create or replace procedure hs_hosting.asset_create_test_data(givenProjectCaption varchar)
    language plpgsql as $$
declare
    relatedProject                      hs_booking.project;
    relatedDebitor                      hs_office.debitor;
    privateCloudBI                      hs_booking.item;
    managedServerBI                     hs_booking.item;
    cloudServerBI                       hs_booking.item;
    managedWebspaceBI                   hs_booking.item;
    debitorNumberSuffix                 varchar;
    defaultPrefix                       varchar;
    managedServerUuid                   uuid;
    managedWebspaceUuid                 uuid;
    webUnixSubjectUuid                  uuid;
    mboxUnixSubjectUuid                 uuid;
    domainSetupUuid                     uuid;
    domainMBoxSetupUuid                 uuid;
    mariaDbInstanceUuid                 uuid;
    mariaDbSubjectUuid                     uuid;
    pgSqlInstanceUuid                   uuid;
    PgSqlSubjectUuid                       uuid;
begin
    call base.defineContext('creating hosting-asset test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

    select project.* into relatedProject
                  from hs_booking.project project
                  where project.caption = givenProjectCaption;
    assert relatedProject.uuid is not null, 'relatedProject for "' || givenProjectCaption || '" must not be null';

    select debitor.* into relatedDebitor
                     from hs_office.debitor debitor
                     where debitor.uuid = relatedProject.debitorUuid;
    assert relatedDebitor.uuid is not null, 'relatedDebitor for "' || givenProjectCaption || '" must not be null';

    select item.* into privateCloudBI
        from hs_booking.item item
       where item.projectUuid = relatedProject.uuid
          and item.type = 'PRIVATE_CLOUD';
    assert privateCloudBI.uuid is not null, 'relatedPrivateCloudBookingItem for "' || givenProjectCaption|| '" must not be null';

    select item.* into managedServerBI
          from hs_booking.item item
          where item.projectUuid = relatedProject.uuid
            and item.type = 'MANAGED_SERVER';
    assert managedServerBI.uuid is not null, 'relatedManagedServerBookingItem for "' || givenProjectCaption|| '" must not be null';

    select item.* into cloudServerBI
          from hs_booking.item item
          where item.parentItemuuid = privateCloudBI.uuid
            and item.type = 'CLOUD_SERVER';
    assert cloudServerBI.uuid is not null, 'relatedCloudServerBookingItem for "' || givenProjectCaption|| '" must not be null';

    select item.* into managedWebspaceBI
          from hs_booking.item item
          where item.projectUuid = relatedProject.uuid
            and item.type = 'MANAGED_WEBSPACE';
    assert managedWebspaceBI.uuid is not null, 'relatedManagedWebspaceBookingItem for "' || givenProjectCaption|| '" must not be null';

    select uuid_generate_v4() into managedServerUuid;
    select uuid_generate_v4() into managedWebspaceUuid;
    select uuid_generate_v4() into webUnixSubjectUuid;
    select uuid_generate_v4() into mboxUnixSubjectUuid;
    select uuid_generate_v4() into domainSetupUuid;
    select uuid_generate_v4() into domainMBoxSetupUuid;
    select uuid_generate_v4() into mariaDbInstanceUuid;
    select uuid_generate_v4() into mariaDbSubjectUuid;
    select uuid_generate_v4() into pgSqlInstanceUuid;
    select uuid_generate_v4() into pgSqlSubjectUuid;
    debitorNumberSuffix := relatedDebitor.debitorNumberSuffix;
    defaultPrefix := relatedDebitor.defaultPrefix;

    insert into hs_hosting.asset
       (uuid,                   bookingitemuuid,        type,                parentAssetUuid,     assignedToAssetUuid,   identifier,                                         caption,                        config)
       values
       (managedServerUuid,      managedServerBI.uuid,   'MANAGED_SERVER',    null,                null,                  'vm10' || debitorNumberSuffix,                       'some ManagedServer',           '{ "monit_max_cpu_usage": 90, "monit_max_ram_usage": 80, "monit_max_ssd_usage": 70 }'::jsonb),
       (uuid_generate_v4(),     cloudServerBI.uuid,     'CLOUD_SERVER',      null,                null,                  'vm20' || debitorNumberSuffix,                       'another CloudServer',          '{}'::jsonb),
       (managedWebspaceUuid,    managedWebspaceBI.uuid, 'MANAGED_WEBSPACE',  managedServerUuid,   null,                  defaultPrefix || '01',                               'some Webspace',                '{}'::jsonb),
       (mariaDbInstanceUuid,    null,                   'MARIADB_INSTANCE',  managedServerUuid,   null,                  'vm10' || debitorNumberSuffix || '.MariaDB.default', 'some default MariaDB instance','{}'::jsonb),
       (mariaDbSubjectUuid,        null,                   'MARIADB_USER',      managedWebspaceUuid, mariaDbInstanceUuid,   defaultPrefix || '01_web',                           'some default MariaDB user',    '{ "password": "<TODO:replace-by-encrypted-mariadb-password"}'::jsonb ),
       (uuid_generate_v4(),     null,                   'MARIADB_DATABASE',  mariaDbSubjectUuid,     mariaDbInstanceUuid,   defaultPrefix || '01_web',                           'some default MariaDB database','{ "encryption": "utf8", "collation": "utf8"}'::jsonb ),
       (pgSqlInstanceUuid,      null,                   'PGSQL_INSTANCE',    managedServerUuid,   null,                  'vm10' || debitorNumberSuffix || '.Postgresql.default', 'some default Postgresql instance','{}'::jsonb),
       (PgSqlSubjectUuid,          null,                   'PGSQL_USER',        managedWebspaceUuid, pgSqlInstanceUuid,     defaultPrefix || '01_web',                           'some default Postgresql user',    '{ "password": "<TODO:replace-by-encrypted-postgresql-password"}'::jsonb ),
       (uuid_generate_v4(),     null,                   'PGSQL_DATABASE',    pgSqlSubjectUuid,       pgSqlInstanceUuid,     defaultPrefix || '01_web',                           'some default Postgresql database','{ "encryption": "utf8", "collation": "utf8"}'::jsonb ),
       (uuid_generate_v4(),     null,                   'EMAIL_ALIAS',       managedWebspaceUuid, null,                  defaultPrefix || '01-web',                           'some E-Mail-Alias',            '{ "target": [ "office@example.org", "archive@example.com" ] }'::jsonb),
       (webUnixSubjectUuid,        null,                   'UNIX_USER',         managedWebspaceUuid, null,                  defaultPrefix || '01-web',                           'some UnixUser for Website',    '{ "SSD-soft-quota": "128", "SSD-hard-quota": "256", "HDD-soft-quota": "512", "HDD-hard-quota": "1024"}'::jsonb),
       (mboxUnixSubjectUuid,       null,                   'UNIX_USER',         managedWebspaceUuid, null,                  defaultPrefix || '01-mbox',                          'some UnixUser for E-Mail',     '{ "SSD-soft-quota": "128", "SSD-hard-quota": "256", "HDD-soft-quota": "512", "HDD-hard-quota": "1024"}'::jsonb),
       (domainSetupUuid,        null,                   'DOMAIN_SETUP',      null,                null,                  defaultPrefix || '.example.org',                     'some Domain-Setup',            '{}'::jsonb),
       (uuid_generate_v4(),     null,                   'DOMAIN_DNS_SETUP',  domainSetupUuid,     null,                  defaultPrefix || '.example.org|DNS',                 'some Domain-DNS-Setup',        '{}'::jsonb),
       (uuid_generate_v4(),     null,                   'DOMAIN_HTTP_SETUP', domainSetupUuid,     webUnixSubjectUuid,       defaultPrefix || '.example.org|HTTP',                'some Domain-HTTP-Setup',       '{ "option-htdocsfallback": true, "use-fcgiphpbin": "/usr/lib/cgi-bin/php", "validsubdomainnames": "*"}'::jsonb),
       (uuid_generate_v4(),     null,                   'DOMAIN_SMTP_SETUP', domainSetupUuid,     managedWebspaceUuid,   defaultPrefix || '.example.org|SMTP',                'some Domain-SMTP-Setup',       '{}'::jsonb),
       (domainMBoxSetupUuid,    null,                   'DOMAIN_MBOX_SETUP', domainSetupUuid,     managedWebspaceUuid,   defaultPrefix || '.example.org|MBOX',                'some Domain-MBOX-Setup',       '{}'::jsonb),
       (uuid_generate_v4(),     null,                   'EMAIL_ADDRESS',     domainMBoxSetupUuid, null,                  'test@' || defaultPrefix || '.example.org',          'some E-Mail-Address',          '{}'::jsonb);
end; $$;
--//


-- ============================================================================
--changeset michael.hoennig:hs-hosting-asset-TEST-DATA-GENERATION context:!without-test-data endDelimiter:--//
-- ----------------------------------------------------------------------------

do language plpgsql $$
    begin
        call base.defineContext('creating hosting-asset test-data', null, 'superuser-alex@hostsharing.net', 'rbac.global#global:ADMIN');

        call hs_hosting.asset_create_test_data('D-1000111 default project');
        call hs_hosting.asset_create_test_data('D-1000212 default project');
        call hs_hosting.asset_create_test_data('D-1000313 default project');
    end;
$$;
--//
