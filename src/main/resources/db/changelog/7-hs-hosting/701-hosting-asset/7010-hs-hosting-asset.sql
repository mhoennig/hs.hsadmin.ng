--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hosting-asset-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create type hs_hosting.AssetType as enum (
    'CLOUD_SERVER',
    'MANAGED_SERVER',
    'MANAGED_WEBSPACE',
    'UNIX_USER',
    'DOMAIN_SETUP',
    'DOMAIN_DNS_SETUP',
    'DOMAIN_HTTP_SETUP',
    'DOMAIN_SMTP_SETUP',
    'DOMAIN_MBOX_SETUP',
    'EMAIL_ALIAS',
    'EMAIL_ADDRESS',
    'PGSQL_INSTANCE',
    'PGSQL_USER',
    'PGSQL_DATABASE',
    'MARIADB_INSTANCE',
    'MARIADB_USER',
    'MARIADB_DATABASE',
    'IPV4_NUMBER',
    'IPV6_NUMBER'
);

CREATE CAST (character varying as hs_hosting.AssetType) WITH INOUT AS IMPLICIT;

create table if not exists hs_hosting.asset
(
    uuid                uuid unique references rbac.object (uuid),
    version             int not null default 0,
    bookingItemUuid     uuid null references hs_booking.item(uuid),
    type                hs_hosting.AssetType not null,
    parentAssetUuid     uuid null references hs_hosting.asset(uuid) initially deferred,
    assignedToAssetUuid uuid null references hs_hosting.asset(uuid) initially deferred,
    identifier          varchar(80) not null,
    caption             varchar(80),
    config              jsonb not null,
    alarmContactUuid    uuid null references hs_office.contact(uuid) initially deferred,

    constraint hosting_asset_has_booking_item_or_parent_asset
        check (bookingItemUuid is not null or parentAssetUuid is not null or type in ('DOMAIN_SETUP', 'IPV4_NUMBER', 'IPV6_NUMBER'))
);
--//


-- ============================================================================
--changeset michael.hoennig:hosting-asset-TYPE-HIERARCHY-CHECK endDelimiter:--//
-- ----------------------------------------------------------------------------

-- TODO.impl: this could be generated from HsHostingAssetType
--  also including a check for assignedToAssetUuud

create or replace function hs_hosting.asset_type_hierarchy_check_tf()
    returns trigger
    language plpgsql as $$
declare
    actualParentType    hs_hosting.AssetType;
    expectedParentType  hs_hosting.AssetType;
begin
    if NEW.parentAssetUuid is not null then
        actualParentType := (select type
            from hs_hosting.asset
            where NEW.parentAssetUuid = uuid);
    end if;

    expectedParentType := (select case NEW.type
        when 'CLOUD_SERVER' then null
        when 'MANAGED_SERVER' then null
        when 'MANAGED_WEBSPACE' then 'MANAGED_SERVER'
        when 'UNIX_USER' then 'MANAGED_WEBSPACE'
        when 'EMAIL_ALIAS' then 'MANAGED_WEBSPACE'
        when 'DOMAIN_SETUP' then null
        when 'DOMAIN_DNS_SETUP' then 'DOMAIN_SETUP'
        when 'DOMAIN_HTTP_SETUP' then 'DOMAIN_SETUP'
        when 'DOMAIN_SMTP_SETUP' then 'DOMAIN_SETUP'
        when 'DOMAIN_MBOX_SETUP' then 'DOMAIN_SETUP'
        when 'EMAIL_ADDRESS' then 'DOMAIN_MBOX_SETUP'

        when 'PGSQL_INSTANCE' then 'MANAGED_SERVER'
        when 'PGSQL_USER' then 'MANAGED_WEBSPACE'
        when 'PGSQL_DATABASE' then 'PGSQL_USER'

        when 'MARIADB_INSTANCE' then 'MANAGED_SERVER'
        when 'MARIADB_USER' then 'MANAGED_WEBSPACE'
        when 'MARIADB_DATABASE' then 'MARIADB_USER'

        when 'IPV4_NUMBER' then null
        when 'IPV6_NUMBER' then null

        else base.raiseException(format('[400] unknown asset type %s', NEW.type::text))
    end);

    if expectedParentType is not null and actualParentType is null then
        raise exception '[400] HostingAsset % must have % as parent, but got <NULL>',
            NEW.type, expectedParentType;
    elsif expectedParentType is not null and actualParentType <> expectedParentType then
        raise exception '[400] HostingAsset % must have % as parent, but got %s',
            NEW.type, expectedParentType, actualParentType;
    end if;
    return NEW;
end; $$;

create trigger hosting_asset_type_hierarchy_check_tg
    before insert on hs_hosting.asset
    for each row
        execute procedure hs_hosting.asset_type_hierarchy_check_tf();
--//



-- ============================================================================
--changeset michael.hoennig:hosting-asset-system-sequences endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS hs_hosting.asset_unixuser_system_id_seq
    AS integer
    MINVALUE 1000000
    MAXVALUE 9999999
    NO CYCLE
    OWNED BY NONE;

--//


-- ============================================================================
--changeset michael.hoennig:hosting-asset-BOOKING-ITEM-HIERARCHY-CHECK endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function hs_hosting.asset_booking_item_hierarchy_check_tf()
    returns trigger
    language plpgsql as $$
declare
    actualBookingItemType       hs_booking.ItemType;
    expectedBookingItemType     hs_booking.ItemType;
begin
    actualBookingItemType := (select type
                                 from hs_booking.item
                                 where NEW.bookingItemUuid = uuid);

    if NEW.type = 'CLOUD_SERVER' then
        expectedBookingItemType := 'CLOUD_SERVER';
    elsif NEW.type = 'MANAGED_SERVER' then
        expectedBookingItemType := 'MANAGED_SERVER';
    elsif NEW.type = 'MANAGED_WEBSPACE' then
        expectedBookingItemType := 'MANAGED_WEBSPACE';
    end if;

    if not actualBookingItemType = expectedBookingItemType then
        raise exception '[400] HostingAsset % % must have % as booking-item, but got %',
            NEW.type, NEW.identifier, expectedBookingItemType, actualBookingItemType;
    end if;
    return NEW;
end; $$;

create trigger hosting_asset_booking_item_hierarchy_check_tg
    before insert on hs_hosting.asset
    for each row
execute procedure hs_hosting.asset_booking_item_hierarchy_check_tf();
--//


-- ============================================================================
--changeset michael.hoennig:hs-hosting-asset-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------
call base.create_journal('hs_hosting.asset');
--//


-- ============================================================================
--changeset michael.hoennig:hs-hosting-asset-MAIN-TABLE-HISTORIZATION endDelimiter:--//
-- ----------------------------------------------------------------------------
call base.tx_create_historicization('hs_hosting.asset');
--//


