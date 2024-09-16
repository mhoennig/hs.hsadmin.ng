--liquibase formatted sql

-- TODO: These changesets are just for the external remote views to simulate the legacy tables.
--  Once we don't need the external remote views anymore, create revert changesets.

-- ============================================================================
--changeset michael.hoennig:hs-office-partner-MIGRATION-mapping endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TABLE hs_office_partner_legacy_id
(
    uuid        uuid NOT NULL REFERENCES hs_office_partner(uuid),
    bp_id       integer NOT NULL
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-MIGRATION-sequence endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS hs_office_partner_legacy_id_seq
    AS integer
    START 1000000000
    OWNED BY hs_office_partner_legacy_id.bp_id;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-MIGRATION-default endDelimiter:--//
-- ----------------------------------------------------------------------------

ALTER TABLE hs_office_partner_legacy_id
    ALTER COLUMN bp_id
        SET DEFAULT nextVal('hs_office_partner_legacy_id_seq');
--/

-- ============================================================================
--changeset michael.hoennig:hs-office-partner-MIGRATION-insert endDelimiter:--//
-- ----------------------------------------------------------------------------

CALL base.defineContext('schema-migration');
INSERT INTO hs_office_partner_legacy_id(uuid, bp_id)
    SELECT uuid, nextVal('hs_office_partner_legacy_id_seq') FROM hs_office_partner;
--/


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-MIGRATION-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function insertPartnerLegacyIdMapping()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of trigger';
    end if;

    INSERT INTO hs_office_partner_legacy_id VALUES
        (NEW.uuid, nextVal('hs_office_partner_legacy_id_seq'));

    return NEW;
end; $$;

create trigger createPartnerLegacyIdMapping
    after insert on hs_office_partner
        for each row
            execute procedure insertPartnerLegacyIdMapping();
--/


-- ============================================================================
--changeset michael.hoennig:hs-office-partner-MIGRATION-delete-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function deletePartnerLegacyIdMapping()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP <> 'DELETE' then
        raise exception 'invalid usage of trigger';
    end if;

    DELETE FROM hs_office_partner_legacy_id
        WHERE uuid = OLD.uuid;

    return OLD;
end; $$;

create trigger removePartnerLegacyIdMapping
    before delete on hs_office_partner
    for each row
        execute procedure deletePartnerLegacyIdMapping();
--/
