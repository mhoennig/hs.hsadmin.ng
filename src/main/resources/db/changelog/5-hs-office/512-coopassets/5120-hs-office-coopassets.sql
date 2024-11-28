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

-- Not as CHECK constraints because those cannot be deferrable,
-- but we need these constraints deferrable because the rows are linked to each other.

CREATE OR REPLACE FUNCTION validate_transaction_type()
    RETURNS TRIGGER AS $$
BEGIN
    -- REVERSAL transactions must have revertedAssetTxUuid
    IF NEW.transactionType = 'REVERSAL' AND NEW.revertedAssetTxUuid IS NULL THEN
        RAISE EXCEPTION 'REVERSAL transactions must have revertedAssetTxUuid';
    END IF;

    -- Non-REVERSAL transactions must not have revertedAssetTxUuid
    IF NEW.transactionType != 'REVERSAL' AND NEW.revertedAssetTxUuid IS NOT NULL THEN
        RAISE EXCEPTION 'Non-REVERSAL transactions must not have revertedAssetTxUuid';
    END IF;

    -- TRANSFER transactions must have assetAdoptionTxUuid
    IF NEW.transactionType = 'TRANSFER' AND NEW.assetAdoptionTxUuid IS NULL THEN
        RAISE EXCEPTION 'TRANSFER transactions must have assetAdoptionTxUuid';
    END IF;

    -- Non-TRANSFER transactions must not have assetAdoptionTxUuid
    IF NEW.transactionType != 'TRANSFER' AND NEW.assetAdoptionTxUuid IS NOT NULL THEN
        RAISE EXCEPTION 'Non-TRANSFER transactions must not have assetAdoptionTxUuid';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach the trigger to the table
CREATE TRIGGER enforce_transaction_constraints
    AFTER INSERT OR UPDATE ON hs_office.coopassettx
    FOR EACH ROW EXECUTE FUNCTION validate_transaction_type();

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
