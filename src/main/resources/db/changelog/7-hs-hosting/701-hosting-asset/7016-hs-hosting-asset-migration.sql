--liquibase formatted sql

-- TODO: These changesets are just for the external remote views to simulate the legacy tables.
--  Once we don't need the external remote views anymore, create revert changesets.

-- ============================================================================
--changeset hs-hosting-asset-MIGRATION-mapping:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE TABLE hs_hosting.asset_legacy_id
(
    uuid            uuid NOT NULL REFERENCES hs_hosting.asset(uuid),
    legacy_id       integer NOT NULL
);
--//


-- ============================================================================
--changeset hs-hosting-asset-MIGRATION-sequence:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS hs_hosting.asset_legacy_id_seq
    AS integer
    START 1000000000
    OWNED BY hs_hosting.asset_legacy_id.legacy_id;
--//


-- ============================================================================
--changeset hs-hosting-asset-MIGRATION-default:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

ALTER TABLE hs_hosting.asset_legacy_id
    ALTER COLUMN legacy_id
        SET DEFAULT nextVal('hs_hosting.asset_legacy_id_seq');
--/


-- ============================================================================
--changeset hs-hosting-asset-MIGRATION-insert:1 endDelimiter:--//
-- ----------------------------------------------------------------------------

CALL base.defineContext('schema-migration');
INSERT INTO hs_hosting.asset_legacy_id(uuid, legacy_id)
    SELECT uuid, nextVal('hs_hosting.asset_legacy_id_seq') FROM hs_hosting.asset;
--/


-- ============================================================================
--changeset hs-hosting-asset-MIGRATION-insert-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function hs_hosting.asset_insert_legacy_id_mapping_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP <> 'INSERT' then
        raise exception 'invalid usage of trigger';
    end if;

    INSERT INTO hs_hosting.asset_legacy_id VALUES
        (NEW.uuid, nextVal('hs_hosting.asset_legacy_id_seq'));

    return NEW;
end; $$;

create trigger insert_legacy_id_mapping_tg
    after insert on hs_hosting.asset
        for each row
            execute procedure hs_hosting.asset_insert_legacy_id_mapping_tf();
--/


-- ============================================================================
--changeset hs-hosting-asset-MIGRATION-delete-trigger:1 endDelimiter:--//
-- ----------------------------------------------------------------------------
create or replace function hs_hosting.asset_delete_legacy_id_mapping_tf()
    returns trigger
    language plpgsql
    strict as $$
begin
    if TG_OP <> 'DELETE' then
        raise exception 'invalid usage of trigger';
    end if;

    DELETE FROM hs_hosting.asset_legacy_id
           WHERE uuid = OLD.uuid;

    return OLD;
end; $$;

create trigger delete_legacy_id_mapping_tg
    before delete on hs_hosting.asset
        for each row
            execute procedure hs_hosting.asset_delete_legacy_id_mapping_tf();
--/
