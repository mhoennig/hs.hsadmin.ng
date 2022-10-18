--liquibase formatted sql

-- ============================================================================
--changeset hs-office-coopshares-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE HsOfficeCoopSharesTransactionType AS ENUM ('ADJUSTMENT', 'SUBSCRIPTION', 'CANCELLATION');

CREATE CAST (character varying as HsOfficeCoopSharesTransactionType) WITH INOUT AS IMPLICIT;

create table if not exists hs_office_coopsharestransaction
(
    uuid            uuid unique references RbacObject (uuid) initially deferred,
    membershipUuid  uuid not null references hs_office_membership(uuid),
    transactionType HsOfficeCoopSharesTransactionType not null,
    valueDate       date not null,
    shareCount      integer,
    reference       varchar(48),
    comment         varchar(512)
);
--//


-- ============================================================================
--changeset hs-office-coopshares-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_coopsharestransaction');
--//
