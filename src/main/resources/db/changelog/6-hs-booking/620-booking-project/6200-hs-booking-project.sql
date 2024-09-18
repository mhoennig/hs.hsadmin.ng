--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:booking-project-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_booking_project
(
    uuid                uuid unique references rbac.object (uuid),
    version             int not null default 0,
    debitorUuid         uuid not null references hs_office.debitor(uuid),
    caption             varchar(80) not null
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-booking-project-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_booking_project');
--//


-- ============================================================================
--changeset michael.hoennig:hs-booking-project-MAIN-TABLE-HISTORIZATION endDelimiter:--//
-- ----------------------------------------------------------------------------
call base.tx_create_historicization('hs_booking_project');
--//
