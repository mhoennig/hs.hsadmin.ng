--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE HsOfficeCoopSharesTransactionType AS ENUM ('ADJUSTMENT', 'SUBSCRIPTION', 'CANCELLATION');

CREATE CAST (character varying as HsOfficeCoopSharesTransactionType) WITH INOUT AS IMPLICIT;

create table if not exists hs_office_coopsharestransaction
(
    uuid            uuid unique references rbac.object (uuid) initially deferred,
    version         int not null default 0,
    membershipUuid  uuid not null references hs_office_membership(uuid),
    transactionType HsOfficeCoopSharesTransactionType not null,
    valueDate       date not null,
    shareCount      integer not null,
    reference       varchar(48) not null,
    adjustedShareTxUuid uuid unique REFERENCES hs_office_coopsharestransaction(uuid) DEFERRABLE INITIALLY DEFERRED,
    comment         varchar(512)
);
--//

-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-BUSINESS-RULES endDelimiter:--//
-- ----------------------------------------------------------------------------

alter table hs_office_coopsharestransaction
    add constraint hs_office_coopsharestransaction_reverse_entry_missing
        check ( transactionType = 'ADJUSTMENT' and adjustedShareTxUuid is not null
             or transactionType <> 'ADJUSTMENT' and adjustedShareTxUuid is null);
--//

-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-SHARE-COUNT-CONSTRAINT endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function checkSharesByMembershipUuid(forMembershipUuid UUID, newShareCount integer)
returns boolean
language plpgsql as $$
declare
    currentShareCount integer;
    totalShareCount integer;
begin
    select sum(cst.shareCount)
    from hs_office_coopsharestransaction cst
    where cst.membershipUuid = forMembershipUuid
    into currentShareCount;
    totalShareCount := currentShareCount + newShareCount;
    if totalShareCount < 0 then
        raise exception '[400] coop shares transaction would result in a negative number of shares';
    end if;
    return true;
end; $$;

alter table hs_office_coopsharestransaction
    add constraint hs_office_coopshares_positive
        check ( checkSharesByMembershipUuid(membershipUuid, shareCount) );

--//

-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office_coopsharestransaction');
--//
