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
--changeset hs-office-coopshares-SHARE-COUNT-CONSTRAINT:1 endDelimiter:--//
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
--changeset hs-office-coopshares-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_coopsharestransaction');
--//
