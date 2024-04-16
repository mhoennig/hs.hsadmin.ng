--liquibase formatted sql

-- ============================================================================
--changeset booking-item-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_booking_item
(
    uuid                uuid unique references RbacObject (uuid),
    version             int not null default 0,
    debitorUuid         uuid not null references hs_office_debitor(uuid),
    validity            daterange not null,
    caption             varchar(80) not null,
    resources           jsonb not null
);
--//


-- ============================================================================
--changeset hs-booking-item-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_booking_item');
--//
