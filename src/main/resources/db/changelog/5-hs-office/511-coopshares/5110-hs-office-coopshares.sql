--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TYPE hs_office.CoopSharesTransactionType AS ENUM ('REVERSAL', 'SUBSCRIPTION', 'CANCELLATION');

CREATE CAST (character varying as hs_office.CoopSharesTransactionType) WITH INOUT AS IMPLICIT;

create table if not exists hs_office.coopsharetx
(
    uuid            uuid unique references rbac.object (uuid) initially deferred,
    version         int not null default 0,
    membershipUuid  uuid not null references hs_office.membership(uuid),
    transactionType hs_office.CoopSharesTransactionType not null,
    valueDate       date not null,
    shareCount      integer not null,
    reference       varchar(48) not null,
    revertedShareTxUuid uuid unique REFERENCES hs_office.coopsharetx(uuid) DEFERRABLE INITIALLY DEFERRED,
    comment         varchar(512)
);
--//

-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-BUSINESS-RULES endDelimiter:--//
-- ----------------------------------------------------------------------------

alter table hs_office.coopsharetx
    add constraint reverse_entry_missing
        check ( transactionType = 'REVERSAL' and revertedShareTxUuid is not null
             or transactionType <> 'REVERSAL' and revertedShareTxUuid is null);
--//

-- ============================================================================
--changeset marc.sandlus:hs-office-coopshares-SHARE-COUNT-CONSTRAINT-BY-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------

alter table hs_office.coopsharetx drop constraint if exists check_positive_total_shares_count;

drop function if exists hs_office.coopsharestx_check_positive_total cascade;

create or replace function hs_office.coopsharetx_enforce_positive_total()
returns trigger as $$

declare
    currentShareCount integer;
    totalShareCount integer;
begin
    select sum(cst.shareCount)
    from hs_office.coopsharetx cst
    where cst.membershipUuid = NEW.membershipUuid
    into currentShareCount;
    totalShareCount := currentShareCount + NEW.shareCount;
    if totalShareCount < 0 then
        raise exception '[400] coop shares transaction would result in a negative number of shares';
    end if;
    return NEW;
end;
$$ LANGUAGE plpgsql;;


create trigger positive_total_shares_count_tg before insert
    on hs_office.coopsharetx
    for each row execute function hs_office.coopsharetx_enforce_positive_total();

--//

-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office.coopsharetx');
--//
