--liquibase formatted sql

-- TODO: These changesets are just for the external remote views to simulate the legacy tables.
--  Once we don't need the external remote views anymore, create revert changesets.

-- ============================================================================
--changeset hs-office-coopshares-MIGRATION-mapping:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TABLE hs_office_coopsharestransaction_legacy_id
(
    uuid            uuid NOT NULL REFERENCES hs_office_coopsharestransaction(uuid),
    member_share_id  integer NOT NULL
);
--//


-- ============================================================================
--changeset hs-office-coopshares-MIGRATION-sequence:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS hs_office_coopsharestransaction_legacy_id_seq
    AS integer
    START 1000000000
    OWNED BY hs_office_coopsharestransaction_legacy_id.member_share_id;
--//


-- ============================================================================
--changeset hs-office-coopshares-MIGRATION-default:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

ALTER TABLE hs_office_coopsharestransaction_legacy_id
    ALTER COLUMN member_share_id
        SET DEFAULT nextVal('hs_office_coopsharestransaction_legacy_id_seq');

--/

-- ============================================================================
--changeset hs-office-coopshares-MIGRATION-insert:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CALL defineContext('schema-migration');
INSERT INTO hs_office_coopsharestransaction_legacy_id(uuid, member_share_id)
    SELECT uuid, nextVal('hs_office_coopsharestransaction_legacy_id_seq') FROM hs_office_coopsharestransaction;
--/


-- ============================================================================
--changeset hs-office-coopShares-MIGRATION-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function insertCoopSharesLegacyIdMapping()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of trigger';
    end if;

    INSERT INTO hs_office_coopsharestransaction_legacy_id VALUES
        (NEW.uuid, nextVal('hs_office_coopsharestransaction_legacy_id_seq'));

    return NEW;
end; $$;

create trigger createCoopSharesLegacyIdMapping
    after insert on hs_office_coopsharestransaction
        for each row
            execute procedure insertCoopSharesLegacyIdMapping();
--/


-- ============================================================================
--changeset hs-office-coopShares-MIGRATION-delete-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function deleteCoopSharesLegacyIdMapping()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP <> 'DELETE' then
        raise exception 'invalid usage of trigger';
    end if;

    DELETE FROM hs_office_coopsharestransaction_legacy_id
           WHERE uuid = OLD.uuid;

    return OLD;
end; $$;

create trigger removeCoopSharesLegacyIdMapping
    before delete on hs_office_coopsharestransaction
        for each row
            execute procedure deleteCoopSharesLegacyIdMapping();
--/
