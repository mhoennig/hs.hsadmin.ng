--liquibase formatted sql

-- ============================================================================
--changeset booking-project-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_booking_project
(
    uuid                uuid unique references RbacObject (uuid),
    version             int not null default 0,
    debitorUuid         uuid not null references hs_office_debitor(uuid),
    caption             varchar(80) not null
);
--//


-- ============================================================================
--changeset hs-booking-project-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_booking_project');
--//
