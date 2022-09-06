--liquibase formatted sql

-- ============================================================================
--changeset hs-admin-contact-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table if not exists hs_admin_contact
(
    uuid           uuid unique references RbacObject (uuid),
    label          varchar(96) not null,
    postalAddress  text,
    emailAddresses text, -- TODO: change to json
    phoneNumbers   text  -- TODO: change to json
);
--//
