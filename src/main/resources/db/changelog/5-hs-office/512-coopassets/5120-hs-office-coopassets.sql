--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-coopassets-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE HsOfficeCoopAssetsTransactionType AS ENUM ('ADJUSTMENT',
                                                       'DEPOSIT',
                                                       'DISBURSAL',
                                                       'TRANSFER',
                                                       'ADOPTION',
                                                       'CLEARING',
                                                       'LOSS',
                                                       'LIMITATION');

CREATE CAST (character varying as HsOfficeCoopAssetsTransactionType) WITH INOUT AS IMPLICIT;

create table if not exists hs_office_coopassetstransaction
(
    uuid                uuid unique references rbac.object (uuid) initially deferred,
    version             int not null default 0,
    membershipUuid      uuid not null references hs_office_membership(uuid),
    transactionType     HsOfficeCoopAssetsTransactionType not null,
    valueDate           date not null,
    assetValue          money not null,
    reference           varchar(48) not null,
    adjustedAssetTxUuid uuid unique REFERENCES hs_office_coopassetstransaction(uuid) DEFERRABLE INITIALLY DEFERRED,
    comment             varchar(512)
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-coopassets-BUSINESS-RULES endDelimiter:--//
-- ----------------------------------------------------------------------------

alter table hs_office_coopassetstransaction
    add constraint hs_office_coopassetstransaction_reverse_entry_missing
        check ( transactionType = 'ADJUSTMENT' and adjustedAssetTxUuid is not null
             or transactionType <> 'ADJUSTMENT' and adjustedAssetTxUuid is null);
--//

-- ============================================================================
--changeset michael.hoennig:hs-office-coopassets-ASSET-VALUE-CONSTRAINT endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function checkAssetsByMembershipUuid(forMembershipUuid UUID, newAssetValue money)
returns boolean
language plpgsql as $$
declare
    currentAssetValue money;
    totalAssetValue money;
begin
    select sum(cat.assetValue)
        from hs_office_coopassetstransaction cat
        where cat.membershipUuid = forMembershipUuid
        into currentAssetValue;
    totalAssetValue := currentAssetValue + newAssetValue;
    if totalAssetValue::numeric < 0 then
        raise exception '[400] coop assets transaction would result in a negative balance of assets';
    end if;
    return true;
end; $$;

alter table hs_office_coopassetstransaction
    add constraint hs_office_coopassets_positive
        check ( checkAssetsByMembershipUuid(membershipUuid, assetValue) );
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-coopassets-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office_coopassetstransaction');
--//
