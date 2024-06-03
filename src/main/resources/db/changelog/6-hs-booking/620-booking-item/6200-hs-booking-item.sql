--liquibase formatted sql

-- ============================================================================
--changeset booking-item-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create type HsBookingItemType as enum (
    'PRIVATE_CLOUD',
    'CLOUD_SERVER',
    'MANAGED_SERVER',
    'MANAGED_WEBSPACE'
    );

CREATE CAST (character varying as HsBookingItemType) WITH INOUT AS IMPLICIT;

create table if not exists hs_booking_item
(
    uuid                uuid unique references RbacObject (uuid),
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
--changeset hs-booking-item-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_booking_item');
--//
