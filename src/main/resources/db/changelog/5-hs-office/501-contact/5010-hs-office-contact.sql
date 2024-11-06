--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-contact-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_office.contact
(
    uuid           uuid unique references rbac.object (uuid) initially deferred,
    version        int not null default 0,
    caption        varchar(128) not null,
    postalAddress  jsonb not null,
    emailAddresses jsonb not null,
    phoneNumbers   jsonb not null
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-contact-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office.contact');
--//
