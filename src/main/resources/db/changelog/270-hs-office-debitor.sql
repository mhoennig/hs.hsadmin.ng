--liquibase formatted sql

-- ============================================================================
--changeset hs-office-debitor-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_office_debitor
(
    uuid                uuid unique references RbacObject (uuid) initially deferred,
    partnerUuid         uuid not null references hs_office_partner(uuid),
    debitorNumber       numeric(5) not null,
    billingContactUuid  uuid not null references hs_office_contact(uuid),
    vatId               varchar(24), -- TODO.spec: here or in person?
    vatCountryCode      varchar(2),
    vatBusiness         boolean not null -- TODO.spec: more of such?
    -- TODO.impl: SEPA-mandate + bank account
);
--//
