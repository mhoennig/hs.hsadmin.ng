--liquibase formatted sql

-- ============================================================================
--changeset hs-office-partner-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_office_partner
(
    uuid                uuid unique references RbacObject (uuid) initially deferred,
    personUuid          uuid not null references hs_office_person(uuid),
    contactUuid         uuid not null references hs_office_contact(uuid),
    registrationOffice  varchar(96),
    registrationNumber  varchar(96),
    birthName           varchar(96),
    birthday            date,
    dateOfDeath         date
);
--//
