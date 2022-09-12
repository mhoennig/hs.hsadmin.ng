--liquibase formatted sql

-- ============================================================================
--changeset hs-admin-partner-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_admin_partner
(
    uuid                uuid unique references RbacObject (uuid) on delete cascade,
    personUuid          uuid not null references hs_admin_person(uuid),
    contactUuid         uuid not null references hs_admin_contact(uuid),
    registrationOffice  varchar(96),
    registrationNumber  varchar(96),
    birthName           varchar(96),
    birthday            date,
    dateOfDeath         date
);
--//
