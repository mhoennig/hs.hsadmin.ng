--liquibase formatted sql

-- ============================================================================
--changeset hs-office-contact-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_office_contact
(
    uuid           uuid unique references RbacObject (uuid) initially deferred,
    version        int not null default 0,
    label          varchar(128) not null,
    postalAddress  text,
    emailAddresses text, -- TODO.feat: change to json
    phoneNumbers   text  -- TODO.feat: change to json
);
--//


-- ============================================================================
--changeset hs-office-contact-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_contact');
--//
