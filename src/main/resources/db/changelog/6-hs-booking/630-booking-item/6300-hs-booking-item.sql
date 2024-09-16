--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:booking-item-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create type HsBookingItemType as enum (
    'PRIVATE_CLOUD',
    'CLOUD_SERVER',
    'MANAGED_SERVER',
    'MANAGED_WEBSPACE',
    'DOMAIN_SETUP'
    );

CREATE CAST (character varying as HsBookingItemType) WITH INOUT AS IMPLICIT;

create table if not exists hs_booking_item
(
    uuid                uuid unique references rbac.object (uuid),
    version             int not null default 0,
    projectUuid         uuid null references hs_booking_project(uuid),
    type                HsBookingItemType not null,
    parentItemUuid      uuid null references hs_booking_item(uuid) initially deferred,
    validity            daterange not null,
    caption             varchar(80) not null,
    resources           jsonb not null,

    constraint chk_hs_booking_item_has_project_or_parent_asset
        check (projectUuid is not null or parentItemUuid is not null)
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-booking-item-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_booking_item');
--//


-- ============================================================================
--changeset michael.hoennig:hs-booking-item-MAIN-TABLE-HISTORIZATION endDelimiter:--//
-- ----------------------------------------------------------------------------
call base.tx_create_historicization('hs_booking_item');
--//

