--liquibase formatted sql

-- ============================================================================
--changeset hs-office-debitor-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_office_debitor
(
    uuid                    uuid unique references RbacObject (uuid) initially deferred,
    partnerUuid             uuid not null references hs_office_partner(uuid),
    billable                boolean not null default true,
    debitorNumberSuffix     numeric(2) not null,
    billingContactUuid      uuid not null references hs_office_contact(uuid),
    vatId                   varchar(24), -- TODO.spec: here or in person?
    vatCountryCode          varchar(2),
    vatBusiness             boolean not null,
    vatReverseCharge        boolean not null,
    refundBankAccountUuid   uuid references hs_office_bankaccount(uuid),
    defaultPrefix           char(3) not null unique
            constraint check_default_prefix check (
                defaultPrefix::text ~ '^([a-z]{3}|al0|bh1|c4s|f3k|k8i|l3d|mh1|o13|p2m|s80|t4w)$'
                )
    -- TODO.impl: SEPA-mandate
);
--//


-- ============================================================================
--changeset hs-office-debitor-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_debitor');
--//
