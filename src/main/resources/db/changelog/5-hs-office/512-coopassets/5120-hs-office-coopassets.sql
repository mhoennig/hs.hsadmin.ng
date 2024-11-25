--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-coopassets-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE hs_office.CoopAssetsTransactionType AS ENUM ('REVERSAL',
                                                       'DEPOSIT',
                                                       'DISBURSAL',
                                                       'TRANSFER',
                                                       'ADOPTION',
                                                       'CLEARING',
                                                       'LOSS',
                                                       'LIMITATION');

CREATE CAST (character varying as hs_office.CoopAssetsTransactionType) WITH INOUT AS IMPLICIT;

create table if not exists hs_office.coopassettx
(
    uuid                uuid unique references rbac.object (uuid) initially deferred,
    version             int not null default 0,
    membershipUuid      uuid not null references hs_office.membership(uuid),
    transactionType     hs_office.CoopAssetsTransactionType not null,
    valueDate           date not null,
    assetValue          numeric(12,2) not null, -- https://wiki.postgresql.org/wiki/Don't_Do_This#Don.27t_use_money
    reference           varchar(48) not null,
    revertedAssetTxUuid uuid unique REFERENCES hs_office.coopassettx(uuid) DEFERRABLE INITIALLY DEFERRED,
    assetAdoptionTxUuid uuid unique REFERENCES hs_office.coopassettx(uuid) DEFERRABLE INITIALLY DEFERRED,
    comment             varchar(512)
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-coopassets-BUSINESS-RULES endDelimiter:--//
-- ----------------------------------------------------------------------------

alter table hs_office.coopassettx
    add constraint reversal_asset_tx_must_have_reverted_asset_tx
        check (transactionType <> 'REVERSAL' or revertedAssetTxUuid is not null);

alter table hs_office.coopassettx
    add constraint non_reversal_asset_tx_must_not_have_reverted_asset_tx
        check (transactionType = 'REVERSAL' or revertedAssetTxUuid is null or transactionType = 'REVERSAL');

alter table hs_office.coopassettx
    add constraint transfer_asset_tx_must_have_adopted_asset_tx
        check (transactionType <> 'TRANSFER' or assetAdoptionTxUuid is not null);

alter table hs_office.coopassettx
    add constraint non_transfer_asset_tx_must_not_have_adopted_asset_tx
        check (transactionType = 'TRANSFER' or assetAdoptionTxUuid is null);
--//

-- ============================================================================
--changeset michael.hoennig:hs-office-coopassets-ASSET-VALUE-CONSTRAINT endDelimiter:--//
-- ----------------------------------------------------------------------------

create or replace function hs_office.coopassetstx_check_positive_total(forMembershipUuid UUID, newAssetValue numeric(12, 5))
returns boolean
language plpgsql as $$
declare
    currentAssetValue numeric(12,2);
    totalAssetValue numeric(12,2);
begin
    select sum(cat.assetValue)
        from hs_office.coopassettx cat
        where cat.membershipUuid = forMembershipUuid
        into currentAssetValue;
    totalAssetValue := currentAssetValue + newAssetValue;
    if totalAssetValue::numeric < 0 then
        raise exception '[400] coop assets transaction would result in a negative balance of assets';
    end if;
    return true;
end; $$;

alter table hs_office.coopassettx
    add constraint check_positive_total
        check ( hs_office.coopassetstx_check_positive_total(membershipUuid, assetValue) );
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-coopassets-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office.coopassettx');
--//
