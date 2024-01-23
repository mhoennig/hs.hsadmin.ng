--liquibase formatted sql

-- ============================================================================
--changeset hs-office-membership-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE HsOfficeReasonForTermination AS ENUM ('NONE', 'CANCELLATION', 'TRANSFER', 'DEATH', 'LIQUIDATION', 'EXPULSION', 'UNKNOWN');

CREATE CAST (character varying as HsOfficeReasonForTermination) WITH INOUT AS IMPLICIT;

create table if not exists hs_office_membership
(
    uuid                    uuid unique references RbacObject (uuid) initially deferred,
    partnerUuid             uuid not null references hs_office_partner(uuid),
    mainDebitorUuid         uuid not null references hs_office_debitor(uuid),
    memberNumber            numeric(5) not null unique,
    validity                daterange not null,
    reasonForTermination    HsOfficeReasonForTermination not null default 'NONE',
    membershipFeeBillable   boolean not null default true
);
--//


-- ============================================================================
--changeset hs-office-membership-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_membership');
--//
