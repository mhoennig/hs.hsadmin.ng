--liquibase formatted sql


-- ============================================================================
--changeset hs-office-partner-DETAILS-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_office_partner_details
(
    uuid                uuid unique references RbacObject (uuid) initially deferred,
    registrationOffice  varchar(96),
    registrationNumber  varchar(96),
    birthName           varchar(96),
    birthday            date,
    dateOfDeath         date
);
--//


-- ============================================================================
--changeset hs-office-partner-DETAILS-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_partner_details');
--//

-- ============================================================================
--changeset hs-office-partner-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_office_partner
(
    uuid                uuid unique references RbacObject (uuid) initially deferred,
    personUuid          uuid not null references hs_office_person(uuid),
    contactUuid         uuid not null references hs_office_contact(uuid),
    detailsUuid         uuid not null references hs_office_partner_details(uuid) on delete cascade
);
--//


-- ============================================================================
--changeset hs-office-partner-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_partner');
--//
