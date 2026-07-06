--liquibase formatted sql


-- ============================================================================
--changeset michael.hoennig:base-array-functions-WITHOUT-NULL-VALUES runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION base.without_null_values(arr anyarray)
    RETURNS anyarray
AS $$
    SELECT array_agg(e) FROM unnest(arr) AS e WHERE e IS NOT NULL
$$ LANGUAGE sql IMMUTABLE;


-- ============================================================================
-- the odd changeset id is kept as released (garbled by a copy-paste accident)
--changeset michael.hoennig:base-array-functions-ADD-IF-NOT-NULLCREATE runOnChange:true endDelimiter:--//
--validCheckSum: ANY
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION base.add_if_not_null(arr anyarray, val anyelement)
    RETURNS anyarray
AS $$
    SELECT CASE WHEN val IS NULL THEN arr ELSE arr || val END
$$ LANGUAGE sql IMMUTABLE;


