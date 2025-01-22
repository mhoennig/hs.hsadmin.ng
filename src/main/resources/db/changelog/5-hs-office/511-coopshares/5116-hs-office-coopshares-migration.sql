--liquibase formatted sql

-- TODO.legacy: These changesets are just for the external remote views to simulate the legacy tables.
--  Once we don't need the external remote views anymore, create revert changesets.

-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-MIGRATION-legacy-mapping endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TABLE hs_office.coopsharetx_legacy_id
(
    uuid            uuid PRIMARY KEY NOT NULL REFERENCES hs_office.coopsharetx(uuid),
    member_share_id  integer UNIQUE NOT NULL
);
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-MIGRATION-sequence endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS hs_office.coopsharetx_legacy_id_seq
    AS integer
    START 1000000000
    OWNED BY hs_office.coopsharetx_legacy_id.member_share_id;
--//


-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-MIGRATION-default endDelimiter:--//
-- ----------------------------------------------------------------------------

ALTER TABLE hs_office.coopsharetx_legacy_id
    ALTER COLUMN member_share_id
        SET DEFAULT nextVal('hs_office.coopsharetx_legacy_id_seq');

--/

-- ============================================================================
--changeset michael.hoennig:hs-office-coopshares-MIGRATION-insert endDelimiter:--//
-- ----------------------------------------------------------------------------

CALL base.defineContext('schema-migration');
INSERT INTO hs_office.coopsharetx_legacy_id(uuid, member_share_id)
    SELECT uuid, nextVal('hs_office.coopsharetx_legacy_id_seq') FROM hs_office.coopsharetx;
--/


-- ============================================================================
--changeset michael.hoennig:hs-office-coopShares-MIGRATION-insert-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function hs_office.coopsharetx_insert_legacy_id_mapping_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of trigger';
    end if;

    INSERT INTO hs_office.coopsharetx_legacy_id VALUES
        (NEW.uuid, nextVal('hs_office.coopsharetx_legacy_id_seq'));

    return NEW;
end; $$;

create trigger insert_legacy_id_mapping_tg
    after insert on hs_office.coopsharetx
        for each row
            execute procedure hs_office.coopsharetx_insert_legacy_id_mapping_tf();
--/


-- ============================================================================
--changeset michael.hoennig:hs-office-coopShares-MIGRATION-delete-trigger endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function hs_office.coopsharetx_delete_legacy_id_mapping_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP <> 'DELETE' then
        raise exception 'invalid usage of trigger';
    end if;

    DELETE FROM hs_office.coopsharetx_legacy_id
           WHERE uuid = OLD.uuid;

    return OLD;
end; $$;

create trigger delete_legacy_id_mapping_tg
    before delete on hs_office.coopsharetx
        for each row
            execute procedure hs_office.coopsharetx_delete_legacy_id_mapping_tf();
--/
