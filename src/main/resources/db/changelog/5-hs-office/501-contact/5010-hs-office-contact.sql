--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-contact-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_office_contact
(
    uuid           uuid unique references rbac.object (uuid) initially deferred,
    version        int not null default 0,
    caption        varchar(128) not null,
    postalAddress  text,
    emailAddresses jsonb not null,
    phoneNumbers   jsonb not null
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-contact-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office_contact');
--//
