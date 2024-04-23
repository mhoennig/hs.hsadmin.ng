--liquibase formatted sql

-- ============================================================================
--changeset hosting-asset-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create type HsHostingAssetType as enum (
    'CLOUD_SERVER',
    'MANAGED_SERVER',
    'MANAGED_WEBSPACE',
    'UNIX_USER',
    'DOMAIN_SETUP',
    'EMAIL_ALIAS',
    'EMAIL_ADDRESS',
    'PGSQL_USER',
    'PGSQL_DATABASE',
    'MARIADB_USER',
    'MARIADB_DATABASE'
);

CREATE CAST (character varying as HsHostingAssetType) WITH INOUT AS IMPLICIT;

create table if not exists hs_hosting_asset
(
    uuid                uuid unique references RbacObject (uuid),
    version             int not null default 0,
    bookingItemUuid     uuid not null references hs_booking_item(uuid),
    type                HsHostingAssetType,
    parentAssetUuid     uuid null references hs_hosting_asset(uuid),
    identifier          varchar(80) not null,
    caption             varchar(80) not null,
    config              jsonb not null
);
--//


-- ============================================================================
--changeset hs-hosting-asset-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_hosting_asset');
--//
