--liquibase formatted sql

-- TODO.legacy: These changesets are just for the external remote views to simulate the legacy tables.
--  Once we don't need the external remote views anymore, create revert changesets.

-- ============================================================================
--changeset michael.hoennig:hs-office-sepamandate-MIGRATION-legacy-mapping endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TABLE hs_office.sepamandate_legacy_id
(
    uuid             uuid PRIMARY KEY NOT NULL REFERENCES hs_office.sepamandate(uuid),
    sepa_mandate_id  integer UNIQUE NOT NULL
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-sepamandate-MIGRATION-sequence endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS hs_office.sepamandate_legacy_id_seq
    AS integer
    START 1000000000
    OWNED BY hs_office.sepamandate_legacy_id.sepa_mandate_id;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-sepamandate-MIGRATION-default endDelimiter:--//
-- ----------------------------------------------------------------------------

ALTER TABLE hs_office.sepamandate_legacy_id
    ALTER COLUMN sepa_mandate_id
        SET DEFAULT nextVal('hs_office.sepamandate_legacy_id_seq');

--/


-- ============================================================================
--changeset michael.hoennig:hs-office-sepamandate-MIGRATION-insert endDelimiter:--//
-- ----------------------------------------------------------------------------

CALL base.defineContext('schema-migration');
INSERT INTO hs_office.sepamandate_legacy_id(uuid, sepa_mandate_id)
    SELECT uuid, nextVal('hs_office.sepamandate_legacy_id_seq') FROM hs_office.sepamandate;
--/


-- ============================================================================
--changeset michael.hoennig:hs-office-sepamandate-MIGRATION-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function hs_office.sepamandate_insert_legacy_id_mapping_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of trigger';
    end if;

    INSERT INTO hs_office.sepamandate_legacy_id VALUES
        (NEW.uuid, nextVal('hs_office.sepamandate_legacy_id_seq'));

    return NEW;
end; $$;

create trigger insert_legacy_id_mapping_tg
    after insert on hs_office.sepamandate
        for each row
            execute procedure hs_office.sepamandate_insert_legacy_id_mapping_tf();
--/


-- ============================================================================
--changeset michael.hoennig:hs-office-sepamandate-MIGRATION-delete-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function hs_office.sepamandate_delete_legacy_id_mapping_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP <> 'DELETE' then
        raise exception 'invalid usage of trigger';
    end if;

    DELETE FROM hs_office.sepamandate_legacy_id
           WHERE uuid = OLD.uuid;

    return OLD;
end; $$;

create trigger delete_legacy_id_mapping_tf
    before delete on hs_office.sepamandate
        for each row
            execute procedure hs_office.sepamandate_delete_legacy_id_mapping_tf();
--/
