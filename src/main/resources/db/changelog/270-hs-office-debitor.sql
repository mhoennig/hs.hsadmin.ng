--liquibase formatted sql

-- ============================================================================
--changeset hs-office-debitor-MAIN-TABLE:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_office_debitor
(
    uuid                    uuid unique references RbacObject (uuid) initially deferred,
    debitorNumberSuffix     numeric(2) not null,
    debitorRelUuid          uuid not null references hs_office_relation(uuid),
    billable                boolean not null default true,
    vatId                   varchar(24), -- TODO.spec: here or in person?
    vatCountryCode          varchar(2),
    vatBusiness             boolean not null,
    vatReverseCharge        boolean not null,
    refundBankAccountUuid   uuid references hs_office_bankaccount(uuid),
    defaultPrefix           char(3) not null unique
            constraint check_default_prefix check (
                defaultPrefix::text ~ '^([a-z]{3}|al0|bh1|c4s|f3k|k8i|l3d|mh1|o13|p2m|s80|t4w)$'
                )
);
--//


-- ============================================================================
--changeset hs-office-debitor-DELETE-DEPENDENTS-TRIGGER:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Trigger function to delete related rows of a debitor to delete.
 */
create or replace function deleteHsOfficeDependentsOnDebitorDelete()
    returns trigger
    language PLPGSQL
as $$
declare
    counter integer;
begin
    DELETE FROM hs_office_relation r WHERE r.uuid = OLD.debitorRelUuid;
    GET DIAGNOSTICS counter = ROW_COUNT;
    if counter = 0 then
        raise exception 'debitor relation % could not be deleted', OLD.debitorRelUuid;
    end if;

    RETURN OLD;
end; $$;

/**
    Triggers deletion of related details of a debitor to delete.
 */
create trigger hs_office_debitor_delete_dependents_trigger
    after delete
    on hs_office_debitor
    for each row
execute procedure deleteHsOfficeDependentsOnDebitorDelete();


-- ============================================================================
--changeset hs-office-debitor-MAIN-TABLE-JOURNAL:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

call create_journal('hs_office_debitor');
--//
