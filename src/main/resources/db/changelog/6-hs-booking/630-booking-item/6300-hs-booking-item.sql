--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:booking-item-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create type hs_booking.ItemType as enum (
    'PRIVATE_CLOUD',
    'CLOUD_SERVER',
    'MANAGED_SERVER',
    'MANAGED_WEBSPACE',
    'DOMAIN_SETUP'
    );

CREATE CAST (character varying as hs_booking.ItemType) WITH INOUT AS IMPLICIT;

create table if not exists hs_booking.item
(
    uuid                uuid unique references rbac.object (uuid),
    version             int not null default 0,
    projectUuid         uuid null references hs_booking.project(uuid),
    type                hs_booking.ItemType not null,
    parentItemUuid      uuid null references hs_booking.item(uuid) initially deferred,
    validity            daterange not null,
    caption             varchar(80) not null,
    resources           jsonb not null,

    constraint booking_item_has_project_or_parent_asset
        check (projectUuid is not null or parentItemUuid is not null)
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-booking-item-EVENT-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_booking.item_created_event
(
    bookingItemUuid     uuid unique references hs_booking.item (uuid),
    version             int not null default 0,
    assetJson           text,
    statusMessage       text
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-booking-item-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_booking.item');
--//


-- ============================================================================
--changeset michael.hoennig:hs-booking-item-MAIN-TABLE-HISTORIZATION endDelimiter:--//
-- ----------------------------------------------------------------------------
call base.tx_create_historicization('hs_booking.item');
--//

