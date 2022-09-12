--liquibase formatted sql

-- ============================================================================
--changeset hs-admin-contact-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_admin_contact
(
    uuid           uuid unique references RbacObject (uuid) on delete cascade,
    label          varchar(96) not null,
    postalAddress  text,
    emailAddresses text, -- TODO.feat: change to json
    phoneNumbers   text  -- TODO.feat: change to json
);
--//
