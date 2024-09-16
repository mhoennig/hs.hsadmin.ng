--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-DETAILS-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_office_partner_details
(
    uuid                uuid unique references rbac.object (uuid) initially deferred,
    version             int not null default 0,
    registrationOffice  varchar(96),
    registrationNumber  varchar(96),
    birthPlace          varchar(96),
    birthName           varchar(96),
    birthday            date,
    dateOfDeath         date
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-DETAILS-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office_partner_details');
--//

-- ============================================================================
--changeset michael.hoennig:hs-office-partner-MAIN-TABLE endDelimiter:--//
-- ----------------------------------------------------------------------------

create table hs_office_partner
(
    uuid                uuid unique references rbac.object (uuid) initially deferred,
    version             int not null default 0,
    partnerNumber       numeric(5) unique not null,
    partnerRelUuid      uuid not null references hs_office_relation(uuid), -- deleted in after delete trigger
    detailsUuid         uuid not null references hs_office_partner_details(uuid) -- deleted in after delete trigger
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-DELETE-DEPENDENTS-TRIGGER endDelimiter:--//
-- ----------------------------------------------------------------------------

/**
    Trigger function to delete related details of a partner to delete.
 */
create or replace function deleteHsOfficeDependentsOnPartnerDelete()
    returns trigger
    language PLPGSQL
as $$
declare
    counter integer;
begin
    DELETE FROM hs_office_partner_details d WHERE d.uuid = OLD.detailsUuid;
    GET DIAGNOSTICS counter = ROW_COUNT;
    if counter = 0 then
        raise exception 'partner details % could not be deleted', OLD.detailsUuid;
    end if;

    DELETE FROM hs_office_relation r WHERE r.uuid = OLD.partnerRelUuid;
    GET DIAGNOSTICS counter = ROW_COUNT;
    if counter = 0 then
        raise exception 'partner relation % could not be deleted', OLD.partnerRelUuid;
    end if;

    RETURN OLD;
end; $$;

/**
    Triggers deletion of related rows of a partner to delete.
 */
create trigger hs_office_partner_delete_dependents_trigger
    after delete
    on hs_office_partner
    for each row
        execute procedure deleteHsOfficeDependentsOnPartnerDelete();

-- ============================================================================
--changeset michael.hoennig:hs-office-partner-MAIN-TABLE-JOURNAL endDelimiter:--//
-- ----------------------------------------------------------------------------

call base.create_journal('hs_office_partner');
--//
