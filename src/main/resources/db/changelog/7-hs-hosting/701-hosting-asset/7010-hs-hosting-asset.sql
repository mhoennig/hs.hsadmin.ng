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
--changeset hosting-asset-HIERARCHY-CHECK:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function hs_hosting_asset_type_hierarchy_check_tf()
    returns trigger
    language plpgsql as $$
declare
    actualParentType    HsHostingAssetType;
    expectedParentType  HsHostingAssetType;
begin
    if NEW.parentAssetUuid is not null then
        actualParentType := (select type
            from hs_hosting_asset
            where NEW.parentAssetUuid = uuid);
    end if;

    expectedParentType := (select case NEW.type
        when 'CLOUD_SERVER' then null
        when 'MANAGED_SERVER' then null
        when 'MANAGED_WEBSPACE' then 'MANAGED_SERVER'
        when 'UNIX_USER' then 'MANAGED_WEBSPACE'
        when 'DOMAIN_SETUP' then 'UNIX_USER'
        when 'EMAIL_ALIAS' then 'MANAGED_WEBSPACE'
        when 'EMAIL_ADDRESS' then 'DOMAIN_SETUP'
        when 'PGSQL_USER' then 'MANAGED_WEBSPACE'
        when 'PGSQL_DATABASE' then 'MANAGED_WEBSPACE'
        when 'MARIADB_USER' then 'MANAGED_WEBSPACE'
        when 'MARIADB_DATABASE' then 'MANAGED_WEBSPACE'
        else raiseException(format('[400] unknown asset type %s', NEW.type::text))
    end);

    if expectedParentType is not null and actualParentType is null then
        raise exception '[400] % must have % as parent, but got <NULL>',
            NEW.type, expectedParentType;
    elsif expectedParentType is not null and actualParentType <> expectedParentType then
        raise exception '[400] % must have % as parent, but got %s',
            NEW.type, expectedParentType, actualParentType;
    end if;
    return NEW;
end; $$;

create trigger hs_hosting_asset_type_hierarchy_check_tg
    before insert on hs_hosting_asset
    for each row
        execute procedure hs_hosting_asset_type_hierarchy_check_tf();
--//


-- ============================================================================
--changeset hs-hosting-asset-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_hosting_asset');
--//
