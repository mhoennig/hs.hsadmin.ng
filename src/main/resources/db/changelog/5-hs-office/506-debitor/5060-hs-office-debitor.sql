--liquibase formatted sql

-- ============================================================================
--changeset michael.hoennig:hs-office-debitor-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_office.debitor
(
    uuid                    uuid unique references rbac.object (uuid) initially deferred,
    version                 int not null default 0,
    debitorNumberSuffix     char(2) not null check (debitorNumberSuffix::text ~ '^[0-9][0-9]$'),
    debitorRelUuid          uuid not null references hs_office.relation(uuid),
    billable                boolean not null default true,
    vatId                   varchar(24),
    vatCountryCode          varchar(2),
    vatBusiness             boolean not null,
    vatReverseCharge        boolean not null,
    refundBankAccountUuid   uuid references hs_office.bankaccount(uuid),
    defaultPrefix           char(3) not null unique
            constraint check_default_prefix check (
                defaultPrefix::text ~ '^([a-z]{3}|al0|bh1|c4s|f3k|k8i|l3d|mh1|o13|p2m|s80|t4w)$'
                )
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-debitor-DELETE-DEPENDENTS-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Trigger function to delete related relations of a debitor to delete.
 */
create or replace function hs_office.debitor_delete_dependents_tf()
    returns trigger
    language PLPGSQL
as $$
declare
    counter integer;
begin
    DELETE FROM hs_office.relation r WHERE r.uuid = OLD.debitorRelUuid;
    GET DIAGNOSTICS counter = ROW_COUNT;
    if counter = 0 then
        raise exception 'debitor relation % could not be deleted', OLD.debitorRelUuid;
    end if;

    RETURN OLD;
end; $$;

/**
    Triggers deletion of related details of a debitor to delete.
 */
create trigger debitor_delete_dependents_tg
    after delete
    on hs_office.debitor
    for each row
execute procedure hs_office.debitor_delete_dependents_tf();


-- ============================================================================
--changeset michael.hoennig:hs-office-debitor-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office.debitor');
--//
--changeset michael.hoennig:hs-office-debitor-DROP-REFUND-BANKACCOUNT endDelimiter:--//
-- ----------------------------------------------------------------------------

-- The administration asks a leaving member for the refund bank account, thus nobody maintains it in advance.
do language plpgsql $$
    begin
        call base.defineContext('dropping the refund bank account of debitors');
        delete from rbac.grant g
            using hs_office.debitor debitor, rbac.role role
            where g.grantedByTriggerOf = debitor.uuid
                and role.objectUuid = debitor.refundBankAccountUuid
                and role.uuid in (g.ascendantUuid, g.descendantUuid);
    end;
$$;

-- the RBAC restricted-view changeset (5063) re-creates the view without the column
drop view if exists hs_office.debitor_rv;

alter table hs_office.debitor drop column refundBankAccountUuid;
--//
